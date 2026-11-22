package com.aayt.caster.mobile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class AppPermissionStatus(
    val notificationsGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val installUnknownAppsGranted: Boolean,
    val installerPackageName: String?,
    val isAndroidAutoInstalled: Boolean
) {
    val isPlayStoreInstaller: Boolean
        get() = installerPackageName == "com.android.vending"
}

object PermissionHelper {

    private const val AA_PACKAGE = "com.google.android.projection.gearhead"

    fun checkStatus(context: Context): AppPermissionStatus {
        val pkg = context.packageName
        val notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled()

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = pm.isIgnoringBatteryOptimizations(pkg)

        val canInstallApk = context.packageManager.canRequestPackageInstalls()

        val installerPkg = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(pkg).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(pkg)
            }
        }.getOrNull()

        val aaInstalled = runCatching {
            context.packageManager.getPackageInfo(AA_PACKAGE, 0)
            true
        }.getOrDefault(false)

        return AppPermissionStatus(
            notificationsGranted = notificationsGranted,
            batteryOptimizationIgnored = batteryIgnored,
            installUnknownAppsGranted = canInstallApk,
            installerPackageName = installerPkg,
            isAndroidAutoInstalled = aaInstalled
        )
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val directIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = runCatching {
            context.startActivity(directIntent)
            true
        }.getOrDefault(false)

        if (!started) {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            launchSafely(context, listIntent)
        }
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        launchSafely(context, intent)
    }

    fun openAndroidAutoSettings(context: Context) {
        // 1. Try Android Auto's native Companion Settings Activity directly
        val candidates = listOf(
            Intent().apply {
                component = ComponentName(
                    AA_PACKAGE,
                    "com.google.android.projection.gearhead.companion.settings.DefaultSettingsActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$AA_PACKAGE")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent("android.settings.CONNECTED_DEVICE_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )

        for (candidate in candidates) {
            val ok = runCatching {
                context.startActivity(candidate)
                true
            }.getOrDefault(false)
            if (ok) return
        }
        openAppDetailsSettings(context)
    }

    fun openGitHubRepo(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun launchSafely(context: Context, intent: Intent) {
        val ok = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!ok) {
            openAppDetailsSettings(context)
        }
    }
}
