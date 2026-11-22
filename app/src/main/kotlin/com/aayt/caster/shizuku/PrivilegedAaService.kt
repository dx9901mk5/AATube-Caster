package com.aayt.caster.shizuku

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

/**
 * Shizuku UserService running with Shell (UID 2000) privileges.
 * Executes Android Auto recognition scripts, Play Store installer attribution,
 * battery/background whitelisting, and gearhead cache refresh without root.
 */
class PrivilegedAaService : IPrivilegedAaService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun runAaRecognitionScript(
        appPackage: String,
        sourceApkPath: String,
        forceReinstallPlayStore: Boolean
    ): String {
        val log = StringBuilder()

        // 1. Battery optimization & background AppOps for both AATube and Android Auto
        val prepCommands = listOf(
            "dumpsys deviceidle whitelist +$appPackage",
            "dumpsys deviceidle whitelist +com.google.android.projection.gearhead",
            "appops set $appPackage RUN_IN_BACKGROUND allow",
            "appops set $appPackage RUN_ANY_IN_BACKGROUND allow",
            "appops set $appPackage START_FOREGROUND allow",
            "appops set $appPackage PROJECT_MEDIA allow",
            "appops set $appPackage REQUEST_INSTALL_PACKAGES allow",
            "pm enable $appPackage/.car.YoutubeCarAppService"
        )
        for (cmd in prepCommands) {
            execSh(cmd)
        }
        log.appendLine("✅ 已寫入省電白名單、背景保活與 PROJECT_MEDIA 權限")

        // 2. Check current installer attribution
        val installerOut = execSh("pm list packages -i $appPackage")
        val hasPlayInstaller = installerOut.contains("installer=com.android.vending")
        log.appendLine("📦 目前安裝來源：${installerOut.trim().ifBlank { "未知" }}")

        // 3. Verify CarAppService registration
        val carServices = execSh("pm query-services --components -a androidx.car.app.CarAppService")
        val isRegistered = carServices.contains(appPackage)
        log.appendLine("🚗 CarAppService 註冊：${if (isRegistered) "已註冊 ($appPackage/.car.YoutubeCarAppService)" else "未偵測到"}")

        // 4. Refresh Android Auto (gearhead) cache
        execSh("am force-stop com.google.android.projection.gearhead")
        log.appendLine("🔄 已重置 Android Auto (gearhead) 快取，強制重新掃描車載 App")

        // 5. If installer is not com.android.vending (or user forced rewrite), run detached self-reinstall
        if (!hasPlayInstaller || forceReinstallPlayStore) {
            if (sourceApkPath.isNotBlank()) {
                val escapedApk = sourceApkPath.replace("'", "'\\''")
                val detachedCmd =
                    "cp '$escapedApk' /data/local/tmp/aatube_self.apk && " +
                        "pm install -i com.android.vending -r -g /data/local/tmp/aatube_self.apk && " +
                        "rm -f /data/local/tmp/aatube_self.apk && " +
                        "am force-stop com.google.android.projection.gearhead && " +
                        "am start -n $appPackage/.mobile.MainActivity"

                Runtime.getRuntime().exec(arrayOf("sh", "-c", "nohup sh -c \"$detachedCmd\" >/dev/null 2>&1 &"))
                log.appendLine("⚡ 正在透過 Shizuku 原地重寫安裝來源為 com.android.vending，App 將在 2 秒後自動重啟完成！")
            } else {
                log.appendLine("⚠️ 找不到本機 APK 路徑，無法重寫安裝來源")
            }
        } else {
            log.appendLine("✅ 來源已為 com.android.vending，所有 AA 辨識權限寫入完成！")
        }

        return log.toString().trim()
    }

    override fun installApkAsPlayStore(apkPath: String): Boolean {
        if (apkPath.isBlank()) return false
        val escapedApk = apkPath.replace("'", "'\\''")
        val detachedCmd =
            "cp '$escapedApk' /data/local/tmp/aatube_ota.apk && " +
                "pm install -i com.android.vending -r -g /data/local/tmp/aatube_ota.apk && " +
                "rm -f /data/local/tmp/aatube_ota.apk && " +
                "am force-stop com.google.android.projection.gearhead && " +
                "am start -n com.aayt.caster/.mobile.MainActivity"

        return runCatching {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "nohup sh -c \"$detachedCmd\" >/dev/null 2>&1 &"))
            true
        }.getOrDefault(false)
    }

    private fun execSh(command: String): String {
        return runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val out = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            out
        }.getOrDefault("")
    }
}
