# LittleClock 通用線私有日照樣式

本分支以通用線 `codex/v4.0.28-weather-toggles` 的 `41fa54d` 為基準，
不包含紅米專用的私人相簿 Provider，只使用既有 `storopiaTest` flavor 產生
Storopia 私有 APK。

日照區塊採用 QuietPanel 的呈現方式：12sp 白色文字、黑色陰影、14dp
日照線、18dp 文字列，並讓 220dp 寬度隨天氣字體比例縮放。所有「原始排版」
模式（包含「整體綁定」與「自動避讓」）都會把日出／日落文字置中；「自由
排版」則靠右。

## 分支與產物

- 分支：`codex/universal-private-daylight-desktop`
- 通用線回復點：`littleclock-universal-before-daylight-style`
- 本次改版前回復點：`littleclock-universal-before-autoavoid-daylight-centered`
- 預計完成標籤：`v4.1.1-universal-private`
- 標準 APK：
  `dist\LittleClock-v4.1.1.apk`
- Storopia 私有 APK：
  `dist\LittleClock-v4.1.1-test-android4.2-storopia.apk`

若不採用這個小改版，切回 `codex/universal-daylight-style-private` 即可回復
上一個通用私有版；該分支沒有被本分支修改。
