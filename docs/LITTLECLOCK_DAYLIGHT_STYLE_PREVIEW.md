# LittleClock 日照樣式預覽

本分支將日照區塊改成 QuietPanel 的呈現方式：12sp 白色靠右文字、黑色陰影、
14dp 日照線、18dp 文字列，並讓 220dp 寬度隨天氣字體比例縮放。

## 回滾點

- 改版前 Git 標籤：`littleclock-before-quietpanel-daylight-style`
- 改版前 APK：
  `dist\LittleClock-v42.0.3-redmi42-before-quietpanel-daylight-style.apk`
- 預覽分支：`codex/littleclock-daylight-style-preview`

若要保留 App 設定並回復舊畫面，可用相同簽章直接覆蓋安裝改版前 APK：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r `
  .\dist\LittleClock-v42.0.3-redmi42-before-quietpanel-daylight-style.apk
```

若只要回復原始碼，切回 `codex/special-redmi-android42` 分支即可。
