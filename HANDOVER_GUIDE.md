# 📑 「小時鐘」 (LittleClock) 專案維護交接指南

本文檔為開啟新對話繼續進行專案維護、功能擴充或 Bug 修正時的**無縫接續指南**。

---

## 📌 1. 專案基本資訊與位置

- **專案名稱**：小時鐘 (LittleClock)
- **當前版本**：`v4.1.1-test-android4.2-storopia` / `v4.1.1`
- **專案絕對路徑**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock-photo-pan-frame-throttle`
- **測試包位置**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock-photo-pan-frame-throttle\dist\LittleClock-v4.1.1-test-android4.2-storopia.apk`
- **標準包位置**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock-photo-pan-frame-throttle\dist\LittleClock-v4.1.1.apk`
- **授權狀態**：Storopia 的分發授權尚未確認；包含此字型的 APK 僅供內部測試。

---

## 📱 專案定位與 AI 維護原則

- **老設備優先**：本專案以舊手機、平板、電視盒長時間穩定運作為主要目標；Android 4.2（API 17）是必須持續保留的最低相容版本。
- **限制先於功能**：新增 API、第三方依賴、背景服務或常駐輪詢前，必須先確認不破壞 API 17、低記憶體與低 CPU 設備的使用情境。
- **AI 全程維護**：本專案的架構設計、程式修改、測試與文件維護由 AI 完成；每次變更仍應保留可重現的建置、測試與實機驗證紀錄。
- **效能取向**：優先選擇有界的記憶體配置、單一背景工作、可停止的動畫與低頻排程；避免為了新裝置效果而犧牲老設備的穩定性與續航。

---

## 🛠️ 2. 技術架構與設計哲學

1. **核心技術**：原生 Android Java（`compileSdk 36`, `targetSdk 36`, `minSdk 17`）。
2. **最小依賴**：僅使用 AndroidX ExifInterface；標準 release APK 約 **5.6 MB**（包含內建精選風景資產）。
3. **字型資產**：
   - 採用 `FontSubsetting` 技術收錄 13 款開源字型與 Storopia 內部測試字型；新增臺灣繁中圓體 jf open 粉圓 2.1 子集。依 OFL 保留名稱條款，子集內部 family name 改為 `LittleClock FenYuan`。
   - 時間、日期與天氣分別保存 `clock_font_id`、`date_font_id`、`weather_font_id`；舊安裝若無後兩項會沿用時間字型。
   - **Latin 字型規則**：日期選擇 Orbitron、Audiowide、Oxanium、Saira Stencil、Zen Dots 或 Storopia 時，日期與星期自動切換為英文格式（例如 `Wed, Jul 22`）。
4. **專屬 App Icon**：深藍底、青綠相片山脈與 `12:00`；API 26+ 使用 Adaptive Icon。

---

## 🌟 3. 已完成的核心功能與關鍵優化

- **字型目錄集中管理 (v1.4.0-test)**：
  - 所有字型名稱、資產路徑與日期語系邏輯集中於 `FontManager`，並由建置任務檢查標準版 13 個字型檔。
- **時間／日期／天氣獨立字型**：
  - 設定頁提供三組字型選單；日期語系只跟日期字型連動，天氣的溫度與地名則共用天氣字型。
  - `prepare_fonts.py` 可接受檔名參數，只重建指定子集，例如 `python prepare_fonts.py font_huninn.ttf`。
- **低階裝置播放控制 (v1.5.0-test)**：
  - 左右滑動、固定 50 筆播放歷史、一輪不重複、防烙印微移與直橫向獨立位置。
  - 低耗電模式預設開啟；柔和背景只使用 48×48 Bitmap，進階像素分析可完全停用。
  - 支援收藏、隱藏、環境光亮度與 MediaStore 變更監聽。
- **🔍 雙指捏合手勢拉大/縮小時鐘 (Pinch-to-Zoom)**：
  - 支援雙指任意放大幅度 (`0.4x` ~ `6.0x`) 與單指拖曳位置，並透過 `CLOCK_SCALE_FACTOR` 自動持久化記憶。
- **相片解碼上限與 OOM 降級重試**：
  - 依螢幕解析度限制解碼像素並使用 RGB_565，避免不必要的完整原圖解碼。
  - 遇 OOM 時將 `inSampleSize` 放大 2 倍重試；仍需以低記憶體實機驗證極端案例。
- **📸 照片 EXIF 角度自動修正**：
  - 解碼時自動讀取相機拍攝的 `TAG_ORIENTATION` 進行矩陣旋轉修正。
- **權限與效能優化**：
  - API 29+ 透過 MediaStore 查詢相片，API 17–28 才使用檔案系統掃描。
  - API 34+ 支援完整與部分相片權限，所有掃描與解碼共用單一背景執行緒。
  - `SharedPreferences` 設定儲存全數改用非同步 `apply()`。
- **系統相容性**：
  - `minSdk 17`、`targetSdk 36`；支援 Android 16 預測返回與 edge-to-edge。
  - APK 無 native `.so`，可直接支援 16 KB page size 裝置。
  - Android 17 目前為 Beta；已檢查公開相容性變更，正式版推出後仍需補跑 emulator/device 測試。
- **天氣與系統字型 (v2.0)**：
  - Open-Meteo、手動英文地名、無 GPS／Google 服務、前景與 Wi-Fi 限定更新，舊系統明確啟用 TLS 1.2。
  - 天氣預設關閉；一般每 60 分鐘、低耗電每 120 分鐘，8 秒逾時、6 小時快取上限。
  - API 17–28 提供基本系統字型；API 29+ 透過公開 `SystemFonts` API 列出裝置字型，僅載入使用中的一款。
- **天氣精簡排列 (v2.1)**：
  - 無地名時預設把天氣圖示、溫度與日期放在同一列；顯示地名或關閉精簡模式時使用獨立天氣列。
- **啟動優化 (v2.2)**：
  - 優先解碼上次成功顯示的照片，再以背景優先級更新完整相簿索引。
  - 主畫面不再列舉全部系統字型，並移除初始版面的重複字型套用。
- **跨版本穩定性 (v4.0.6)**：
  - 開機、App 更新、時區／系統時間變更與精準鬧鐘授權後，重新建立一般鬧鐘；番茄鐘在開機後依儲存的牆上時間恢復。
  - Android 13+ 在啟用鬧鐘時請求通知權限；Android 14+ 視需要引導全螢幕鬧鐘權限。
  - 內建數位風景使用版本化安裝，升級會補齊新增圖片。
- **固定雙版本流程**：
  - Storopia 版本使用獨立套件名稱，能與標準版並存。
  - 標準版與測試版使用相同人類可讀版本號，但各自由 Gradle 管理內部版本與簽章。

---

## 🚀 4. 常用維護與構建指令

在 PowerShell 或命令提示字元中執行：

```powershell
# 1. 切換至專案目錄
cd C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock

# 2. 一鍵編譯與打包 Release APK
powershell.exe -ExecutionPolicy Bypass -File .\build.ps1

# 3. 檢查 Git 狀態
git status
```

---

## 🧠 5. 已持久化保存的開發規範 (Memory)

> **「日後所有程式寫作與檔案修改任務前，必須優先檢查目標工作目錄是否已透過 Git 進行版本管理。若是，須在變更與構建過程中配合 Git 狀態進行維護。」**

---

## 💬 6. 新對話接續提示 (在新對話直接發送以下文字即可)

```text
我們正在維護「小時鐘」(LittleClock) Android 專案，專案路徑為 C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock-photo-pan-frame-throttle，當前版本為 v4.1.1-test / v4.1.1，已完成 Git 初始化。請參考該目錄下的 HANDOVER_GUIDE.md 並在開始寫作前先檢查 Git 管理狀態。
```
