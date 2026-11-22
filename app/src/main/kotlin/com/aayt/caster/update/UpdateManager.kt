package com.aayt.caster.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aayt.caster.BuildConfig
import com.aayt.caster.mobile.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0L
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

data class ReleaseUpdateInfo(
    val latestVersion: String,
    val title: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val releasePageUrl: String,
    val isNewer: Boolean
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class UpToDate(val currentVersion: String, val latestRelease: ReleaseUpdateInfo?) : UpdateUiState
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateUiState
    data class Downloading(
        val info: ReleaseUpdateInfo,
        val progressPercent: Int,
        val downloadedMb: Float,
        val totalMb: Float
    ) : UpdateUiState
    data class WaitingInstallPermission(val apkFile: File, val info: ReleaseUpdateInfo) : UpdateUiState
    data class ReadyToInstall(val apkFile: File, val info: ReleaseUpdateInfo) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateManager(
    private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdates(silentIfUpToDate: Boolean = false) {
        if (_uiState.value is UpdateUiState.Checking || _uiState.value is UpdateUiState.Downloading) return
        if (!silentIfUpToDate) {
            _uiState.value = UpdateUiState.Checking
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val req = Request.Builder()
                        .url(GITHUB_LATEST_RELEASE_API)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "AATube-Caster/${BuildConfig.VERSION_NAME}")
                        .build()

                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("GitHub API 回應碼：${resp.code}")
                        }
                        val bodyStr = resp.body?.string() ?: throw IllegalStateException("空白回應")
                        val release = json.decodeFromString<GitHubRelease>(bodyStr)
                        val apkAsset = release.assets.firstOrNull {
                            it.name.endsWith(".apk", ignoreCase = true)
                        } ?: throw IllegalStateException("該版本尚未附加 APK 安裝檔")

                        val cleanLatest = release.tagName.removePrefix("v").removePrefix("V").trim()
                        val cleanCurrent = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
                        val newer = compareSemVer(cleanLatest, cleanCurrent) > 0

                        ReleaseUpdateInfo(
                            latestVersion = cleanLatest,
                            title = release.name.ifBlank { release.tagName },
                            changelog = release.body.trim(),
                            apkDownloadUrl = apkAsset.downloadUrl,
                            releasePageUrl = release.htmlUrl.ifBlank { GITHUB_REPO_URL },
                            isNewer = newer
                        )
                    }
                }
            }

            result
                .onSuccess { info ->
                    if (info.isNewer) {
                        _uiState.value = UpdateUiState.UpdateAvailable(info)
                    } else if (!silentIfUpToDate) {
                        _uiState.value = UpdateUiState.UpToDate(BuildConfig.VERSION_NAME, info)
                    }
                }
                .onFailure { err ->
                    if (!silentIfUpToDate) {
                        _uiState.value = UpdateUiState.Error("檢查更新失敗：${err.localizedMessage ?: "網路異常"}")
                    }
                }
        }
    }

    fun downloadAndInstall(info: ReleaseUpdateInfo) {
        if (_uiState.value is UpdateUiState.Downloading) return
        _uiState.value = UpdateUiState.Downloading(info, progressPercent = 0, downloadedMb = 0f, totalMb = 0f)

        scope.launch {
            val downloadResult = withContext(Dispatchers.IO) {
                runCatching {
                    val updatesDir = File(appContext.externalCacheDir ?: appContext.cacheDir, "updates").apply {
                        mkdirs()
                    }
                    val targetFile = File(updatesDir, "AATube-Caster-${info.latestVersion}.apk")
                    if (targetFile.exists()) targetFile.delete()

                    val req = Request.Builder()
                        .url(info.apkDownloadUrl)
                        .header("User-Agent", "AATube-Caster/${BuildConfig.VERSION_NAME}")
                        .build()

                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            throw IllegalStateException("下載失敗 (HTTP ${resp.code})")
                        }
                        val body = resp.body ?: throw IllegalStateException("無下載內容")
                        val totalBytes = body.contentLength()
                        val totalMb = if (totalBytes > 0) totalBytes / 1_048_576f else 0f

                        body.byteStream().use { input ->
                            FileOutputStream(targetFile).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var downloadedBytes = 0L
                                var lastEmitMs = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read

                                    val now = System.currentTimeMillis()
                                    if (now - lastEmitMs >= 120L) {
                                        lastEmitMs = now
                                        val dlMb = downloadedBytes / 1_048_576f
                                        val pct = if (totalBytes > 0) {
                                            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                        } else {
                                            0
                                        }
                                        _uiState.value = UpdateUiState.Downloading(
                                            info = info,
                                            progressPercent = pct,
                                            downloadedMb = dlMb,
                                            totalMb = totalMb
                                        )
                                    }
                                }
                                output.flush()
                            }
                        }
                    }
                    targetFile
                }
            }

            downloadResult
                .onSuccess { apkFile ->
                    if (!appContext.packageManager.canRequestPackageInstalls()) {
                        _uiState.value = UpdateUiState.WaitingInstallPermission(apkFile, info)
                        PermissionHelper.openInstallUnknownAppsSettings(appContext)
                    } else {
                        _uiState.value = UpdateUiState.ReadyToInstall(apkFile, info)
                        triggerPackageInstaller(apkFile)
                    }
                }
                .onFailure { err ->
                    _uiState.value = UpdateUiState.Error("下載更新包失敗：${err.localizedMessage ?: "網路中斷"}")
                }
        }
    }

    /**
     * Called when returning from Settings -> Allow Unknown Apps so installation starts automatically!
     */
    fun resumePendingInstallIfPermitted() {
        val current = _uiState.value
        if (current is UpdateUiState.WaitingInstallPermission &&
            appContext.packageManager.canRequestPackageInstalls()
        ) {
            _uiState.value = UpdateUiState.ReadyToInstall(current.apkFile, current.info)
            triggerPackageInstaller(current.apkFile)
        }
    }

    fun triggerPackageInstaller(apkFile: File) {
        if (!apkFile.exists()) {
            _uiState.value = UpdateUiState.Error("找不到已下載的 APK 檔案，請重新下載")
            return
        }
        if (!appContext.packageManager.canRequestPackageInstalls()) {
            PermissionHelper.openInstallUnknownAppsSettings(appContext)
            return
        }

        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            appContext.startActivity(installIntent)
        }.onFailure { err ->
            _uiState.value = UpdateUiState.Error("無法啟動系統安裝器：${err.localizedMessage}")
        }
    }

    private fun compareSemVer(v1: String, v2: String): Int {
        val p1 = v1.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val p2 = v2.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    companion object {
        const val GITHUB_REPO_URL = "https://github.com/dx9901mk5/AATube-Caster"
        private const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/dx9901mk5/AATube-Caster/releases/latest"
    }
}
