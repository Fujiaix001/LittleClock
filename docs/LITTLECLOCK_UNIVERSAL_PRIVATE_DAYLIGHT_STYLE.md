# LittleClock 通用線私有日照樣式

本分支以通用線 `codex/v4.0.28-weather-toggles` 的 `41fa54d` 為基準，
不包含紅米專用的私人相簿 Provider，只使用既有 `storopiaTest` flavor 產生
Storopia 私有 APK。

日照區塊採用 QuietPanel 的呈現方式：12sp 白色文字、黑色陰影、14dp
日照線、18dp 文字列，並讓 220dp 寬度隨天氣字體比例縮放。「整體綁定」
模式會把日出／日落文字置中；其他排版模式則靠右。

## 分支與產物

- 分支：`codex/universal-daylight-style-private`
- 通用線回復點：`littleclock-universal-before-daylight-style`
- 私有 APK：
  `dist\LittleClock-v4.1.0-private-storopia-quietpanel-daylight.apk`

若不採用這個小改版，切回 `codex/v4.0.28-weather-toggles` 即可回復通用線
原始碼；它沒有被本分支修改。
