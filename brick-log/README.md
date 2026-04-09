# brick-log

基于 [Timber](https://github.com/JakeWharton/timber) 的增强日志模块，提供文件日志、崩溃收集、JSON 格式化等企业级能力。

## 特性

- **基于 Timber**：兼容 Timber 生态，支持自定义 Tree 扩展
- **自动 Tag**：自动获取调用者类名作为 Tag，无需手动传入
- **调用定位**：日志包含方法名、文件名和行号，一键跳转
- **文件日志**：按日期轮转，大小限制，自动清理旧文件
- **崩溃收集**：捕获 ERROR 级别日志，可对接 Firebase/Bugly
- **JSON 格式化**：自动美化输出 JSON 数据
- **Lambda 延迟拼接**：关闭日志时零开销

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-log:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 基本用法

### 1. 初始化

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BrickLogger.init {
            debug = BuildConfig.DEBUG
        }
    }
}
```

### 2. 输出日志

```kotlin
BrickLogger.d("请求成功")
BrickLogger.i("用户登录: userId=$userId")
BrickLogger.w("缓存即将过期")
BrickLogger.e(exception, "请求失败: $url")
```

### 3. Lambda 延迟拼接

```kotlin
// 当日志未启用时，Lambda 不会执行，避免字符串拼接开销
BrickLogger.d { "响应数据: ${response.body?.string()}" }
BrickLogger.e { "错误详情: ${buildErrorReport()}" }
```

## 文件日志

开启后自动将 INFO 及以上级别的日志写入本地文件：

```kotlin
BrickLogger.init {
    debug = BuildConfig.DEBUG
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    maxFileSize = 5L * 1024 * 1024  // 单文件最大 5MB
    maxFileCount = 10                // 最多保留 10 个文件
}
```

日志文件特性：
- 按日期命名：`log_2024-01-15.txt`
- 超出大小自动轮转：`log_2024-01-15_1705312000000.txt`
- 超出数量自动删除最旧文件
- 异步写入，不阻塞主线程

日志格式：
```
2024-01-15 14:30:25.123 I/UserViewModel: 用户登录成功
2024-01-15 14:30:26.456 E/NetworkExecutor: 请求超时
java.net.SocketTimeoutException: connect timed out
    at java.net.Socket.connect(Socket.java:123)
    ...
```

## 崩溃日志收集

捕获 ERROR 级别的日志和异常，对接第三方平台：

```kotlin
BrickLogger.init {
    crashLog = true
    crashHandler = { tag, throwable, message ->
        // 上报到 Firebase Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("tag", tag ?: "unknown")
            message?.let { log(it) }
            throwable?.let { recordException(it) }
        }
    }
}
```

## JSON 格式化

```kotlin
val json = """{"name":"张三","age":25,"address":{"city":"北京"}}"""
BrickLogger.json(json)
```

输出：
```
┌────────────────────────────────────
│ {
│   "name": "张三",
│   "age": 25,
│   "address": {
│     "city": "北京"
│   }
│ }
└────────────────────────────────────
```

带 Tag 输出：
```kotlin
BrickLogger.json(responseBody, tag = "API Response")
```

## 自定义 Tree

```kotlin
// 自定义 Tree（如上报到自研日志平台）
class AnalyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        AnalyticsSDK.trackLog(priority, tag, message)
    }
}

// 注册
BrickLogger.init {
    debug = BuildConfig.DEBUG
    addTree(AnalyticsTree())
}
```

## 带 Tag 日志

```kotlin
BrickLogger.tag("Network").d("请求开始: GET /users")
BrickLogger.tag("Database").i("查询完成: 耗时 ${cost}ms")
```

## 与 BrickLog 的区别

| 特性 | BrickLog (brick-utils) | BrickLogger (brick-log) |
|------|----------------------|------------------------|
| 依赖 | 无额外依赖 | Timber |
| 文件日志 | ❌ | ✅ |
| 崩溃收集 | ❌ | ✅ |
| JSON 格式化 | ❌ | ✅ |
| 自动 Tag | 手动传入 | 自动获取类名 |
| 调用定位 | ❌ | ✅ 方法名+文件+行号 |
| 自定义扩展 | ❌ | ✅ Timber Tree |

> 简单项目使用 `BrickLog`（零依赖），复杂项目推荐 `BrickLogger`（功能完整）。

## 线程模型

- **控制台日志**：Timber 在调用线程输出，线程安全
- **文件日志**：通过 `SingleThreadExecutor` 异步写入，不阻塞调用线程
- **崩溃回调**：在调用 `BrickLogger.e()` 的线程上同步触发

## FAQ

### Q: 文件日志会影响性能吗？

不会。文件写入在独立线程池执行，调用线程仅提交任务即返回。

### Q: 如何在 Release 包中只保留文件日志？

```kotlin
BrickLogger.init {
    debug = false      // 关闭控制台输出
    fileLog = true      // 保留文件日志
    fileDir = "..."
}
```

### Q: 日志文件存放在哪里？

默认路径为 `fileDir` 指定目录（建议使用 `cacheDir/logs`），文件按日期命名，超出数量自动清理。
