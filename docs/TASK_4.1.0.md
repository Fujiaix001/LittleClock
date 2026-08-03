# LittleClock 4.1.0 詳細執行任務

## 0. 任務目標

將 LittleClock 從 `4.0.29` 升級為 `4.1.0`（對外可簡稱 4.1），加入以下五項功能，同時維持 Android 4.2（API 17）到目前 `targetSdk 36` 的相容性與輕量定位：

1. 可選擇顯示秒數。
2. 可選擇相片播放順序。
3. 可手動切換與保存情境模式。
4. 充電守護。
5. 電量顯示。

這是一份實作任務書。執行者應依照工作包順序完成，不得自行增加其他功能。

---

## 1. 不可違反的產品限制

### 1.1 Android 與建置限制

- 必須保留 `minSdk = 17`。
- 必須保留 `targetSdk = 36`，除非使用者另外明確要求升級。
- 使用 Java 8、Android Framework View 與目前的 Gradle 結構。
- 不導入 Compose、Kotlin、協程或新的第三方執行期依賴。
- 不新增背景常駐服務、WorkManager、JobScheduler 或定期 AlarmManager 工作。
- 不新增任何 Android 權限。
- 保留 standard 與 storopiaTest 兩個 flavor。
- 保留目前 release 簽章設定與 application ID。

### 1.2 天氣網路限制（絕對不可更動）

- `WeatherClient` 在 Android 4.2～4.4 將 HTTPS 降級為 HTTP 的既有行為必須原樣保留。
- 不得刪除、改寫、封鎖或繞過以下邏輯：API 19 以下把 `https://` 改為 `http://`。
- 不得因安全掃描、lint 建議或重構而停用舊 Android 天氣。
- 本版本不修改天氣傳輸協定、API 端點、更新頻率或快取策略。
- 最終必須以差異檢查確認 `WeatherClient.java` 沒有非必要變動。

### 1.3 EXIF 限制

- EXIF 僅保留目前必要的相片方向修正。
- 播放順序禁止逐張讀取 EXIF。
- 最新／最舊排序只能使用：
  - File 來源：`File.lastModified()`。
  - SAF 來源：`DocumentsContract.Document.COLUMN_LAST_MODIFIED`。
- 檔名排序只能使用：
  - File 來源：`File.getName()`。
  - SAF 來源：`DocumentsContract.Document.COLUMN_DISPLAY_NAME`。

### 1.4 效能限制

- 秒數必須使用獨立、可停止的 1 Hz UI ticker；不得讓整個相片主 ticker 永久每秒執行。
- 相片排序必須在目前的背景相簿掃描執行緒進行，不得在主執行緒排序大型清單。
- 排序用的臨時資料在排序完成後應可被釋放。
- 電池狀態使用系統 sticky broadcast／動態 receiver；不得建立輪詢執行緒。
- Activity 不在前景時必須停止秒數 ticker 並解除動態 receiver。
- 不增加常駐 Bitmap、全畫面動畫或逐幀電池動畫。

### 1.5 相容與升級限制

- 升級後，既有使用者的預設畫面和播放行為不得改變。
- 新增設定的預設值必須維持 4.0.29 行為：
  - 秒數：關閉。
  - 播放順序：目前的隨機且一輪不重複。
  - 目前情境：預設。
  - 保持亮屏：永遠保持亮屏。
  - 低電量守護：關閉。
  - 電量顯示：關閉。
- 現有收藏、隱藏、鬧鐘、番茄鐘、天氣快取與 SAF 授權不得遺失。

---

## 2. 明確排除的內容

本版本不要實作：

- 快速控制列。
- 首次操作教學。
- 12／24 小時制切換。
- 依 EXIF 拍攝時間排序。
- 情境模式定時自動切換。
- 情境模式依充電狀態自動切換。
- 自訂情境名稱、刪除情境或無限新增情境。
- 剩餘充電時間估算。
- 電池溫度顯示給一般使用者。
- 充電動畫。
- 影片、GIF、雲端同步或帳號系統。
- 任何天氣功能重構。

---

## 3. 建議的檔案結構

不要將所有功能直接塞入已很大的 `PhotoClockActivity` 與 `SettingsActivity`。新增以下小型元件；若實作過程發現名稱需微調可以調整，但責任邊界不可混在一起：

### 正式程式碼

- `ClockSecondPolicy.java`
  - 秒數模式常數、normalize、是否應顯示秒數的純邏輯。
- `PlaybackOrderPolicy.java`
  - 播放順序常數、normalize、名稱／時間排序 comparator。
- `DisplayProfileStore.java`
  - 四個固定情境、白名單設定複製、初始化與套用。
- `PowerStatePolicy.java`
  - 保持亮屏、低電量降級與低電量暫停的純邏輯。
- `PowerStateMonitor.java`
  - 前景動態接收電池狀態，輸出不可變的狀態快照。
- `BatteryStatusView.java`
  - 使用 Canvas/Paint 繪製電池、百分比和充電符號。

### 單元測試

- `ClockSecondPolicyTest.java`
- `PlaybackOrderPolicyTest.java`
- `DisplayProfileStoreTest.java` 或可在純 Java 測試的 `DisplayProfilePolicyTest.java`
- `PowerStatePolicyTest.java`

單元測試不得依賴模擬器才能執行；核心判斷應抽成純 Java。

---

## 4. 工作包 A：建立基準與版本前檢查

### A-1. 確認工作區

- 執行 `git status --short`。
- 不覆蓋使用者既有變更。
- 記錄目前 HEAD。
- 執行現有單元測試：

```powershell
.\gradlew.bat :app:testStandardDebugUnitTest :app:testStoropiaTestDebugUnitTest --no-daemon
```

- 若基準測試失敗，先判斷是否為既有問題；不要把無關修正混入 4.1.0。

### A-2. 保留基準資料

記錄：

- 目前標準 APK 大小約 5.80 MB。
- 目前 `minSdk 17`、`targetSdk 36`。
- 目前執行期依賴只有 AndroidX ExifInterface。
- 目前 `WeatherClient` API 19 以下的 HTTP 降級程式碼位置。

### A 驗收

- [ ] 現有測試結果已記錄。
- [ ] 無任何功能碼變更。
- [ ] 已確認沒有要覆蓋的使用者變更。

---

## 5. 工作包 B：電池狀態共用基礎

先做這一包，因為秒數「只在充電時顯示」、充電守護與電量顯示都依賴同一份狀態。

### B-1. PowerStateMonitor

建立一個僅在 `PhotoClockActivity` 前景存在的監測器：

- 使用 `Intent.ACTION_BATTERY_CHANGED`。
- 啟動時先讀 sticky intent，立即取得初始狀態。
- Activity `onResume()` 註冊。
- Activity `onPause()` 解除註冊。
- 重複呼叫 register/unregister 必須安全。
- 捕捉舊廠商 ROM 可能拋出的例外，不能使 App 崩潰。

快照至少包含：

- `boolean batteryPresent`
- `boolean plugged`
- `int levelPercent`，未知時使用 `-1`
- `float temperatureCelsius`，未知時使用 `Float.NaN`
- 可選擇保存原始 charging status，但 UI 不需要顯示充電速度。

判定規則：

- `EXTRA_PRESENT == false` 的電視盒視為無電池、長期供電。
- AC、USB 與可用時的 wireless plug 都視為 plugged。
- 缺少 level/scale 時，百分比為 `-1`，不可除以零。
- 無法讀取狀態時維持目前永遠亮屏行為，不可錯誤關閉螢幕。

### B-2. 整合既有溫度保護

- 目前 `PhotoClockActivity.readBatteryTemperature()` 每分鐘讀 sticky battery intent。
- 改成優先使用 `PowerStateMonitor` 最新快照的溫度。
- 如果尚無快照，可保留一次安全 fallback 讀取。
- 不得同時存在兩套長期 receiver。
- 保留既有展演模式熱降級門檻與行為。

### B 驗收

- [ ] 前景時可收到插電、拔電與電量變化。
- [ ] 背景時 receiver 已解除。
- [ ] 重複進出 Activity 沒有 receiver leak 或 IllegalArgumentException。
- [ ] 無電池電視盒不會被判定為低電量。
- [ ] 既有溫度效能保護仍運作。
- [ ] 沒有新增 manifest receiver、service 或權限。

---

## 6. 工作包 C：顯示秒數

### C-1. 設定模型

新增 SharedPreferences key，例如：

```java
public static final String CLOCK_SECONDS_MODE = "clock_seconds_mode";
```

定義三種模式：

- `0 = 關閉`（預設）
- `1 = 只在充電時顯示`
- `2 = 永遠顯示`

`ClockSecondPolicy` 提供：

- `normalize(int mode)`
- `shouldShowSeconds(int mode, boolean batteryPresent, boolean plugged)`

無電池電視盒在「只在充電時顯示」模式下視為已供電，可顯示秒數。

### C-2. 設定頁 UI

在「時鐘顯示」區段加入 Spinner：

- 不顯示秒數
- 充電時顯示秒數
- 永遠顯示秒數

要求：

- 預設選擇「不顯示秒數」。
- 儲存後立即套用。
- 字型沿用目前時間字型。
- 不加入 12 小時制或 AM/PM。

### C-3. 時間顯示

- 秒數關閉時維持 `HH:mm`。
- 秒數啟用時顯示 `HH:mm:ss`。
- 先使用既有 `photoTime` TextView，避免重建整個時間版面。
- 確認橫向、直向、原始排版與自由排版都不崩潰。
- 若文字變寬，只能使用既有縮放／邊界限制處理，不要新增高成本 AutoSizeTextView 依賴。

### C-4. 獨立秒數 ticker

新增獨立 Runnable：

- 只更新時間文字，不做相片掃描、換圖、天氣、鬧鐘、番茄鐘或夜間模式檢查。
- 下一次延遲使用 `1000 - (System.currentTimeMillis() % 1000)` 對齊整秒，並設合理最小值避免 0ms 迴圈。
- 僅在以下條件全部成立時排程：
  - Activity resumed。
  - 秒數依設定和充電狀態應顯示。
  - 非夜間暗屏狀態。
- `onPause()`、`onDestroy()`、秒數關閉、拔電而模式為充電顯示、進入夜間暗屏時，立即移除 callback。
- 離開秒數模式後立即把文字恢復為 `HH:mm`。
- 番茄鐘既有每秒 ticker 不得被破壞，也不要建立無限重複的雙重全畫面更新。

### C-5. 測試

至少測試：

- 無效 mode 回到關閉。
- 關閉永不顯示。
- 充電模式在 plugged 時顯示。
- 充電模式在一般電池未插電時不顯示。
- 無電池電視盒在充電模式下顯示。
- 永遠模式在未插電時仍顯示。

### C 驗收

- [ ] 關閉秒數時，主時鐘仍主要按分鐘更新。
- [ ] 秒數開啟時，只有時間文字每秒更新。
- [ ] 切換插拔電源會正確啟停秒數。
- [ ] 夜間暗屏停止秒數 ticker。
- [ ] 旋轉與返回前景不會產生多個 ticker。
- [ ] API 17 不使用較新未防護 API。

---

## 7. 工作包 D：播放順序

### D-1. 設定模型

新增 key，例如：

```java
public static final String PHOTO_PLAYBACK_ORDER = "photo_playback_order";
```

定義五種順序：

- `0 = 隨機且一輪不重複`（預設，完全沿用目前行為）
- `1 = 檔名 A → Z`
- `2 = 檔名 Z → A`
- `3 = 最新優先`
- `4 = 最舊優先`

設定頁在「相片停留時間」附近加入 Spinner，標題「播放順序」。

### D-2. PhotoSource 排序資料

讓 `PhotoSource` 可提供：

- 穩定識別 `stableKey`
- `displayName`
- `lastModified`

File 來源：

- `displayName = file.getName()`
- `lastModified = file.lastModified()`

SAF 來源：

- 在原本 children query projection 增加 `COLUMN_LAST_MODIFIED`。
- 使用 `COLUMN_DISPLAY_NAME` 作為名稱。
- 某些 provider 不提供修改時間時使用 `0L`，不可因此捨棄相片或崩潰。
- 不得為每張 SAF URI 再發一次 query。

### D-3. 排序規則

`PlaybackOrderPolicy` 應使用穩定、可測試的比較規則：

- 名稱使用 `Locale.US` 的大小寫無關比較。
- 名稱相同時以 `stableKey` 作最後 tie-breaker。
- 最新優先：有效時間由大到小。
- 最舊優先：有效時間由小到大。
- `lastModified <= 0` 視為未知時間，無論最新或最舊都放在有效時間項目之後。
- 未知時間彼此以名稱再以 stableKey 排序。
- 隨機模式不要排序清單，交給既有隨機 deck。

所有排序必須在 `photoScanExecutor` 的背景工作中完成，然後才把結果發佈到主執行緒。

### D-4. PlaybackNavigator

目前 `PlaybackNavigator` 永遠 shuffle deck，需要擴充為兩種導航模式：

- random：維持目前洗牌、一輪不重複、跨輪避免第一張等於上一輪最後一張。
- sequential：依已排序的 list index 由 0 到最後，然後循環。

要求：

- 保留最多 50 筆上一張歷史。
- 手動上一張後，再按下一張要回到剛才的項目。
- sequential 到最後一張後回到第一張。
- 只有一張照片時不能出錯。
- reset/resetAt 必須同時接受或保存導航模式。
- 保留現有建構方式的相容性，讓舊測試容易調整。

### D-5. 切換順序與相簿簽章

- 將播放順序加入相簿 catalog signature，切換順序後會重建已排序清單。
- 盡量保留目前照片：排序完成後用 stable key 找回目前 index，再呼叫對應 resetAt。
- 切換順序不應重新解碼目前照片；下一次換圖才使用新順序。
- 收藏／隱藏／只播收藏仍先過濾，再排序。

### D-6. 禁止事項

- 不得讀 EXIF。
- 不得依照片內容分析排序。
- 不得在 UI thread 排 15,000～50,000 筆資料。
- 不得永久保存多份完整相片清單。
- 不得因 SAF provider 缺少 lastModified 而移除相片。

### D-7. 測試

至少新增：

- 名稱正序、反序。
- 大小寫名稱的穩定結果。
- 名稱相同時 stableKey tie-breaker。
- 最新、最舊。
- 未知時間固定放尾端。
- random 模式不由 policy 改變清單順序。
- sequential navigator 完整循環。
- sequential 上一張／下一張歷史。
- random navigator 現有不重複測試全部繼續通過。

### D 驗收

- [ ] 五種播放順序都可從設定頁選擇。
- [ ] 隨機模式與 4.0.29 行為一致。
- [ ] 排序不讀 EXIF。
- [ ] 排序在背景執行。
- [ ] 50,000 筆上限沒有建立永久第二份 catalog。
- [ ] SAF 缺少修改時間時仍可播放。
- [ ] 收藏、隱藏及上一張功能正常。

---

## 8. 工作包 E：充電守護

### E-1. 保持亮屏模式

新增全域設定 key 與三種模式：

- `0 = 永遠保持亮屏`（預設，沿用目前行為）
- `1 = 只有充電時保持亮屏`
- `2 = 跟隨 Android 系統休眠`

設定頁新增 Spinner「保持螢幕亮起」。

套用規則：

- 永遠：加入 `FLAG_KEEP_SCREEN_ON`。
- 只有充電時：plugged 或無電池裝置時加入；未插電時清除。
- 跟隨系統：清除 `FLAG_KEEP_SCREEN_ON`。
- 充電狀態改變時立即重新套用。
- 狀態未知時採保守相容策略，維持亮屏。
- 不修改系統全域亮度與休眠設定。

將目前 `onCreate()` 中無條件 `addFlags(FLAG_KEEP_SCREEN_ON)` 改為透過單一方法集中控制，例如 `applyKeepScreenOnPolicy()`。

### E-2. 低電量守護

新增全域 checkbox：「低電量時降低相片耗電」，預設關閉。

第一版採固定門檻，不增加複雜設定 UI：

- 有電池、未插電、電量 `<= 20%`：有效效能模式暫時降為 ECO，不改寫使用者保存的效能模式。
- 有電池、未插電、電量 `<= 10%`：
  - 暫停自動換圖。
  - 停止 Ken Burns／相片平移動畫。
  - 時鐘、日期、天氣顯示、鬧鐘和番茄鐘仍正常。
- 電量回到 `>= 15%` 或插電：解除換圖／平移暫停。
- 電量回到 `>= 25%` 或插電：解除強制 ECO，恢復使用者原選擇，但仍受既有溫度和記憶體壓力保護。
- 使用 hysteresis，避免在 10/20% 附近反覆切換。
- 不因低電量取消或修改天氣 HTTP/HTTPS 行為。

`PowerStatePolicy` 應把上述判斷抽成純函式，避免散落大量百分比條件。

### E-3. 動畫與換圖整合

- 低電量暫停時，如果平移 animator 正在執行，安全停止並維持目前照片。
- 不回收目前 Bitmap。
- 不清空 playback history。
- 不變更 next photo index。
- 恢復後從目前照片重新計算下一次換圖時間，避免立刻連跳數張。
- 手動滑動上一張／下一張仍允許；只暫停自動播放與持續動畫。
- 夜間暗屏優先於低電量狀態；喚醒後仍依電量政策決定是否恢復動畫。

### E-4. 測試

至少測試：

- 永遠亮屏模式。
- 充電才亮屏模式的插電／未插電。
- 無電池電視盒視為供電。
- 跟隨系統模式不保持亮屏。
- 低電量守護關閉時不改效能。
- 20% 強制 ECO。
- 10% 暫停動畫／自動換圖。
- 插電立即解除低電量限制。
- hysteresis 恢復門檻。
- 未知百分比不觸發低電量限制。

### E 驗收

- [ ] 預設行為仍為永遠亮屏。
- [ ] 充電限定模式插拔電源立即生效。
- [ ] 跟隨系統模式不再持有 keep-screen-on flag。
- [ ] 低電量守護不修改保存的效能模式。
- [ ] 手動換圖、時鐘、鬧鐘、番茄鐘不受低電量暫停破壞。
- [ ] 不新增背景服務或輪詢。

---

## 9. 工作包 F：電量顯示

### F-1. 顯示模式

新增設定 key 與三種模式：

- `0 = 不顯示`（預設）
- `1 = 永遠顯示`
- `2 = 只在未充電時顯示`

設定頁在時鐘／進階顯示附近加入 Spinner「電量顯示」。

### F-2. BatteryStatusView

使用自繪 View，避免 emoji／字型在 Android 4.2 廠商 ROM 上不一致：

- 顯示時固定放在主畫面右上角，不併入日期列，也不受時鐘區塊拖曳、情境版面切換或舊版排列影響。

- 畫出簡單電池外框與正極凸點。
- 依 0～100% 畫填充比例。
- 顯示整數百分比，例如 `82%`。
- 充電時用簡單幾何閃電符號，不使用字型 glyph。
- `<= 20%` 使用橘色。
- `<= 10%` 使用紅色。
- 其他狀態沿用時鐘主要／次要色彩。
- 未知電量不顯示錯誤百分比，可隱藏或顯示簡單電池輪廓。
- 無電池電視盒隱藏。
- 提供中文 content description，例如「電量 82%，充電中」。

### F-3. 版面位置

- 放在現有日期列中，避免新增第四個獨立可拖曳時鐘區塊。
- 跟隨 dateBlock 移動與縮放。
- 與 compact weather、鬧鐘提示保留合理間距。
- 原始排版、自由排版、橫向、直向都不得超出畫面或壓縮成 0。
- 番茄鐘專注版面若隱藏一般日期列，電量也隨日期列隱藏，不另外懸浮。

### F-4. 更新策略

- 共用 `PowerStateMonitor`，狀態改變時才更新。
- 不建立新的 Handler、Timer 或 Thread。
- 不做持續充電動畫。
- 電量顯示關閉時 View 使用 `GONE`，不要每次 tick 重畫。

### F 驗收

- [ ] 三種顯示模式正確。
- [ ] 充電、未充電、低電量顏色正確。
- [ ] 無電池裝置不顯示假資料。
- [ ] TalkBack content description 有意義。
- [ ] 沒有每秒輪詢或動畫。
- [ ] 日期、鬧鐘、精簡天氣排列未被破壞。

---

## 10. 工作包 G：情境模式

### G-1. 固定情境

第一版只提供四個固定情境，不提供新增、刪除或改名：

- `default`：預設
- `desk`：桌面
- `bedside`：床頭
- `showcase`：展示

新增全域 active profile key，例如：

```java
public static final String ACTIVE_DISPLAY_PROFILE = "active_display_profile";
```

### G-2. 初始化與升級

第一次執行 4.1.0 時：

- 將使用者目前 live preferences 複製成 `default` 情境。
- `desk`、`bedside`、`showcase` 也先複製目前設定，不要自動覆蓋成激進模板。
- active profile 設為 `default`。
- 寫入 profile schema version，避免每次啟動重新初始化。
- 收藏、隱藏、鬧鐘、番茄鐘、天氣快取和 SAF 授權只保留原始全域資料，不複製進 profile。

如此升級後四個情境一開始看起來相同，使用者可自行調整各情境。

### G-3. Profile 白名單

`DisplayProfileStore` 必須使用明確白名單，禁止直接複製整份 SharedPreferences。

應屬於情境的設定：

- 相簿資料夾集合。
- 相片停留時間。
- 播放順序。
- 轉場。
- 相片顯示模式。
- 字型選擇。
- 顯示時間、日期、天氣。
- 秒數模式。
- 天氣顯示排版選項；天氣地點與快取仍為全域。
- 效能模式。
- 時鐘原始／自由排版模式。
- 時間、日期、天氣大小及位置，包括橫向／直向 keys。
- 時間底板、依相片調色、白色相框、智慧取景、防烙印微移、自動亮度。
- 夜間暗屏啟用、開始與結束時間。
- 只播放收藏的布林值；收藏集合本身仍為全域。
- 電量顯示模式。

不得屬於情境的資料：

- favorite photo set。
- hidden photo set。
- weather location、座標、timezone、current/extended cache、last refresh。
- alarm keys。
- pomodoro session、phase、remaining time、完成次數及 duration 設定。
- SAF persisted permission bookkeeping。
- 充電守護的保持亮屏模式與低電量守護開關；這兩項是全域安全政策。
- last displayed photo、暫時錯誤、catalog cache。

### G-4. 儲存格式

- 可使用同一個 SharedPreferences，profile key 前綴例如 `profile.default.`。
- 必須正確複製 boolean、int、long、float、String、StringSet。
- StringSet 必須建立 defensive copy，不可直接重用 SharedPreferences 回傳集合。
- 套用 profile 時只覆蓋白名單中的 live keys。
- profile schema 需有版本號，以便日後加入設定。

### G-5. 設定頁互動

在設定頁最上方加入「目前情境」Spinner：

- 預設
- 桌面
- 床頭
- 展示

互動採下列簡化規格：

1. SettingsActivity 開啟時顯示 active profile。
2. 使用者選擇不同 profile 時顯示確認對話框：「切換後會立即載入此情境；目前尚未套用的設定將捨棄。」
3. 確認後：
   - 套用目標 profile 白名單到 live preferences。
   - 更新 active profile。
   - 重新建立 SettingsActivity UI 或安全 recreate，使畫面顯示新設定。
4. 取消後 Spinner 回到原 active profile，不做任何變更。
5. 使用者在某個 active profile 修改設定並按目前既有「套用」時：
   - 先照目前流程寫入 live preferences。
   - 再把白名單 live values 捕捉回 active profile。
6. 按「取消」時不保存本次 UI 編輯；但先前已確認的 profile 切換仍然有效。

使用 guard flag 避免 Spinner 初始 `onItemSelected` 自動觸發切換對話框。

### G-6. Activity 執行時行為

- Profile 切換後回到 `PhotoClockActivity`，沿用目前 `onResume()` 重新載入設定流程。
- 相簿或播放順序不同時，catalog signature 必須使相簿重建。
- 相同目前照片若仍在新 profile 相簿內，盡量保留；不在則載入新 profile 的第一張。
- 不同 profile 的橫向／直向時鐘位置各自保存。
- Profile 切換不能取消正在執行的鬧鐘或番茄鐘。

### G-7. 測試

至少測試：

- 非法 profile ID 回到 default。
- 初次初始化四個 profile。
- 白名單值可以保存與套用。
- StringSet defensive copy。
- 全域收藏／隱藏不被 profile 覆蓋。
- 天氣 location/cache 不被覆蓋。
- 鬧鐘與番茄鐘不被覆蓋。
- 充電守護全域設定不被覆蓋。
- active profile 保存與恢復。

### G 驗收

- [ ] 四個情境可以切換並各自保存顯示／相簿設定。
- [ ] 升級後預設情境完全保留 4.0.29 設定。
- [ ] 切換情境不會遺失收藏、隱藏、鬧鐘、番茄鐘或天氣快取。
- [ ] SAF 權限不被複製或釋放。
- [ ] 情境切換不需要背景服務。
- [ ] 取消設定不保存未套用編輯。

---

## 11. 工作包 H：版本、文件與發行

所有功能及測試完成後才執行版本更新。

### H-1. Gradle 版本

在 `app/build.gradle.kts` 修改：

```kotlin
val versionMajor = 4
val versionMinor = 1
val basePatch = 0
```

預期：

- `versionCode = 40100`
- standard `versionName = 4.1.0`
- storopiaTest `versionName = 4.1.0-test-android4.2-storopia`

不要改 versionCode 計算公式。

### H-2. README

更新 README：

- 標題改為 LittleClock 4.1.0。
- 核心功能補上秒數、播放順序、四情境、充電守護、電量顯示。
- 建置輸出檔名更新為：
  - `LittleClock-v4.1.0.apk`
  - `LittleClock-v4.1.0-test-android4.2-storopia.apk`
- 保留天氣在舊 Android 的現有支援說明，不加入「停用 HTTP」文字。

### H-3. Release note

新增 `docs/RELEASE_4.1.0.md`，至少包含：

- 顯示秒數三種模式。
- 五種播放順序，明確註明不讀 EXIF。
- 四個手動情境。
- 三種保持亮屏模式與低電量守護。
- 電量顯示。
- Android 4.2 相容性。
- 無新增權限、背景服務或第三方執行期依賴。
- 舊 Android 天氣取得方式維持不變。
- 從 4.0.29 使用同一簽章可覆蓋升級。

### H-4. 不要修改歷史文件

- 不要把 `docs/RELEASE_4.0.x.md` 內的歷史版本號批次改成 4.1.0。
- 搜尋所有 `4.0.29`／舊 README 版本文字，只更新代表「目前版本」的地方。

---

## 12. 完整測試要求

### 12.1 自動測試

至少執行：

```powershell
.\gradlew.bat :app:testStandardDebugUnitTest :app:testStoropiaTestDebugUnitTest --no-daemon
.\gradlew.bat :app:lintStandardRelease :app:lintStoropiaTestRelease --no-daemon
```

最終發行建置：

```powershell
.\build.ps1
```

`build.ps1` 必須成功完成：

- app tests
- standard release lint
- storopiaTest release lint
- 兩個 release APK
- SHA256SUMS.txt

### 12.2 手動測試矩陣

最低測試版本分組：

- API 17／Android 4.2：基本啟動、秒數、File 相簿、電池顯示、亮屏模式、天氣仍可取得。
- API 19／Android 4.4：舊儲存、天氣 HTTP 降級仍可取得。
- API 21：SAF tree、檔名與 lastModified 排序。
- API 23：runtime storage permission 路徑。
- API 29：系統字型與 scoped-storage 邊界。
- API 33：媒體與通知權限。
- API 34 或以上：鬧鐘既有全螢幕／精確權限流程。
- API 36：targetSdk 36 最新行為與 lint。

若沒有所有模擬器，至少實測 API 17、21、33/34、36，並在交付說明列出未測版本。

### 12.3 手動功能情境

必測：

1. 秒數關閉、充電顯示、永遠顯示。
2. 顯示秒數時旋轉十次，確認只有一個 ticker。
3. 重複進出設定與背景，確認無 receiver leak。
4. 五種播放順序各播放跨過最後一張。
5. 上一張後再下一張。
6. 隱藏、收藏、只播收藏與排序組合。
7. SAF provider 缺少 lastModified 的降級行為。
8. 四個情境各保存不同字型、相簿與位置。
9. 切換情境時鬧鐘和番茄鐘保持原狀。
10. 永遠亮屏、充電亮屏、系統休眠三種模式。
11. 低電量守護門檻與插電恢復。
12. 電量顯示的充電、未充電、20%、10%、未知和無電池狀態。
13. 夜間暗屏與秒數／低電量守護的組合。
14. 天氣正常更新，API 19 以下 HTTP 降級仍存在。

---

## 13. 資源預算

完成後應以實際產物回報，不可只說「影響很小」。目標預算：

- standard release APK：預期約 5.85～6.00 MB，建議不得無理由超過 6.10 MB。
- 不新增大型 asset。
- 秒數關閉時不得出現每秒主 ticker wakeup。
- 正常常駐記憶體新增目標小於 0.5 MB，不含排序短暫資料。
- 15,000 張排序暫時記憶體目標約 0.5～1.5 MB。
- 50,000 張排序暫時記憶體目標約 2～5 MB。
- 排序結束後不得永久保留第二份 50,000 筆 catalog。
- 電量顯示不得使用持續動畫。

如果實測超過預算，先找出原因，不要以提高預算作為第一個解法。

---

## 14. 最終程式碼檢查

執行並人工檢查：

```powershell
git status --short
git diff --stat
git diff -- app/src/main/java/com/quietphoto/clock/WeatherClient.java
rg -n "ExifInterface|EXIF" app/src/main/java/com/quietphoto/clock
rg -n "versionMajor|versionMinor|basePatch|minSdk|targetSdk" app/build.gradle.kts
rg -n "implementation\(" app/build.gradle.kts
```

必須確認：

- `WeatherClient.java` 沒有不必要變動。
- API 19 以下 HTTP 降級仍存在。
- 新排序程式沒有呼叫 ExifInterface。
- minSdk 仍為 17。
- targetSdk 仍為 36。
- dependencies 沒有新增執行期套件。
- AndroidManifest 沒有新增 permission、receiver 或 service。
- 所有 Handler callbacks 和 receiver 都有成對清理。
- 沒有把 keystore、keystore.properties 或 local.properties 加入 Git。

---

## 15. 最終交付物

執行者完成後應提供：

- 所有修改檔案清單。
- 五項功能各自的完成摘要。
- 自動測試與 lint 結果。
- 實際測過的 Android API 版本。
- 未測試的 API 版本與原因。
- standard/storopiaTest APK 路徑。
- SHA-256 檔案路徑。
- 4.0.29 與 4.1.0 standard APK 的實際大小差異。
- 確認未新增權限、背景服務與第三方執行期依賴。
- 確認天氣 HTTP 降級未變。
- 確認排序未讀 EXIF。

預期輸出：

- `dist/LittleClock-v4.1.0.apk`
- `dist/LittleClock-v4.1.0-test-android4.2-storopia.apk`
- `dist/SHA256SUMS.txt`
- `docs/RELEASE_4.1.0.md`

---

## 16. 建議提交順序

若需要分段提交，建議：

1. `refactor: add shared foreground power state monitor`
2. `feat: add optional clock seconds display`
3. `feat: add lightweight photo playback ordering`
4. `feat: add charging guard and battery display`
5. `feat: add manual display profiles`
6. `release: prepare LittleClock 4.1.0`

不要在功能尚未通過測試時先建立 4.1.0 release APK。

---

## 17. 完成定義

只有同時符合以下條件才算完成：

- [x] 五項功能全部符合本任務書。
- [x] 版本為 4.1.0／versionCode 40100。
- [ ] API 17 程式碼可編譯；基本功能實機測試待有 Android 4.2 裝置或 adb 後補測。
- [x] standard 與 storopiaTest 單元測試全部通過。
- [x] standard 與 storopiaTest release lint 全部通過。
- [x] standard 與 storopiaTest release APK 成功產生。
- [x] APK 大小符合資源預算，standard 增加 5,497 bytes、storopiaTest 增加 5,558 bytes。
- [x] 沒有新增權限、背景服務或第三方執行期依賴。
- [x] 沒有用 EXIF 排序。
- [x] Android 4.2～4.4 天氣 HTTP 降級保持原樣。
- [x] 舊使用者升級後預設行為不變。
- [x] README 與 4.1.0 release note 已更新。
