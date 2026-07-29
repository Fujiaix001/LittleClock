# LittleClock 移植至 iPad mini 1（iOS 9／armv7）完整實作指南

本文件是交給 Mac 上 AI agent 的執行規格。目標不是逐行翻譯 Android Java，而是在不改動 Android 專案的前提下，以 Objective-C、UIKit 與 iOS 9 原生框架重做相同行為，保留 LittleClock 的核心精神：輕量、高效、簡單、美觀、離線可用、能在低階硬體長時間穩定運行。

## 0. 先理解硬體與工具鏈限制

- iPad mini 第 1 代使用 32 位元 A5、512 MB RAM，最高為 iOS 9.3.5；行動網路版可能是 iOS 9.3.6。
- 2015 MacBook Pro 官方最高支援 macOS Monterey。
- 固定使用 **Xcode 13.4.1**。Xcode 13 支援 iOS 9 裝置；Xcode 14 起移除 armv7、armv7s、i386，且不再支援 iOS 11 以前的 deployment target。
- 使用 **Objective-C + UIKit**，不要使用 Swift、SwiftUI、Combine、CocoaPods 或第三方圖片框架。Objective-C 可避免 Swift runtime、舊 armv7 套件與 ABI 相容問題，也更容易精確控制記憶體。
- 這是重新實作，不是把 APK 轉成 IPA。Android Activity、Service、AlarmManager、MediaStore 與 SharedPreferences 都不能直接搬到 iOS。

參考：

- [Apple：Xcode 13 Release Notes](https://developer.apple.com/documentation/xcode-release-notes/xcode-13-release-notes)
- [Apple：Xcode 13.4.1 Release Notes](https://developer.apple.com/documentation/xcode-release-notes/xcode-13_4_1-release-notes)
- [Apple：Xcode 14 移除 armv7 與 iOS 11 以下目標](https://developer.apple.com/documentation/Xcode-Release-Notes/xcode-14-release-notes)
- [Apple：2015 MacBook Pro 最高支援 macOS Monterey](https://support.apple.com/en-us/108052)
- [Apple：舊版 Xcode 下載入口](https://developer.apple.com/download/all/)

## 1. 不可違反的實作原則

AI agent 在每次修改前都必須遵守：

1. 不修改 `LittleClock` Android 專案；在同層建立 `LittleClock-iOS9`。
2. 每完成一個里程碑就建立 Git commit，先在 iOS 9 實機驗證，再進下一階段。
3. deployment target 固定 `9.0`，Release 架構固定 `armv7`。
4. 所有執行路徑都不得依賴 iOS 10 以上 API；使用新 API 時必須有 availability guard 與 iOS 9 fallback。
5. 不一次載入原始尺寸相片，不把一萬張照片解碼或複製成 `UIImage` 陣列。
6. 主執行緒只做 UIView 更新；檔案列舉、影像縮圖、JSON 解析放到 serial background queue。
7. 同時最多保留「目前圖片＋下一張預載圖片」；收到 memory warning 立刻清空預載與快取。
8. 一般時鐘不做每秒輪詢；只有顯示秒數或番茄鐘運行時才每秒更新。
9. 預設關閉 Ken Burns、即時模糊、Core Image filter、持續 CADisplayLink 與粒子效果。
10. 每個功能必須在 iPad mini 1 實機量測，不以模擬器順暢作為完成標準。

## 2. 安裝正確的編譯環境

### 2.1 確認 macOS 與機型

在 Mac Terminal 執行：

```bash
sw_vers
system_profiler SPHardwareDataType | grep -E "Model Name|Model Identifier|Chip|Processor"
```

預期 macOS 為 Monterey 12.x。若 Mac 使用 OpenCore Legacy Patcher 跑更新系統，也不要改用 Xcode 14；本專案仍以 Xcode 13.4.1 的 armv7 工具鏈為準。

### 2.2 下載及安裝 Xcode 13.4.1

1. 使用 Apple Account 登入 `https://developer.apple.com/download/all/`。
2. 搜尋 `Xcode 13.4.1`，下載 `.xip`。
3. 雙擊解壓縮後重新命名並放到 `/Applications/Xcode_13.4.1.app`。
4. 不要覆蓋其他 Xcode；多版本可以共存。

執行：

```bash
sudo xcode-select --switch /Applications/Xcode_13.4.1.app/Contents/Developer
sudo xcodebuild -runFirstLaunch
xcodebuild -version
xcrun --sdk iphoneos --show-sdk-path
xcrun --sdk iphoneos clang --version
```

預期 `xcodebuild -version` 顯示 Xcode 13.4.1。

若系統只找到 Command Line Tools，重新執行 `xcode-select --switch`，不要把 SDK 從另一版 Xcode 手工混入。

### 2.3 建立工作目錄與 Git

假設 Android 原始碼已複製到 Mac 的 `~/Projects/LittleClock`：

```bash
mkdir -p ~/Projects/LittleClock-iOS9
cd ~/Projects/LittleClock-iOS9
git init
printf ".DS_Store\nDerivedData/\nbuild/\n*.xcuserstate\nxcuserdata/\nrelease/\n" > .gitignore
git add .gitignore
git commit -m "chore: initialize iOS 9 port"
```

## 3. 建立 Xcode 專案

使用 Xcode 13.4.1：

1. File → New → Project → iOS → App。
2. Product Name：`LittleClockLegacy`。
3. Organization Identifier：`com.quietphoto`。
4. Interface：Storyboard 或 UIKit；Language：**Objective-C**。
5. 不勾 Core Data、Tests。
6. 儲存在 `~/Projects/LittleClock-iOS9`。

建立後移除 Main storyboard 依賴，整個 UI 由程式碼建立，避免 storyboard 在舊版尺寸與旋轉上留下隱性約束：

- Target → General → Main Interface 清空。
- `Info.plist` 移除 `UIMainStoryboardFile`。
- `AppDelegate` 自己建立 `UIWindow` 與 root view controller。

`AppDelegate.m` 的基本內容：

```objective-c
#import "AppDelegate.h"
#import "LCClockViewController.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application
        didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    LCClockViewController *controller = [[LCClockViewController alloc] init];
    self.window.rootViewController = controller;
    [self.window makeKeyAndVisible];
    return YES;
}

@end
```

### 3.1 Target Build Settings

在 Xcode 對 `LittleClockLegacy` target 設定：

| 設定 | Debug | Release |
|---|---:|---:|
| iOS Deployment Target | 9.0 | 9.0 |
| Architectures | armv7 | armv7 |
| Build Active Architecture Only | Yes | No |
| Enable Bitcode | No | No |
| Optimization Level | None (`-O0`) | Fastest, Smallest (`-Os`) |
| Dead Code Stripping | Yes | Yes |
| Strip Linked Product | No | Yes |
| Generate Debug Symbols | Yes | Yes |
| Debug Information Format | DWARF | DWARF with dSYM |
| Automatic Reference Counting | Yes | Yes |

也可以用命令檢查最終設定：

```bash
cd ~/Projects/LittleClock-iOS9
xcodebuild -project LittleClockLegacy.xcodeproj \
  -scheme LittleClockLegacy \
  -configuration Release \
  -sdk iphoneos \
  -showBuildSettings | grep -E "ARCHS|IPHONEOS_DEPLOYMENT_TARGET|ENABLE_BITCODE|GCC_OPTIMIZATION_LEVEL"
```

不可出現 `arm64 only`，也不可把 deployment target 自動提高到 11。

### 3.2 Info.plist

加入或確認：

```xml
<key>CFBundleDisplayName</key>
<string>小時鐘</string>
<key>LSRequiresIPhoneOS</key>
<true/>
<key>MinimumOSVersion</key>
<string>9.0</string>
<key>UIDeviceFamily</key>
<array><integer>2</integer></array>
<key>UIRequiresFullScreen</key>
<true/>
<key>UISupportedInterfaceOrientations~ipad</key>
<array>
  <string>UIInterfaceOrientationPortrait</string>
  <string>UIInterfaceOrientationPortraitUpsideDown</string>
  <string>UIInterfaceOrientationLandscapeLeft</string>
  <string>UIInterfaceOrientationLandscapeRight</string>
</array>
<key>NSPhotoLibraryUsageDescription</key>
<string>選擇並播放您的相片作為時鐘背景。</string>
<key>UIFileSharingEnabled</key>
<true/>
```

若加入自訂字型，另加：

```xml
<key>UIAppFonts</key>
<array>
  <string>Fonts/font_iansui.ttf</string>
  <string>Fonts/font_orbitron.ttf</string>
  <string>Fonts/font_sans.ttf</string>
</array>
```

第一版只帶實際會用的 3–5 款字型，不要為了選單把所有字型都載入記憶體。需要更多字型時再個別加入。

## 4. 建議的 iOS 專案結構

```text
LittleClock-iOS9/
├── LittleClockLegacy/
│   ├── AppDelegate.h/.m
│   ├── Controllers/
│   │   ├── LCClockViewController.h/.m
│   │   └── LCSettingsViewController.h/.m
│   ├── Views/
│   │   ├── LCClockPanelView.h/.m
│   │   ├── LCWeatherIconView.h/.m
│   │   └── LCPhotoActionView.h/.m
│   ├── Models/
│   │   ├── LCPreferences.h/.m
│   │   ├── LCPomodoroState.h/.m
│   │   └── LCAlarmState.h/.m
│   ├── Services/
│   │   ├── LCPhotoCatalog.h/.m
│   │   ├── LCImageLoader.h/.m
│   │   ├── LCWeatherClient.h/.m
│   │   ├── LCPomodoroScheduler.h/.m
│   │   └── LCAlarmScheduler.h/.m
│   ├── Resources/
│   │   ├── DigitalLandscapes/
│   │   ├── FocusReminders/
│   │   └── Fonts/
│   └── Info.plist
├── Scripts/build-ipa.sh
└── README.md
```

不要建立一個數千行的 ViewController。相片目錄、縮圖解碼、番茄鐘狀態、鬧鐘排程與天氣各自成為小型類別；類別之間使用簡單 delegate/block 回傳結果。

## 5. Android 功能到 iOS 9 的映射

| Android 現有設計 | iOS 9 實作 |
|---|---|
| `PhotoClockActivity` | `LCClockViewController` + 小型 service/model |
| `SettingsActivity` | `UITableViewController`，使用可重用 cell |
| `SharedPreferences` | `NSUserDefaults` |
| `MediaStore` / SAF | Photos.framework 的 `PHFetchResult`；另支援 Documents/Photos |
| `AlarmManager` | `UILocalNotification` + 儲存絕對結束時間 |
| `Vibrator` | iPad mini 1 沒有震動馬達；改為視覺提醒或可選輕柔音效 |
| `SensorManager` 環境光 | 公開 iOS SDK 無直接環境光 API；移除或改成手動夜間亮度 |
| `Handler` | `NSTimer` 或 GCD timer；畫面離開立即 invalidate/cancel |
| 自訂 Drawable | `UIView drawRect:` / `CAShapeLayer`，靜態圖形只建立一次 |
| Android alarm foreground service | iOS 9 local notification；一般 App 無法保證背景全螢幕響鈴 |

### 無法一比一保留的地方

1. **震動**：iPad mini 1 本身沒有震動馬達，任何 API 都無法創造實體震動。番茄鐘結束可顯示全畫面提醒、短暫提高畫面亮度，或提供可關閉的柔和提示音。
2. **背景鬧鐘畫面**：iOS 9 一般 App 在背景不能像 Android foreground service 一樣任意跳出全螢幕 Activity。使用 `UILocalNotification`；使用者點通知後回到 App 的鬧鐘畫面。
3. **環境光自動亮度**：沒有公開 API，不能使用 private API。保留夜間時段與手動亮度即可。

不要因為已越獄就把第一版寫成 system tweak。先完成普通 sandboxed App；只有 local notification 確實不符合需求時，再把鬧鐘 daemon/tweak 拆成另一個獨立專案。

## 6. 分階段改寫順序

### Milestone 1：時鐘＋內建照片

- 全螢幕、隱藏 status bar。
- 顯示時間、日期。
- 讀取 Bundle 內建照片，按設定間隔交叉淡入。
- 橫直式手工排版；分別保存時鐘位置與比例。
- 先不要做 Photos、天氣、番茄鐘、鬧鐘。

驗收後 commit：

```bash
git add LittleClockLegacy
git commit -m "feat: add lightweight clock and bundled slideshow"
```

### Milestone 2：相簿與漸進播放

- 請求 Photos 權限。
- 使用 `PHFetchResult<PHAsset *>`，不要把所有照片轉成陣列。
- 取得第一個可用 asset 後立即播放；後續索引與 shuffle 狀態背景準備。
- 支援左滑／右滑、收藏、隱藏。
- 收藏與隱藏只保存 `PHAsset.localIdentifier` 字串集合。

### Milestone 3：設定與字型

- 使用 `UITableViewController`，自然支援直式捲動。
- 滑桿使用節點吸附 15/30/40/60/120 秒。
- 設定先存在 controller 的 draft model；按「套用」才寫入 `NSUserDefaults`。
- 長按照片使用 `UILongPressGestureRecognizer.minimumPressDuration = 0.9`。
- 相片選單使用自訂緊湊 view，不使用高度不可控的預設 action sheet。

### Milestone 4：番茄鐘

- 狀態只保存 `phase`、`running`、`endDate`、`remainingWhenPaused`。
- 運行時剩餘秒數永遠以 `endDate - now` 計算，不逐秒累減後保存。
- 進入背景時排程 `UILocalNotification`；回到前景立即重算。
- 直式倒數完全置中；橫式採左上偏移；番茄鐘模式鎖定排版。
- 不專心提醒依方向隨機使用 ANG/CRY 圖片，停留 3 秒。

### Milestone 5：鬧鐘、天氣與省電收尾

- 每日鬧鐘使用 local notification，重複或每次重新計算下一次時間。
- 天氣用 `NSURLSession`、HTTPS、60 分鐘磁碟快取；失敗時保持舊快取，不阻塞 UI。
- 加入夜間亮度、低耗電模式、memory warning 處理與 Instruments 實測。

## 7. 高效相片目錄設計

### 7.1 Photos.framework

`LCPhotoCatalog` 不應保存 `UIImage`，只保存來源與播放索引：

```objective-c
@interface LCPhotoCatalog : NSObject
@property (nonatomic, strong) PHFetchResult *assets;
@property (nonatomic, strong) NSIndexSet *hiddenIndexes;
@property (nonatomic, assign) NSUInteger currentIndex;
- (void)reloadWithCompletion:(dispatch_block_t)completion;
- (PHAsset *)nextAsset;
@end
```

抓取時只建立 lazy fetch result：

```objective-c
PHFetchOptions *options = [[PHFetchOptions alloc] init];
options.sortDescriptors = @[[NSSortDescriptor sortDescriptorWithKey:@"creationDate"
                                                            ascending:NO]];
self.assets = [PHAsset fetchAssetsWithMediaType:PHAssetMediaTypeImage options:options];
```

`PHFetchResult` 本身適合大量照片；不要執行這類程式：

```objective-c
// 禁止：一口氣建立一萬個 UIImage
NSMutableArray *images = [NSMutableArray array];
for (PHAsset *asset in result) {
    [images addObject:[self fullResolutionImageForAsset:asset]];
}
```

### 7.2 請求螢幕尺寸影像

iPad mini 1 螢幕為 1024×768、scale 1。圖片目標長邊以 1280 像素為上限；不取 12MP/48MP 原圖。

```objective-c
- (PHImageRequestID)requestDisplayImage:(PHAsset *)asset
                              completion:(void (^)(UIImage *image))completion {
    PHImageRequestOptions *options = [[PHImageRequestOptions alloc] init];
    options.networkAccessAllowed = NO;
    options.resizeMode = PHImageRequestOptionsResizeModeFast;
    options.deliveryMode = PHImageRequestOptionsDeliveryModeOpportunistic;
    options.synchronous = NO;

    CGSize screen = UIScreen.mainScreen.bounds.size;
    CGFloat scale = UIScreen.mainScreen.scale;
    CGSize target = CGSizeMake(screen.width * scale * 1.25,
                               screen.height * scale * 1.25);

    return [[PHImageManager defaultManager]
        requestImageForAsset:asset
                  targetSize:target
                 contentMode:PHImageContentModeAspectFill
                     options:options
               resultHandler:^(UIImage *image, NSDictionary *info) {
        if (!image) return;
        dispatch_async(dispatch_get_main_queue(), ^{ completion(image); });
    }];
}
```

換圖前用 `cancelImageRequest:` 取消尚未完成的舊請求。`resultHandler` 可能回呼低品質及較高品質兩次；只替換同一個 image view，不把兩張保存在陣列。

### 7.3 Bundle／Documents 圖片降採樣

不要使用 `imageWithContentsOfFile:` 直接解壓完整大圖。使用 ImageIO thumbnail：

```objective-c
#import <ImageIO/ImageIO.h>

static UIImage *LCDownsampleImage(NSURL *url, NSUInteger maxPixel) {
    NSDictionary *sourceOptions = @{(id)kCGImageSourceShouldCache: @NO};
    CGImageSourceRef source = CGImageSourceCreateWithURL((__bridge CFURLRef)url,
                                                         (__bridge CFDictionaryRef)sourceOptions);
    if (!source) return nil;
    NSDictionary *thumbOptions = @{
        (id)kCGImageSourceCreateThumbnailFromImageAlways: @YES,
        (id)kCGImageSourceCreateThumbnailWithTransform: @YES,
        (id)kCGImageSourceThumbnailMaxPixelSize: @(maxPixel),
        (id)kCGImageSourceShouldCacheImmediately: @YES
    };
    CGImageRef cgImage = CGImageSourceCreateThumbnailAtIndex(
        source, 0, (__bridge CFDictionaryRef)thumbOptions);
    CFRelease(source);
    if (!cgImage) return nil;
    UIImage *image = [UIImage imageWithCGImage:cgImage];
    CGImageRelease(cgImage);
    return image;
}
```

呼叫必須位於 serial background queue 並使用 `@autoreleasepool`：

```objective-c
dispatch_async(self.decodeQueue, ^{
    @autoreleasepool {
        UIImage *image = LCDownsampleImage(url, 1280);
        dispatch_async(dispatch_get_main_queue(), ^{ completion(image); });
    }
});
```

### 7.4 快取限制

```objective-c
self.imageCache = [[NSCache alloc] init];
self.imageCache.countLimit = 2;
self.imageCache.totalCostLimit = 12 * 1024 * 1024;
```

成本使用 `CGImageGetBytesPerRow(image.CGImage) * CGImageGetHeight(image.CGImage)`。收到 `didReceiveMemoryWarning` 時：

```objective-c
[self.imageCache removeAllObjects];
[self.imageManager cancelImageRequest:self.pendingRequestID];
self.nextImage = nil;
```

不要快取模糊背景的第二份 bitmap。若選擇完整顯示模式，背景直接用純黑或從同一張圖片取平均色；第一版不做即時 blur。

## 8. 低耗電計時與畫面更新

### 一般時鐘

- 不顯示秒數：計算下一分鐘邊界，只排一次 timer。
- 顯示秒數：每秒一次；離開前景立即 invalidate。
- 不使用 `CADisplayLink` 更新時間。

```objective-c
- (void)scheduleClockTick {
    [self.clockTimer invalidate];
    NSDate *now = [NSDate date];
    NSTimeInterval seconds = 60.0 - fmod(now.timeIntervalSince1970, 60.0);
    self.clockTimer = [NSTimer scheduledTimerWithTimeInterval:MAX(0.05, seconds)
                                                       target:self
                                                     selector:@selector(clockTick:)
                                                     userInfo:nil
                                                      repeats:NO];
}

- (void)clockTick:(NSTimer *)timer {
    [self updateClockLabels];
    [self scheduleClockTick];
}
```

### 相片切換

- 以一個 one-shot `NSTimer` 排到下一次換圖，不做每秒檢查。
- 低耗電模式直接替換圖片，不做 Ken Burns。
- 一般模式只做 0.20–0.30 秒 alpha crossfade。
- 動畫完成後立刻把舊 `UIImageView.image = nil`。

### 番茄鐘

只在 App 前景且番茄鐘運行時，每秒刷新一次文字。進背景後停止 timer，以 `endDate` 和 local notification 接手。

```objective-c
NSTimeInterval remaining = MAX(0, [self.state.endDate timeIntervalSinceNow]);
NSInteger totalSeconds = (NSInteger)ceil(remaining);
self.countdownLabel.text = [NSString stringWithFormat:@"%02ld:%02ld",
                            (long)(totalSeconds / 60),
                            (long)(totalSeconds % 60)];
```

這可避免 suspend、時鐘校正或畫面掉幀造成倒數漂移。

## 9. 橫直式排版與手勢

在 `viewDidLayoutSubviews` 依 `bounds.width < bounds.height` 判斷方向，使用 frame 計算，不建立大量互相衝突的 constraint。

NSUserDefaults key 分開：

```text
clock.position.portrait.x
clock.position.portrait.y
clock.scale.portrait
clock.position.landscape.x
clock.position.landscape.y
clock.scale.landscape
```

番茄鐘規則：

- 直式：倒數 label 在安全可視範圍完全置中。
- 橫式：倒數稍偏左上，但必須用 `sizeThatFits:` 後 clamp 在畫面內。
- 倒數以紅色呈現，使用 `minimumScaleFactor` 與 `adjustsFontSizeToFitWidth`。
- 右下資訊只保存一組小型 label，不複製整個時鐘 view tree。

照片長按：

```objective-c
UILongPressGestureRecognizer *press = [[UILongPressGestureRecognizer alloc]
    initWithTarget:self action:@selector(handlePhotoLongPress:)];
press.minimumPressDuration = 0.9;
press.allowableMovement = 12.0;
[self.view addGestureRecognizer:press];
```

只有 `state == UIGestureRecognizerStateBegan` 時顯示選單。選單三列建議高度 40、40、34 pt，字型與設定頁相同。

## 10. 設定頁實作

使用 `UITableViewController(style:UITableViewStyleGrouped)`。iOS 9 原生 table view 已具備捲動、cell reuse 與旋轉支援。

推薦 section：

1. 相片來源與內建精選。
2. 相片停留時間與顯示模式。
3. 時鐘字型、大小、日期與天氣。
4. 夜間／低耗電。
5. 番茄鐘。
6. 鬧鐘。

設定 draft：

```objective-c
@property (nonatomic, strong) NSMutableDictionary *draft;
```

進入時從 `NSUserDefaults` 複製；使用者操作只改 draft；按「套用」才一次寫入並 `synchronize`（iOS 9 可用，雖然平常不必頻繁呼叫）。取消或返回時丟棄 draft。

相片停留滑桿可使用連續 0–4，顯示與保存時映射：

```objective-c
static const NSInteger LCDurations[] = {15, 30, 40, 60, 120};
NSInteger index = (NSInteger)lround(slider.value);
index = MAX(0, MIN(4, index));
NSInteger seconds = LCDurations[index];
```

## 11. 番茄鐘與通知

在 iOS 9 註冊通知：

```objective-c
UIUserNotificationType types = UIUserNotificationTypeAlert |
                               UIUserNotificationTypeSound;
UIUserNotificationSettings *settings =
    [UIUserNotificationSettings settingsForTypes:types categories:nil];
[[UIApplication sharedApplication] registerUserNotificationSettings:settings];
```

排程番茄鐘結束：

```objective-c
UILocalNotification *note = [[UILocalNotification alloc] init];
note.fireDate = endDate;
note.timeZone = nil;
note.alertBody = @"專注階段結束";
note.soundName = nil; // 預設無聲；iPad mini 1 沒有震動馬達
note.userInfo = @{ @"kind": @"pomodoro" };
[[UIApplication sharedApplication] scheduleLocalNotification:note];
```

每次暫停、跳過、結束都先取消原 notification，再依新狀態排程。因 iOS 有 local notification 數量限制，只保留下一個番茄鐘事件與合理數量的鬧鐘事件，不預排大量循環。

App 在前景完成階段時：

- 顯示 3 秒全畫面提醒。
- 隨機選擇方向相符的 ANG/CRY 圖。
- 對白維持「專心一點！」／「怎麼這麼不專心？」。
- 若使用者開啟「柔和提示音」才播放短音，不預設發聲。

## 12. 鬧鐘限制與做法

普通 App 的可靠做法是 `UILocalNotification`。儲存 hour、minute、repeat、enabled，每次啟動／進前景重新計算下一個 `fireDate`。

重複每日：

```objective-c
note.fireDate = nextDate;
note.repeatInterval = NSCalendarUnitDay;
note.timeZone = [NSTimeZone localTimeZone];
note.alertBody = @"鬧鐘時間到了";
note.soundName = @"alarm.caf"; // 使用者若選無聲則設 nil
```

注意：

- iOS 不保證背景 App 自行顯示全螢幕控制介面。
- 使用者必須允許通知。
- 若裝置重開機、時區改變，iOS 仍管理已排程 local notification；App 進前景時再校正。
- 不要在第一版建立越獄 daemon。若普通通知實測不夠，再單獨設計 daemon，避免把整個時鐘變成高風險 system tweak。

## 13. 天氣

- 沿用 Open-Meteo HTTPS API，但必須先在 iOS 9 實機測 TLS 是否仍能連線。
- 使用 `NSURLSessionConfiguration.ephemeralSessionConfiguration`，timeout 15 秒。
- App 只在前景更新；一般 60 分鐘、低耗電 120 分鐘。
- JSON 放 background queue 解析；UI 更新回 main queue。
- 將最後成功結果與 timestamp 寫入 `NSUserDefaults`；超過 6 小時隱藏。
- 網路錯誤完全不影響時鐘或相片播放。
- 第一版可以先不做 Wi-Fi-only 檢測；iPad mini 若沒有行動網路，實際上自然只會走 Wi-Fi。若需要判斷，使用 SystemConfiguration Reachability，不引入第三方 library。

## 14. 搬移既有資產

從 Android 專案複製，不移動或刪除來源：

```bash
cd ~/Projects/LittleClock-iOS9
mkdir -p LittleClockLegacy/Resources/{DigitalLandscapes,FocusReminders,Fonts}

cp ~/Projects/LittleClock/app/src/main/assets/digital_landscapes/*.jpg \
   LittleClockLegacy/Resources/DigitalLandscapes/

cp ~/Projects/LittleClock/app/src/main/res/drawable-nodpi/focus_reminder_ang*.jpg \
   LittleClockLegacy/Resources/FocusReminders/
cp ~/Projects/LittleClock/app/src/main/res/drawable-nodpi/focus_reminder_cry*.jpg \
   LittleClockLegacy/Resources/FocusReminders/

cp ~/Projects/LittleClock/app/src/main/assets/fonts/font_iansui.ttf \
   LittleClockLegacy/Resources/Fonts/
cp ~/Projects/LittleClock/app/src/main/assets/fonts/font_orbitron.ttf \
   LittleClockLegacy/Resources/Fonts/
cp ~/Projects/LittleClock/app/src/main/assets/fonts/font_sans.ttf \
   LittleClockLegacy/Resources/Fonts/
cp ~/Projects/LittleClock/app/src/main/assets/fonts/LICENSES.txt \
   LittleClockLegacy/Resources/Fonts/
```

將檔案加入 Xcode target 的 Copy Bundle Resources。不要使用 folder reference 後又額外逐檔加入，否則會重複打包。

目前內建 100 張風景約 4.6 MB，適合保留；載入時仍必須 downsample。先不要把 Android 多密度 launcher icon 全部複製，改做一組 iPad AppIcon asset catalog。

## 15. Release 編譯與 IPA 腳本

建立 `Scripts/build-ipa.sh`：

```bash
#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT="$ROOT/LittleClockLegacy.xcodeproj"
SCHEME="LittleClockLegacy"
DERIVED="$ROOT/DerivedData"
RELEASE="$ROOT/release"

rm -rf "$DERIVED" "$RELEASE/Payload"
mkdir -p "$RELEASE/Payload"

xcodebuild \
  -project "$PROJECT" \
  -scheme "$SCHEME" \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath "$DERIVED" \
  ARCHS=armv7 \
  ONLY_ACTIVE_ARCH=NO \
  IPHONEOS_DEPLOYMENT_TARGET=9.0 \
  ENABLE_BITCODE=NO \
  CODE_SIGNING_ALLOWED=NO \
  clean build

APP="$DERIVED/Build/Products/Release-iphoneos/LittleClockLegacy.app"
test -d "$APP"

/usr/bin/codesign --force --sign - --timestamp=none "$APP"
cp -R "$APP" "$RELEASE/Payload/"

cd "$RELEASE"
/usr/bin/ditto -c -k --sequesterRsrc --keepParent Payload LittleClockLegacy-iOS9-armv7.ipa

echo "Built: $RELEASE/LittleClockLegacy-iOS9-armv7.ipa"
```

授權並執行：

```bash
chmod +x Scripts/build-ipa.sh
./Scripts/build-ipa.sh
```

驗證：

```bash
APP=DerivedData/Build/Products/Release-iphoneos/LittleClockLegacy.app
BIN="$APP/LittleClockLegacy"

file "$BIN"
lipo -info "$BIN"
otool -l "$BIN" | grep -A4 LC_VERSION_MIN_IPHONEOS
plutil -p "$APP/Info.plist" | grep -E "CFBundleIdentifier|MinimumOSVersion"
codesign -dv --verbose=4 "$APP" 2>&1 | head -30
unzip -l release/LittleClockLegacy-iOS9-armv7.ipa | head -30
```

必須看到 `armv7` 與最低系統 9.0。若看到只有 `arm64`，不要安裝，回去修正 target architecture。

若 `codesign -` 產物在 AppSync 上仍被拒絕，可改用 `ldid` 做 ad-hoc/fakesign；但先試 macOS 內建 `codesign`，避免增加工具依賴。

## 16. 越獄 iPad 的永久安裝流程

只安裝自己編譯的 IPA。iPad 上的套件來源應使用 AppSync Unified 官方維護來源；AppSync 的用途是讓越獄裝置安裝 ad-hoc、fakesigned 或 unsigned IPA，不應用於侵權軟體。

官方專案與說明：

- [AppSync Unified 專案／官方來源說明](https://github.com/akemin-dayo/AppSync)
- [OWASP 對 AppSync Unified 的工具說明](https://mas.owasp.org/MASTG/tools/ios/MASTG-TOOL-0127/)

iPad 上：

1. 在 Cydia 加入官方來源 `https://cydia.akemi.ai/`（若舊 Cydia 的 TLS 失敗，可使用專案目前正式公布的替代來源，不要裝不明重包）。
2. 安裝 `AppSync Unified`。
3. 安裝 `Filza File Manager`；若來源提供 `appinst`，也一併安裝。
4. 若使用 SSH，安裝 OpenSSH，立刻修改 `root` 與 `mobile` 預設密碼。

在 iPad Terminal：

```sh
passwd
passwd mobile
```

取得 iPad IP（設定 → Wi-Fi → 目前網路），Mac 傳送：

```bash
scp ~/Projects/LittleClock-iOS9/release/LittleClockLegacy-iOS9-armv7.ipa \
    root@IPAD_IP:/var/mobile/Media/
```

若有 `appinst`：

```bash
ssh root@IPAD_IP
appinst /var/mobile/Media/LittleClockLegacy-iOS9-armv7.ipa
uicache
exit
```

若沒有 `appinst`，在 Filza 開啟 `/var/mobile/Media/`，點 IPA → Install，完成後 respring。

注意：iOS 9.3.5 常見越獄是半持久／半不完美越獄。IPA 與 AppSync 套件會保留，但重開機後可能要重新啟用越獄環境，AppSync 的簽章繞過才會恢復。這與 Apple 免費憑證七天到期是不同問題。

## 17. 實機效能驗收標準

每個 milestone 都在 iPad mini 1 測至少 30 分鐘；Release build 才算數。

| 項目 | 目標 |
|---|---:|
| 一般時鐘穩定 CPU | 平均低於 3% |
| 低耗電模式穩定 CPU | 平均低於 2% |
| 穩定記憶體 footprint | 35–80 MB |
| 換大圖瞬間峰值 | 儘量低於 120 MB |
| 同時解碼工作 | 1 個 serial queue |
| 圖片 bitmap 快取 | 最多 2 張／12 MB |
| View controller 疊加 | 長時間固定，不持續增加 |
| 時鐘更新 | 分鐘模式每分鐘一次 |
| 換圖卡住主執行緒 | 不可超過 100 ms |

Instruments：Xcode → Product → Profile：

1. **Allocations**：連續換圖 100 次，Live Bytes 應回到穩定區間，不可階梯式上升。
2. **Leaks**：不得有持續性 leak。
3. **Time Profiler**：待機時主執行緒大部分時間應休眠；不可看到每幀 timer 或圖片反覆解碼。
4. **Energy Log**：相同亮度下比較一般模式與低耗電模式。

命令列先確認 IPA 大小：

```bash
du -h release/LittleClockLegacy-iOS9-armv7.ipa
```

IPA 大小不是 RAM 用量；真正需要盯的是解壓後 bitmap 與是否同時保留多張圖片。

## 18. Memory warning 與生命週期

`LCClockViewController` 必須實作：

```objective-c
- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    [self.imageCache removeAllObjects];
    self.preloadedImage = nil;
    if (self.pendingRequestID != PHInvalidImageRequestID) {
        [self.imageManager cancelImageRequest:self.pendingRequestID];
        self.pendingRequestID = PHInvalidImageRequestID;
    }
}

- (void)applicationDidEnterBackground {
    [self.clockTimer invalidate];
    [self.photoTimer invalidate];
    [self.pomodoroTimer invalidate];
    [self.imageCache removeAllObjects];
}
```

回前景時以目前時間重建 timer；不要試圖補跑背景期間錯過的每一次 tick 或換圖。

若使用 `UIApplication.sharedApplication.idleTimerDisabled = YES` 保持螢幕常亮，必須由使用者設定控制；夜間模式或離開時鐘主頁時恢復 `NO`。長時間展示的最大耗電來源仍是螢幕亮度，不是計時器。

## 19. AI agent 每輪工作格式

把以下提示詞連同本文件交給 Mac 上 agent：

```text
你正在把 LittleClock Android 4.0.7 重新實作成 iPad mini 1 / iOS 9.3.x 專用 App。
請完整閱讀 docs/IPAD_MINI1_IOS9_PORTING_GUIDE.md，再檢查目前 Git 狀態。

硬性限制：
- Xcode 13.4.1、Objective-C、UIKit、deployment target 9.0、Release armv7。
- 不可使用 Swift、SwiftUI、Combine、CocoaPods、第三方圖片載入框架或 iOS 10+ 專用 API。
- 不可修改 Android 專案；不可刪除或覆寫使用者資產。
- 相片必須 downsample 到長邊最多 1280；serial background decode；最多快取 2 張/12 MB。
- 一般時鐘對齊分鐘更新；只有顯示秒數或番茄鐘時每秒更新。
- 先讓第一張照片顯示，再在背景準備剩餘目錄；不得等待一萬張全部處理完才播放。
- 直式與橫式排版位置獨立保存。
- 每次只完成目前 milestone，先 build、檢查 armv7，再回報檔案與實機測試清單。

開始前請輸出：
1. 目前 milestone。
2. 預計修改檔案。
3. 記憶體與 CPU 風險。

完成後必須執行：
./Scripts/build-ipa.sh
file DerivedData/Build/Products/Release-iphoneos/LittleClockLegacy.app/LittleClockLegacy
lipo -info DerivedData/Build/Products/Release-iphoneos/LittleClockLegacy.app/LittleClockLegacy

若建置未成功或不是 armv7，不得宣稱完成。
```

## 20. 最終完成條件

- iPad mini 1 的 iOS 9.3.x 能冷啟動與旋轉，不閃退。
- 內建精選立即播放；一萬張 Photos 相簿不必全部掃完才顯示第一張。
- 時鐘模式的位置與縮放在橫直式分別記憶。
- 番茄鐘倒數不因背景、暫停或系統時間更新而漂移。
- 相片長按 0.9 秒才出現緊湊選單。
- 連續換圖 100 次記憶體沒有階梯式成長。
- 低耗電模式沒有持續動畫，待機 CPU 符合目標。
- IPA 經 `file`、`lipo`、`otool` 驗證為 armv7、MinimumOSVersion 9.0。
- AppSync 安裝後重啟 App、重新進入越獄環境均能開啟。
- Android 版本與原始資產完全保留。

先達到穩定、低耗電、可長時間展示，再逐步補齊天氣與鬧鐘。iPad mini 1 的價值不在追求新框架，而在用最少的運算持續完成一件漂亮、可靠的事。
