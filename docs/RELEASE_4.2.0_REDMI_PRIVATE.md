# LittleClock 4.2.0 Redmi 私人相簿版

## 專用功能

- 讀取 `PhonePrivateAlbum` 的 App 私有空間照片。
- 在 4.2 分類式設定頁中啟用或停用私人相簿。
- 透過快取的 `/folders` Provider 查詢選擇來源資料夾。
- 相容只提供 `/photos` 的舊版 Provider。
- 私人相簿異動或 App 回到前景時重新整理照片索引。
- 保留返回設定頁前正在顯示的照片，降低舊 Redmi 的重解碼與畫面閃動。

## 相容性

- 第一代 Redmi／Android 4.2.2（API 17）專用。
- 需要 `PhonePrivateAlbum` 1.2.0 以上；建議使用 1.3.0。
- `versionName`：`4.2.0-redmi42-private`
- `versionCode`：`420100`
- 沒有新增第三方依賴或 Android 權限。
