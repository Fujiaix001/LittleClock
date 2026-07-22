# ⏰ 小時鐘 (LittleClock)

一軟體極致輕量 (~840 KB)、無廣告、無背景常駐、零第三方依賴的 Android 高顏值數位相框時鐘 App。
專為新舊平板、手機、電視電視盒或 Amazon Fire 平板打造，能將老舊裝置變身為藝廊質感的專屬桌面畫布時鐘。

---

## ✨ 核心特色與亮點

- **🔍 雙指捏合手勢自由縮放 (Pinch-to-Zoom)**：支援在螢幕上雙指拉大或縮小時鐘至任意尺寸，並自動記憶偏好設定。
- **🖼️ 智慧相片展示與輕量解碼**：
  - 相片背景平滑推頁/淡入/3D 轉場與溫和 Ken Burns 鏡頭微推效果。
  - 專利智慧視覺重心 (Entropy Focal) 畫面自動對焦。
  - **相片 EXIF 方向自動修正**：相機直拍/橫拍照片自動旋轉呈現。
  - 記憶體極致防護 (**v1.3.0** 新增)：內建 `maxPixels` 像素上限保險鎖與 OOM 自動降級重試機制，不論原圖為 4,000萬或 1億像素，解碼記憶體均強制鎖定在 <5MB，老手機 (如紅米一代) 永不當機。
- **🔤 11 款全開源 CJK/中英科技字型 (FontSubsetting)**：
  - 收錄 Orbitron (幾何科技)、DotGothic16 (經典電子鐘)、Noto Sans/Serif JP、Zen Maru Gothic (圓體)、Klee One (楷體)、Dela Gothic One (重磅厚黑)、Audiowide (極致科技)、Oxanium (硬派電競)、Saira Stencil One (重磅鏤空) 與 Zen Dots (圓潤未來)。
  - 字型全數採用 `FontSubsetting` 技術，全檔僅約 ~250KB。
- **🎨 專屬極簡綠底 App Icon**：配備標準 Android 桌面與電視介面導向圖示。
- **🔌 充電自動連動與夜間暗屏**：
  - 接上電源自動開啟。
  - 支援定時護眼夜間暗屏休眠。
- **🛡️ 100% 正則開源合規 (SIL OFL 1.1)**：
  - 全專案無版權疑慮，適合個人使用、公開分發與商業展示。

---

## 🛠️ 建置與編譯

本專案使用原生 Android SDK 與 Gradle 構建，零外部庫依賴。

### 使用 PowerShell 一鍵構建：
```powershell
.\build.ps1
```
構建完成後的 APK 將會自動產出於 `dist/LittleClock-v1.3.2.apk`。

---

## 📄 授權說明 (Licenses)

- **主程式碼**：MIT License / Open Source
- **內嵌字型**：SIL Open Font License 1.1 (詳細聲明請見 `app/src/main/assets/fonts/LICENSES.txt`)
