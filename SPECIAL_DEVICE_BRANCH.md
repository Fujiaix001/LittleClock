# 特定裝置版：Redmi／Android 4.2 私有相簿

這個分支的版本標記是 `v4.1.1-redmi-android42-private-album`。

- Git 分支：`codex/special-redmi-android42`
- 對應手機：第一代 Redmi，Android 4.2.2
- 依賴：`PhonePrivateAlbum` 的 `com.quietphoto.privatealbum.photos` Provider
- 用途：只服務這台舊手機及其既有的私有相簿配置

這是裝置專用版本，不是通用功能的基線。未來通用版應從整合前的
`9a8a4c5` 基線另開分支，避免把舊手機專用 Provider、Android 4.2 相容
處理和相關選項帶進所有新裝置。

本次只重新定義分支與版本標記，沒有重新編譯；目前已安裝的 Storopia APK
仍保留原本的 Android `versionName` `4.1.1-test-android4.2-storopia`。
