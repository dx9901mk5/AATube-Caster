package com.aayt.caster.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.aayt.caster.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShizukuUiState(
    val isInstalled: Boolean = false,
    val binderAlive: Boolean = false,
    val permissionGranted: Boolean = false,
    val serviceBound: Boolean = false,
    val isRunningScript: Boolean = false,
    val lastScriptOutput: String? = null
)

class ShizukuAaManager(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ShizukuUiState())
    val state: StateFlow<ShizukuUiState> = _state.asStateFlow()

    @Volatile
    private var service: IPrivilegedAaService? = null
    private var listenersRegistered = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, PrivilegedAaService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shizuku_aa")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IPrivilegedAaService.Stub.asInterface(binder)
            _state.update { it.copy(serviceBound = service != null) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _state.update { it.copy(serviceBound = false) }
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshAndBind()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        _state.update {
            it.copy(binderAlive = false, permissionGranted = false, serviceBound = false)
        }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _state.update { it.copy(permissionGranted = granted) }
            if (granted) {
                bindPrivilegedService()
            }
        }
    }

    fun init() {
        if (!listenersRegistered) {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = true
        }
        refreshAndBind()
    }

    fun refreshAndBind() {
        val installed = runCatching {
            appContext.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        }.getOrDefault(false)

        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = if (alive) {
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        } else {
            false
        }

        _state.update {
            it.copy(
                isInstalled = installed,
                binderAlive = alive,
                permissionGranted = granted,
                serviceBound = service != null
            )
        }

        if (alive && granted && service == null) {
            bindPrivilegedService()
        }
    }

    fun requestPermission() {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) {
            openShizukuApp()
            return
        }
        runCatching {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } else {
                refreshAndBind()
            }
        }
    }

    fun openShizukuApp() {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appContext.startActivity(launchIntent) }
        }
    }

    private fun bindPrivilegedService() {
        runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        }
    }

    fun runAaRecognitionScript(forceReinstallPlayStore: Boolean = false) {
        val svc = service
        if (svc == null) {
            refreshAndBind()
            _state.update {
                it.copy(lastScriptOutput = "⚠️ Shizuku UserService 尚未連線，請確認 Shizuku 已啟動並授權後再試一次")
            }
            return
        }

        _state.update { it.copy(isRunningScript = true) }
        scope.launch {
            val output = withContext(Dispatchers.IO) {
                runCatching {
                    val apkPath = appContext.applicationInfo.sourceDir
                    svc.runAaRecognitionScript(
                        appContext.packageName,
                        apkPath,
                        forceReinstallPlayStore
                    )
                }.getOrElse { err ->
                    "❌ 執行失敗：${err.localizedMessage}"
                }
            }
            _state.update { it.copy(isRunningScript = false, lastScriptOutput = output) }
        }
    }

    fun installApkViaShizuku(apkPath: String): Boolean {
        val svc = service ?: return false
        return runCatching { svc.installApkAsPlayStore(apkPath) }.getOrDefault(false)
    }

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1002
    }
}
