# AA Tube 車載無廣告影音 (AATube-Caster)

**免 Root！Android Auto 車載無廣告 YouTube 投放與原生觸控手勢播放器**  
**Rootless Ad-Free YouTube Caster & Native Touch Gesture Player for Android Auto**

---

## ✨ 專案特色 | Key Features

### 🇹🇼 繁體中文介紹
* **免 Root 支援 Android 14 / 15 / 16**：採用官方 `androidx.car.app` (`CarAppService` + `NavigationTemplate` + `Action.PAN`) 搭配硬體加速 `VirtualDisplay` 與 `Presentation`，無需 Root 或刷寫 LSPosed 模組即可投射完整自訂畫面至實體車機螢幕。
* **天生 100% 無廣告解析引擎**：內建 `NewPipeExtractor` + `Media3 ExoPlayer` (`MergingMediaSource`)，免登入、無片頭與插播廣告，支援 1080p / 720p 硬體解碼、台灣 24 小時新聞直播與字幕顯示，手機低發熱不燙手。
* **原生 YouTube App「一鍵分享秒投車機」**：在手機官方 YouTube App 按下「分享 -> 投放到車機播放 (AA Tube)」，即可零打擾自動解析並投放到車機（支援設定「立即插播」或「排入下一首待播」）。
* **車載專屬可觸控 YouTube 介面 & 完整手勢**：
  * **單擊畫面**：顯示／隱藏半透明控制列、畫質切換、字幕與時間軸進度條
  * **左右雙擊快轉／倒退**：支援 YouTube 經典 **±10 秒水波紋快轉**（連續點擊累加 ±10s / ±20s / ±30s）
  * **雙指張開／捏合**：一秒切換「填滿超寬車機螢幕 (`Zoom to Fill`)」與「原始 16:9 比例 (`Fit`)」
  * **全螢幕上滑**：拉出底部「相關推薦影片 & 待播清單」抽屜
  * **全螢幕下滑**：無縫縮小為右下角 **子母畫面 (`MiniPlayer`)**，一邊看片一邊滑逛首頁與搜尋結果
* **車載音訊與方向盤整合**：完整整合 `MediaSession`，支援方向盤實體上下首／暫停鍵，並支援 **導航語音自動降音（Audio Ducking）**，Google Maps 播報路況時不中斷影片。

### 🇺🇸 English Overview
* **Rootless on Android 14 / 15 / 16**: Built on official `androidx.car.app` (`CarAppService` + `SurfaceCallback` + `Action.PAN`) bridged to a hardware-accelerated `VirtualDisplay` & Jetpack Compose `Presentation`—no Magisk or LSPosed required.
* **100% Ad-Free Native Playback**: Powered by `NewPipeExtractor` + `Media3 ExoPlayer` (`MergingMediaSource` for 1080p/720p adaptive streams, HLS live streams, and subtitles) with low thermal overhead.
* **One-Tap Share from Official YouTube App**: Share any video, Short, or Live stream from the native YouTube app directly to your car display (`Play Immediately` or `Enqueue Next`).
* **Touchable Car YouTube UI & Authentic YT Gestures**:
  * **Single Tap**: Toggle HUD overlay, timeline scrubber, resolution picker (`1080p/720p/480p`), subtitles & touch-lock shield.
  * **Double-Tap Left / Right**: Cumulative **±10s / ±20s / ±30s ripple seek**.
  * **Pinch Out / In**: Switch between ultrawide **Zoom-to-Fill** and **16:9 Fit**.
  * **Swipe Up**: Pull up the **Related Videos & Up Next Queue** sheet.
  * **Swipe Down**: Minimize seamlessly into a live **Picture-in-Picture MiniPlayer** while browsing/searching videos on the car screen.
* **Steering Wheel & Navigation Audio Ducking**: Full `MediaSessionService` integration for steering wheel Next/Previous controls and automatic audio ducking during turn-by-turn navigation prompts.

---

## 🚀 免 Root 實車安裝步驟 | Rootless Car Setup (Android 14+)

1. **開啟 Android Auto 未知的來源 | Enable Unknown Sources in Android Auto**
   * 手機進入 `設定 -> 連接的裝置 -> 連線偏好設定 -> Android Auto`，滑到底部連點 10 次「版本」啟用開發人員模式，點右上角 `⋮ -> 開發人員設定`，勾選 **「未知的來源 (Unknown sources)」**。
   * Go to Phone `Settings -> Connected devices -> Android Auto`, tap **Version** 10 times, open `⋮ -> Developer settings`, and check **Unknown sources**.

2. **搭配 Shizuku 標記安裝來源安裝 | Install via Shizuku (`com.android.vending`)**
   * Android 14+ 會驗證車載模板 App 的安裝來源是否為 Google Play (`com.android.vending`)。
   * 啟動 **Shizuku**（透過無線偵錯即可免電腦啟用），使用 **Install with Options**（或支援指定 Installer 的 Shizuku 安裝器）安裝 APK，並將 **Installer Package** 設為 `com.android.vending`，連上車機即可看到 **AA Tube 車載影音** 圖示！
   * *(替代方案 / Alternative)*：亦可執行 `./gradlew bundleDebug` 打包 `.aab` 上傳至 Google Play Console「內部應用程式分享 (Internal App Sharing)」，免審核直接點擊連結安裝。

---

## 🛠️ 編譯方式 | Build Instructions

```bash
./gradlew assembleDebug
```

產出 APK 位於 / Output APK path: `app/build/outputs/apk/debug/app-debug.apk`

---

## ⚠️ 行車安全聲明 | Safety Disclaimer

本專案僅供學術研究、停車休息（如電車充電等候）或副駕／乘客娛樂使用。駕駛車輛行進間請專注路況，切勿分心觀看或操作螢幕，以確保行車安全。  
*For passengers and parked entertainment (e.g., EV charging stops) only. Never watch videos or interact with the screen while driving.*
