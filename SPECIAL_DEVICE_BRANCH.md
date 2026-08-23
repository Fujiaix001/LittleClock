# 特定裝置版：第一代 Redmi／Android 4.2 私人相簿

- Git 分支：`v4.2-redmi-private-album`
- 工作目錄：`current-redmi-private-v4.2.0`
- Android 版本：4.2.2（API 17）
- LittleClock 版本：`4.2.0-redmi42-private`
- `versionCode`：`420100`，可直接更新舊 `42.0.x` Redmi 專用版
- 依賴：`PhonePrivateAlbum` 1.2.0 以上的
  `com.quietphoto.privatealbum.photos` Provider

這個分支保留通用 4.2 的友善設定頁，但額外讀取手機私有相簿照片，並可依
原始匯入資料夾選擇播放範圍。Provider 僅供讀取；照片的匯入與刪除仍由
`PhonePrivateAlbum` 負責。

「私人」指照片存放於 PhonePrivateAlbum 的 App 私有資料目錄，不會成為一般相簿
資料夾。Provider 為合作 App 提供唯讀內容，並非照片加密功能。

這是裝置專用版本，不可作為通用 LittleClock 的合併基線。
