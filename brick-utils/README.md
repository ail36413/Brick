# brick-utils

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)

Android 通用工具扩展库，为日常开发提供高频 Kotlin 扩展函数，减少样板代码。

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-utils:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 模块总览

| 文件 | 功能分类 | 核心 API |
|------|---------|----------|
| `ContextExt` | 尺寸 & 屏幕 | `Int.dp`, `Float.dpF`, `screenWidth`, `statusBarHeight`, `navigationBarHeight` |
| `ViewExt` | 视图操作 | `onClick {}`, `setVisible()`, `postDelayed()` |
| `StringExt` | 字符串校验 & 处理 | `isPhoneNumber()`, `isEmail()`, `isUrl()`, `maskPhone()`, `md5()`, `sha256()` |
| `DateExt` | 日期时间 | `Long.formatDate()`, `String.parseDate()`, `Long.toFriendlyTime()`, `isYesterday()`, `isSameDay()` |
| `FileExt` | 文件操作 | `File.friendlySize()`, `File.md5()`, `File.sha256()`, `File.totalSize()`, `File.safeDeleteRecursively()` |
| `SpDelegate` | SP 属性委托 | `string()`, `int()`, `boolean()`, `long()`, `stringSet()` |
| `BrickLog` | 日志 | `BrickLog.d()`, `BrickLog.e()`, `init(isDebug, minLevel)`, Lambda 延迟拼接 |
| `SystemExt` | 系统工具 | `copyToClipboard()`, `showKeyboard()`, `toast()`, `shareText()`, `openAppSettings()`, `appVersionName()` |
| `NetworkExt` | 网络 | `isNetworkAvailable()`, `isWifiConnected()`, `isMobileDataConnected()`, `getNetworkTypeName()`, `observeNetworkState()` |
| `ActivityExt` | Activity 跳转 | `startActivity<T>()`, `extraOrNull<T>()`, `finishWithResult()` |
| `CollectionExt` | 集合操作 | `ifNotEmpty {}`, `safeJoinToString()` |
| `DeviceExt` | 设备信息 | `deviceBrand`, `deviceModel`, `osVersion`, `sdkVersion`, `deviceSummary()` |
| `EncodeExt` | 编解码 | `encodeBase64()`, `decodeBase64String()`, `ByteArray.toHexString()`, `String.hexToByteArray()` |

## 使用示例

### 尺寸转换
```kotlin
val pxValue = 16.dp           // 16dp → px (Int)
val pxFloat = 16.5.dpF        // 16.5dp → px (Float)
val width = context.screenWidth
val barH = context.statusBarHeight
val navH = context.navigationBarHeight
```

### 防抖点击
```kotlin
binding.btnSubmit.onClick(interval = 500) {
    submitForm()
}
// View 可见性控制
binding.tvEmpty.setVisible(list.isEmpty())
binding.btnSubmit.visible()   // View.VISIBLE
binding.btnSubmit.gone()      // View.GONE
```

### SP 属性委托
```kotlin
object AppPrefs : SpDelegate("app_prefs") {
    var token by string("token", "")
    var userId by int("user_id", 0)
    var isFirstLaunch by boolean("first_launch", true)
}

// Application.onCreate
AppPrefs.init(applicationContext)

// 读写
AppPrefs.token = "abc123"
val t = AppPrefs.token
```

### 字符串校验 & 脱敏
```kotlin
"13812345678".isPhoneNumber()  // true
"test@mail.com".isEmail()      // true
"https://example.com".isUrl()  // true
"13812345678".maskPhone()      // "138****5678"
"hello".md5()                  // "5d41402abc..."
```

### 网络状态

> 所有网络相关函数均已标记 `@RequiresPermission(ACCESS_NETWORK_STATE)`，请确保在 `AndroidManifest.xml` 中声明权限。

```kotlin
if (context.isNetworkAvailable()) { /* ... */ }
context.getNetworkTypeName()  // "WiFi" / "Mobile" / "Ethernet" / "None"
context.isMobileDataConnected()  // true / false

// Flow 监听（lifecycle-safe）
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        context.observeNetworkState().collect { isAvailable -> updateUI(isAvailable) }
    }
}
```

### 日志
```kotlin
BrickLog.init(isDebug = BuildConfig.DEBUG, prefix = "MyApp")

// 生产环境仅输出 WARN 及以上
BrickLog.init(isDebug = true, prefix = "MyApp", minLevel = BrickLog.Level.WARN)

BrickLog.d("tag", "message")
BrickLog.e("tag", "error", throwable)

// Lambda 延迟拼接，避免关闭日志时的字符串拼接开销
BrickLog.d("tag") { "耗时操作结果: ${heavyComputation()}" }
```

### 设备信息
```kotlin
val brand = deviceBrand        // "Xiaomi"
val model = deviceModel        // "Mi 14"
val version = osVersion        // "14"
val sdk = sdkVersion           // 34
val info = deviceSummary()     // "Xiaomi Mi 14 | Android 14 (SDK 34)"
```

### 编解码
```kotlin
// Base64
"hello".encodeBase64()            // "aGVsbG8="
"aGVsbG8=".decodeBase64String()   // "hello"

// Hex
byteArrayOf(0x0A, 0xFF.toByte()).toHexString()  // "0aff"
"0aff".hexToByteArray()                          // [10, -1]
```

### 系统工具
```kotlin
context.shareText("分享内容", "分享到")
context.openAppSettings()  // 跳转当前应用设置页
context.toast("操作成功")
context.copyToClipboard("文本")
context.appVersionName()   // "1.0.0"
context.appVersionCode()   // 1
```

### 文件操作
```kotlin
File("photo.jpg").friendlySize()     // "2.3 MB"
File("data.bin").sha256()             // "小写 64 位 hex"
context.cacheDir.totalSize()          // 目录总字节数
File("/tmp/old").safeDeleteRecursively()
inputStream.writeToFile(File("out.bin"))
```

### 日期时间
```kotlin
System.currentTimeMillis().formatDate()              // "2026-04-03 10:30:00"
System.currentTimeMillis().formatDate("yyyy/MM/dd")  // "2026/04/03"
"2026-04-03".parseDate("yyyy-MM-dd")                 // Date?
(now - 30_000L).toFriendlyTime()                     // "刚刚"
now.isToday()                                        // true
yesterday.isYesterday()                              // true
time1.isSameDay(time2)                               // Boolean
```

### Activity 工具
```kotlin
context.startActivity<DetailActivity>()
context.startActivity<DetailActivity> {
    putExtra("id", 123)
}
val userId = extraOrNull<String>("user_id")
finishWithResult(Activity.RESULT_OK)
```

### 集合工具
```kotlin
list?.ifNotEmpty { items -> adapter.submitList(items.toList()) }
tags.safeJoinToString(", ")          // null 安全
```

## 线程安全

- `BrickLog.enabled` / `prefix` 使用 `@Volatile` 保证多线程可见性
- `SpDelegate` 每次读写前执行 `ensureInitialized()` 检查
- `StringExt` 中正则表达式编译为常量，避免重复创建

## 最佳实践

1. **SpDelegate 初始化**：务必在 `Application.onCreate()` 中调用 `init(context)`，否则访问属性会抛出 `IllegalStateException`
2. **防抖点击**：默认间隔 500ms，对于高频操作（如聊天发送）可适当降低；对于危险操作（如支付）建议增加到 1000ms+
3. **网络状态监听**：`observeNetworkState()` 返回冷 Flow，搭配 `repeatOnLifecycle(STARTED)` 使用以确保生命周期安全
4. **MD5/SHA-256**：`StringExt` 中的哈希函数适合小字符串；大文件请使用 `FileExt.md5()` / `FileExt.sha256()` 避免内存溢出
5. **日期格式化**：`formatDate()` 使用 `ThreadLocal` 缓存 `SimpleDateFormat`，线程安全且高频调用无性能损耗；默认使用系统时区，跨时区场景请显式传入 `TimeZone`
6. **日志级别**：生产环境建议设置 `minLevel = BrickLog.Level.WARN`，减少不必要的日志输出；使用 Lambda 重载避免字符串拼接开销
7. **文件 SHA-256**：需要文件完整性校验时优先使用 `File.sha256()` 而非 `File.md5()`（MD5 已不安全）
8. **缓存管理**：使用 `File.totalSize()` 计算缓存目录大小，配合 `friendlySize()` 展示给用户

## 常见问题

详见 [FAQ.md](../FAQ.md#brick-utils)。

## License

```
Copyright 2024 ail36413

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
