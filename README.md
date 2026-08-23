# ⏰ 小時鐘 (LittleClock) 4.2.0 Redmi 私人相簿版

一款輕量、無廣告、無背景常駐的 Android 相片時鐘；僅使用 AndroidX ExifInterface 處理相片方向。
適用於新舊平板、手機、電視盒與 Amazon Fire 平板。

> **專用版本**：此分支只供第一代 Redmi／Android 4.2.2 使用，並依賴
> `PhonePrivateAlbum` 的 `com.quietphoto.privatealbum.photos` 唯讀 Provider。

> **專案定位與維護方式**：本專案專為老舊 Android 設備長時間穩定展示而設計，最低相容目標固定為 Android 4.2（API 17）。功能、依賴與效能取捨都必須先符合舊設備限制；本專案由 AI 全程設計與維護，所有變更仍需經過編譯、測試與實機驗證。

---

## 核心功能

- **時鐘縮放**：支援雙指縮放，並記憶偏好設定。
- **友善設定頁**：所有功能依相簿、時鐘、天氣、電源、專注工具與相片效果分類；類別可獨立展開，儲存操作固定在畫面底部。
- **手機私有相簿**：可播放 `PhonePrivateAlbum` 中的照片，並依原始匯入資料夾選擇播放範圍；相簿異動後自動重新讀取。
- **可選秒數**：可關閉、只在充電時顯示，或永遠顯示秒數；秒數使用獨立輕量更新，不增加相片解碼工作。
- **播放手勢**：左滑下一張、右滑上一張；一輪播完前不重複。
- **播放順序**：可選隨機、檔名正序／反序、最新優先或最舊優先；排序使用檔名與檔案時間，不讀取 EXIF。
- **低耗電模式**：預設停用像素分析、柔和背景與裝飾效果，適合低階裝置。
- **漸進式相簿**：找到第一張可用照片就開始播放；其餘照片在背景建立索引。SAF 資料夾會定期低優先序更新，不打斷播放。
- **相片管理**：長按相片可收藏或隱藏，並可選擇只播放收藏。
- **三種顯示方式**：填滿、完整顯示，以及選用的低解析度柔和背景。
- **相片展示與輕量解碼**：
  - 相片背景平滑推頁/淡入/3D 轉場與溫和 Ken Burns 鏡頭微推效果。
  - 依畫面明暗分布調整取景位置。
  - **相片 EXIF 方向自動修正**：相機直拍/橫拍照片自動旋轉呈現。
  - 限制解碼像素並在記憶體不足時降級重試。
- **13 款開源字型與 1 款內部測試字型**：
  - 收錄 Orbitron、DotGothic16、Noto Sans/Serif JP、Zen Maru Gothic、Klee One、Dela Gothic One、芫荽、LittleClock 粉圓體（源自 jf open 粉圓）、Audiowide、Oxanium、Saira Stencil One 與 Zen Dots。
  - 時間、日期與天氣可分別選擇字型；日期格式由日期字型決定，因此科技感拉丁數字可搭配繁中文字型。
  - 字型採用 `FontSubsetting` 技術縮減檔案大小。
  - 標準版 13 個 `.ttf` 合計約 798 KB；CJK 字型只保留介面實際需要的日期中文字，Latin 字型使用英文日期。
  - Storopia 僅供內部測試，授權確認前不可分發含此字型的 APK。
- **系統字型**：Android 4.2 起可選系統無襯線、襯線與等寬字型；Android 10 以上另列出裝置實際安裝的系統字型，不複製進 APK。
- **選用天氣**：
  - 手動搜尋並保存英文地名，不使用 GPS、Google Play 服務或位置權限。
  - 僅在 App 前景且連接 Wi-Fi 時向 Open-Meteo 更新；一般模式每 60 分鐘、低耗電模式每 120 分鐘。
  - 主畫面只顯示本機繪製的天氣圖示與溫度；英文地名預設不顯示，快取超過 6 小時自動隱藏。
  - 精簡排列預設開啟：不顯示地名時，天氣圖示與溫度會放在日期左側；也可切回獨立天氣列。
- **App Icon**：深藍底、青綠相片山脈與 `12:00`，包含 API 17 舊式 PNG 與 API 26+ Adaptive Icon。
- **夜間暗屏**：支援定時降低亮度並暫停相片動畫。
- **情境模式**：可手動保存與切換預設、桌面、床頭、展示四組顯示與相簿設定；收藏、鬧鐘、番茄鐘及天氣快取維持全域。
- **充電守護**：可選永遠亮屏、僅充電時亮屏或跟隨系統休眠；選用低電量守護時會降低相片動畫與自動換圖耗電。
- **電量顯示**：可選擇永遠顯示或只在未充電時顯示；顯示時固定在主畫面右上角，自繪電池圖示相容 Android 4.2 舊裝置。
- **番茄鐘與鬧鐘**：番茄鐘可在背景結束時震動提醒；鬧鐘會在開機、時區／系統時間變更與精準鬧鐘權限授予後重新排程。
- **字型授權資訊**：
  - 開源字型採 SIL OFL 1.1；Storopia 的分發授權尚未確認。

---

## 🛠️ 建置與編譯

本專案使用原生 Android SDK 與 Gradle 構建；唯一執行期依賴是向下相容的 AndroidX ExifInterface 1.3.7。

支援範圍為 Android 4.2（API 17）至 Android 16（API 36）。Android 5.0 以上以系統資料夾選擇器授權相簿；Android 4.2–4.4 保留舊版資料夾瀏覽器。

### 使用 PowerShell 一鍵構建：
```powershell
.\build.ps1
```
構建會固定產生一組雙版本：

- `dist/LittleClock-v4.2.0-redmi42-private-test-storopia.apk`：含 Storopia，僅供內部測試，使用獨立套件名稱。
- `dist/LittleClock-v4.2.0-redmi42-private.apk`：Redmi 標準正式版，不含 Storopia。

首次建立 release APK 前，請妥善保存專案根目錄的 `keystore.properties` 與 `keystore/littleclock-release.jks`；兩者已被 Git 忽略，遺失後無法用同一簽章更新既有正式版。

---

## 📄 授權說明 (Licenses)

- **主程式碼**：MIT License / Open Source
- **內嵌字型**：SIL Open Font License 1.1 (詳細聲明請見 `app/src/main/assets/fonts/LICENSES.txt`)
