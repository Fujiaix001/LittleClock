# 📑 「小時鐘」 (LittleClock) 專案維護交接指南

本文檔為開啟新對話繼續進行專案維護、功能擴充或 Bug 修正時的**無縫接續指南**。

---

## 📌 1. 專案基本資訊與位置

- **專案名稱**：小時鐘 (LittleClock)
- **當前版本**：`v1.3.0`
- **專案絕對路徑**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock`
- **正式發行包位置**：`C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock\dist\LittleClock-v1.3.0.apk`
- **Git 版本管理狀態**：
  - 已完成 `git init` 初始化與 `.gitignore` 設置。
  - 主分支 `master` 已完成 Initial Commit (`5f9e64d` - 包含 `v1.3.0` 全部源碼與資源)。
  - 已設定本地提交身分 `hsuchungming <hsuchungming@local>`。

---

## 🛠️ 2. 技術架構與設計哲學

1. **核心技術**：原生 Android Java（`compileSdk 36`, `targetSdk 36`, `minSdk 17`）。
2. **零外部依賴 (Zero Dependencies)**：無任何第三方 Gradle 庫，完全使用原生 Android API 打造，APK 僅約 **804 KB**。
3. **100% 正則開源字型 (SIL OFL 1.1)**：
   - 採用 `FontSubsetting` 技術收錄 7 款開源字型：`Orbitron` (幾何科技體)、`DotGothic16` (電子鐘)、`Noto Sans/Serif JP`、`Zen Maru Gothic` (圓體)、`Klee One` (楷體)、`Dela Gothic One` (厚黑)。
   - **Orbitron 特殊規則**：選擇 Orbitron 時鐘風格時，日期與星期自動切換為英文格式（例如 `Wed, Jul 22`）。
4. **專屬 App Icon**：純淺綠底極簡 Orbitron 「Clock」文字圖示 (`@mipmap/ic_launcher`)。

---

## 🌟 3. 已完成的核心功能與關鍵優化

- **🔍 雙指捏合手勢拉大/縮小時鐘 (Pinch-to-Zoom)**：
  - 支援雙指任意放大幅度 (`0.4x` ~ `6.0x`) 與單指拖曳位置，並透過 `CLOCK_SCALE_FACTOR` 自動持久化記憶。
- **🛡️ 超大相片 `maxPixels` 解碼安全鎖 + OOM 降級重試**：
  - 限制解碼總像素在 250 萬像素內，單張照片記憶體強制鎖定在 **< 5MB**。
  - 遇極端 OOM 時自動將 `inSampleSize` 放大 2 倍重試解碼，老手機 (如 1GB RAM 紅米一代) 永不當機。
- **📸 照片 EXIF 角度自動修正**：
  - 解碼時自動讀取相機拍攝的 `TAG_ORIENTATION` 進行矩陣旋轉修正。
- **📱 權限與效能優化**：
  - API 33+ 自動請求 `READ_MEDIA_IMAGES` 權限。
  - `SharedPreferences` 設定儲存全數改用非同步 `apply()`。

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
我們正在維護「小時鐘」(LittleClock) Android 專案，專案路徑為 C:\Users\hsuchungming\Desktop\WorkSpace\LittleClock，當前版本為 v1.3.0，已完成 Git 初始化。請參考該目錄下的 HANDOVER_GUIDE.md 並在開始寫作前先檢查 Git 管理狀態。
```
