# ⏰ 小時鐘 (LittleClock)

一款輕量、無廣告、無背景常駐的 Android 相片時鐘；僅使用 AndroidX ExifInterface 處理相片方向。
適用於新舊平板、手機、電視盒與 Amazon Fire 平板。

---

## 核心功能

- **時鐘縮放**：支援雙指縮放，並記憶偏好設定。
- **播放手勢**：左滑下一張、右滑上一張；一輪播完前不重複。
- **低耗電模式**：預設停用像素分析、柔和背景與裝飾效果，適合低階裝置。
- **相片管理**：長按相片可收藏或隱藏，並可選擇只播放收藏。
- **三種顯示方式**：填滿、完整顯示，以及選用的低解析度柔和背景。
- **相片展示與輕量解碼**：
  - 相片背景平滑推頁/淡入/3D 轉場與溫和 Ken Burns 鏡頭微推效果。
  - 依畫面明暗分布調整取景位置。
  - **相片 EXIF 方向自動修正**：相機直拍/橫拍照片自動旋轉呈現。
  - 限制解碼像素並在記憶體不足時降級重試。
- **11 款開源字型與 1 款內部測試字型**：
  - 收錄 Orbitron、DotGothic16、Noto Sans/Serif JP、Zen Maru Gothic、Klee One、Dela Gothic One、Audiowide、Oxanium、Saira Stencil One 與 Zen Dots。
  - 字型採用 `FontSubsetting` 技術縮減檔案大小。
  - 12 個 `.ttf` 合計約 262 KB；CJK 字型只保留日期所需中文字，Latin 字型使用英文日期。
  - Storopia 僅供內部測試，授權確認前不可分發含此字型的 APK。
- **系統字型**：Android 4.2 起可選系統無襯線、襯線與等寬字型；Android 10 以上另列出裝置實際安裝的系統字型，不複製進 APK。
- **選用天氣**：
  - 手動搜尋並保存英文地名，不使用 GPS、Google Play 服務或位置權限。
  - 僅在 App 前景且連接 Wi-Fi 時向 Open-Meteo 更新；一般模式每 60 分鐘、低耗電模式每 120 分鐘。
  - 主畫面只顯示本機繪製的天氣圖示與溫度；英文地名預設不顯示，快取超過 6 小時自動隱藏。
  - 精簡排列預設開啟：不顯示地名時，天氣圖示與溫度會放在日期左側；也可切回獨立天氣列。
- **App Icon**：深藍底、青綠相片山脈與 `12:00`，包含 API 17 舊式 PNG 與 API 26+ Adaptive Icon。
- **夜間暗屏**：支援定時降低亮度並暫停相片動畫。
- **字型授權資訊**：
  - 開源字型採 SIL OFL 1.1；Storopia 的分發授權尚未確認。

---

## 🛠️ 建置與編譯

本專案使用原生 Android SDK 與 Gradle 構建；唯一執行期依賴是向下相容的 AndroidX ExifInterface 1.3.7。

支援範圍為 Android 4.2（API 17）至 Android 16（API 36）。Android 17 目前為 Beta，專案已依 API 37 的公開相容性變更完成靜態檢查，但仍應在正式版系統映像推出後補跑裝置測試。

### 使用 PowerShell 一鍵構建：
```powershell
.\build.ps1
```
構建會固定產生一組雙版本：

- `dist/LittleClock-v2.2.0-test-storopia.apk`：含 Storopia，僅供內部測試。
- `dist/LittleClock-v2.2.1.apk`：功能相同，但 APK 內完全不含 Storopia。

---

## 📄 授權說明 (Licenses)

- **主程式碼**：MIT License / Open Source
- **內嵌字型**：SIL Open Font License 1.1 (詳細聲明請見 `app/src/main/assets/fonts/LICENSES.txt`)
