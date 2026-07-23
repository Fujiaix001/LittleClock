# 📑 「小時鐘」 (LittleClock) 專案維護交接指南

本文檔為開啟新對話繼續進行專案維護、功能擴充或 Bug 修正時的**無縫接續指南**。

---

## 📌 1. 專案基本資訊與位置

- **專案名稱**：小時鐘 (LittleClock)
- **當前版本**：`v2.2.0-test` / `v2.2.1`
- **專案絕對路徑**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock`
- **測試包位置**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock\dist\LittleClock-v2.2.0-test-storopia.apk`
- **標準包位置**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock\dist\LittleClock-v2.2.1.apk`
- **授權狀態**：Storopia 的分發授權尚未確認；包含此字型的 APK 僅供內部測試。

---

## 🛠️ 2. 技術架構與設計哲學

1. **核心技術**：原生 Android Java（`compileSdk 36`, `targetSdk 36`, `minSdk 17`）。
2. **最小依賴**：僅使用 AndroidX ExifInterface 1.3.7；雙版本 release APK 約 **1.08 MB**。
3. **字型資產**：
   - 採用 `FontSubsetting` 技術收錄 11 款開源字型與 Storopia 內部測試字型。
   - **Latin 字型規則**：選擇 Orbitron、Audiowide、Oxanium、Saira Stencil、Zen Dots 或 Storopia 時，日期與星期自動切換為英文格式（例如 `Wed, Jul 22`）。
4. **專屬 App Icon**：深藍底、青綠相片山脈與 `12:00`；API 26+ 使用 Adaptive Icon。

---

## 🌟 3. 已完成的核心功能與關鍵優化

- **字型目錄集中管理 (v1.4.0-test)**：
  - 所有字型名稱、資產路徑與日期語系邏輯集中於 `FontManager`，並由建置任務檢查 12 個字型檔。
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
- **固定雙版本流程**：
  - `2.0.0-test` 含 Storopia；`2.0.1` 功能相同且實際移除字型檔。
  - 後續版本沿用「含 Storopia 的 `-test` 版，再增加 `0.0.1` 建立無 Storopia 版」規則。

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
我們正在維護「小時鐘」(LittleClock) Android 專案，專案路徑為 C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock，當前版本為 v2.2.0-test / v2.2.1，已完成 Git 初始化。請參考該目錄下的 HANDOVER_GUIDE.md 並在開始寫作前先檢查 Git 管理狀態。
```
