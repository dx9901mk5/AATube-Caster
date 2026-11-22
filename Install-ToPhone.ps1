param(
    [string]$ApkPath = "",
    [switch]$CleanReinstall
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApkPath)) {
    $candidates = @(
        "$PSScriptRoot\dist\AATube-Caster-latest.apk",
        "$env:TEMP\AATube-Caster-build\app\outputs\apk\release\app-release.apk",
        "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk",
        "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
    )
    $ApkPath = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($ApkPath) -or -not (Test-Path -LiteralPath $ApkPath)) {
    throw "找不到 APK 檔案，請先執行 .\gradlew.bat assembleRelease"
}

& "D:\Android\SdkPool\Invoke-WithFreeSdk.ps1" {
    $adb = Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"

    Write-Host "==> [1/5] 檢查 ADB 連線裝置..." -ForegroundColor Cyan
    $devicesOutput = & $adb devices
    $onlineDevices = $devicesOutput | Select-String -Pattern "^\S+\s+device$"
    if (-not $onlineDevices) {
        Write-Host ($devicesOutput -join "`n") -ForegroundColor Yellow
        throw "未偵測到已授權的 Android 手機！請插上 USB 並開啟「USB 偵錯」（若為 vivo/OPPO/小米請一併開啟「USB 安裝」）。"
    }
    Write-Host ($onlineDevices -join "`n") -ForegroundColor Green

    if ($CleanReinstall) {
        Write-Host "==> 清除舊版 com.aayt.caster..." -ForegroundColor Yellow
        & $adb shell pm uninstall com.aayt.caster | Out-Null
    }

    Write-Host "==> [2/5] 推送 APK 至手機暫存區 /data/local/tmp/aatube.apk..." -ForegroundColor Cyan
    & $adb push $ApkPath /data/local/tmp/aatube.apk

    Write-Host "==> [3/5] 宣告 Google Play 安裝來源 (com.android.vending) 進行安裝..." -ForegroundColor Cyan
    $installRes = & $adb shell pm install -i "com.android.vending" -r -g /data/local/tmp/aatube.apk 2>&1
    Write-Host ($installRes -join "`n")
    & $adb shell rm -f /data/local/tmp/aatube.apk | Out-Null

    if (($installRes -join " ") -notmatch "Success") {
        throw "ADB 安裝失敗，請確認手機螢幕是否跳出「允許 USB 安裝」提示，或加上 -CleanReinstall 參數重試。"
    }

    Write-Host "==> [4/5] 設定背景執行保活與電池最佳化白名單..." -ForegroundColor Cyan
    & $adb shell dumpsys deviceidle whitelist +com.aayt.caster | Out-Null
    & $adb shell appops set com.aayt.caster RUN_IN_BACKGROUND allow | Out-Null
    & $adb shell appops set com.aayt.caster RUN_ANY_IN_BACKGROUND allow | Out-Null
    & $adb shell appops set com.aayt.caster START_FOREGROUND allow | Out-Null

    Write-Host "==> [5/5] 驗證安裝來源與 Android Auto CarAppService 註冊狀態..." -ForegroundColor Cyan
    $installerCheck = & $adb shell pm list packages -i com.aayt.caster
    $serviceCheck = & $adb shell pm query-services --components -a "androidx.car.app.CarAppService" | Select-String "com.aayt.caster"

    Write-Host "  套件安裝來源：$installerCheck" -ForegroundColor Green
    Write-Host "  車載服務註冊：$serviceCheck" -ForegroundColor Green

    if ($installerCheck -match "installer=com.android.vending") {
        Write-Host "`n✅ 安裝與來源偽裝成功！請確認 Android Auto 開發人員設定已勾選「未知的來源」，即可在「自訂啟動器」與車機畫面看到 AA Tube 車載影音！" -ForegroundColor Green
    } else {
        Write-Host "`n⚠️ 注意：系統回報來源非 com.android.vending ($installerCheck)。若 Android Auto 自訂啟動器未顯示本 App，請在手機改用 Shizuku + InstallerX Revived / Install with Options 指定 com.android.vending 重新安裝。" -ForegroundColor Yellow
    }
}
