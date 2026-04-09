# brick-net

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)

Android 网络基础库，基于 OkHttp + Retrofit + Hilt 封装，支持 HTTP 与 WebSocket。

## 功能特性

### HTTP
- 统一结果包装 `NetworkResult<T>`（Success / TechnicalFailure / BusinessFailure）
- 开箱即用：只需配置 `baseUrl` 即可发起请求
- 文件下载：进度回调、SHA-256 Hash 校验
- 文件上传：单文件/多文件/Multipart，进度回调
- Token 鉴权：自动刷新、401 拦截、并发安全
- 重试机制：指数退避、可配置策略
- 动态配置：运行时切换 BaseUrl（`@BaseUrl`）、按接口超时（`@Timeout`）
- 响应映射：自定义 code/msg/data 字段名，兼容不同后端
- 日志格式化：敏感信息脱敏（Header + Body 字段级）、JSON 美化
- 请求监控：`NetEvent` 事件追踪（耗时、成功率），支持同步/异步分发
- 轮询工具：`pollingFlow()` 周期请求
- 请求去重：`RequestDedup` 相同请求合并，避免重复发送
- 请求节流：`RequestThrottle` 限制请求间隔，防止频繁触发
- 连接池优化：可配置空闲连接数和存活时间，适配不同流量场景
- SSL 证书固定：防止中间人攻击，支持多域名 pin 配置
- 网络状态监听：实时监控网络连接/断开和类型变化（Wi-Fi / 蜂窝 / 以太网）

### WebSocket
- 多连接管理：同时维护多个 WebSocket 连接
- 心跳检测：可配置间隔和超时
- 断线重连：指数退避 + 随机抖动
- 离线消息队列：断开期间消息自动缓存，重连后补发

## 引入方式

### 1. 添加 JitPack 仓库

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. 添加依赖

```kotlin
// app/build.gradle.kts
dependencies {
    val brickVersion = "1.0.0"
    implementation("com.github.ail36413.Brick:brick-net:$brickVersion")
}
```

### 3. Hilt 前置要求

brick-net 依赖 Hilt，项目需要集成 Hilt：

```kotlin
// 根 build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}

// app/build.gradle.kts
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
}
```

Application 类添加 `@HiltAndroidApp`：

```kotlin
@HiltAndroidApp
class MyApp : Application()
```

## 快速开始（5 分钟）

### 最小配置：只需一个 baseUrl

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig {
        return NetworkConfig(baseUrl = "https://api.example.com/")
    }
}
```

> `baseUrl` 必须以 `http://` 或 `https://` 开头，以 `/` 结尾。

### 定义 API 接口

```kotlin
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): GlobalResponse<User>
}
```

### 发起请求

```kotlin
@AndroidEntryPoint
class UserActivity : AppCompatActivity() {

    @Inject lateinit var executor: NetworkExecutor
    @Inject lateinit var retrofit: Retrofit

    private val api by lazy { retrofit.create(UserApi::class.java) }

    private fun loadUser() {
        lifecycleScope.launch {
            val result = executor.executeRequest { api.getUser(1) }

            result
                .onSuccess { user -> showUser(user) }
                .onBusinessFailure { code, msg -> showToast("业务错误: $msg") }
                .onTechnicalFailure { ex -> showToast("网络错误: ${ex.message}") }
        }
    }
}
```

## 进阶配置

### 完整 NetworkConfig

```kotlin
NetworkConfig(
    baseUrl = "https://api.example.com/",
    connectTimeout = 15L,              // 连接超时（秒）
    readTimeout = 15L,                 // 读取超时（秒）
    writeTimeout = 15L,                // 写入超时（秒）
    defaultSuccessCode = 0,            // 全局业务成功码
    isLogEnabled = true,               // 开启网络日志
    networkLogLevel = NetworkLogLevel.BODY, // 日志级别
    extraHeaders = mapOf(              // 公共请求头
        "X-App-Version" to "1.0.0",
        "X-Device-Id" to deviceId
    ),
    cacheDir = cacheDir,               // OkHttp 缓存目录
    cacheSize = 10_000_000L,           // 缓存大小（10MB）
    enableRetryInterceptor = true,     // 开启重试
    retryMaxAttempts = 2,              // 最大重试次数
    retryInitialBackoffMs = 300L,      // 初始退避延迟
    maxIdleConnections = 10,           // 连接池最大空闲连接数
    keepAliveDurationSeconds = 600,    // 连接池存活时间（10 分钟）
    certificatePins = listOf(          // SSL 证书固定
        CertificatePin(
            pattern = "api.example.com",
            pins = listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
        )
    ),
    responseFieldMapping = ResponseFieldMapping(
        codeKey = "status",            // 后端 code 字段名
        msgKey = "message",            // 后端 msg 字段名
        dataKey = "result"             // 后端 data 字段名
    ),
    sensitiveHeaders = NetworkConfig.DEFAULT_SENSITIVE_HEADERS + setOf(
        "x-custom-secret"              // 追加自定义脱敏 Header
    ),
    sensitiveBodyFields = NetworkConfig.DEFAULT_SENSITIVE_BODY_FIELDS + setOf(
        "id_card", "phone"             // 追加自定义脱敏 Body 字段
    )
)
```

### Token 鉴权

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideTokenProvider(): TokenProvider {
        return InMemoryTokenProvider().apply {
            updateToken("your-access-token")
        }
    }

    @Provides
    @Singleton
    fun provideUnauthorizedHandler(): UnauthorizedHandler {
        return object : UnauthorizedHandler {
            override fun onUnauthorized() {
                // Token 刷新失败，跳转登录页
            }
        }
    }
}
```

> 提供 `TokenProvider` 后，库会自动处理 401 → 刷新 Token → 重试请求。

### 自定义拦截器

```kotlin
@Provides
@Singleton
@AppInterceptor
fun provideCustomInterceptors(): Map<Int, Interceptor> = mapOf(
    0 to MyAuthInterceptor(),    // 数字越小优先级越高
    1 to MyTimingInterceptor()
)
```

### 动态 BaseUrl（按接口覆盖）

```kotlin
interface FileApi {
    @BaseUrl("https://cdn.example.com/")
    @GET("files/{name}")
    suspend fun downloadFile(@Path("name") name: String): ResponseBody
}
```

### 按接口超时

```kotlin
interface SlowApi {
    @Timeout(read = 60, write = 60)
    @POST("heavy-task")
    suspend fun heavyTask(@Body body: RequestBody): GlobalResponse<Result>
}
```

### 响应字段映射（ResponseFieldMapping）

不同后端的 JSON 结构可能各不相同，`ResponseFieldMapping` 允许全局配置字段映射规则，无需逐接口适配：

```kotlin
// 后端返回 { "status": 200, "message": "ok", "result": {...} }
NetworkConfig(
    baseUrl = "https://api.example.com/",
    responseFieldMapping = ResponseFieldMapping(
        codeKey = "status",      // code 字段名
        msgKey = "message",      // msg 字段名
        dataKey = "result"       // data 字段名
    )
)
```

**备用字段名**：后端可能在不同接口使用不同字段名，通过 `codeFallbackKeys` 等参数设置备选字段：

```kotlin
ResponseFieldMapping(
    codeKey = "code",
    codeFallbackKeys = listOf("status", "errCode"),  // 依次尝试
    msgFallbackKeys = listOf("message", "error"),
    dataFallbackKeys = listOf("result", "body")
)
```

**自定义 code 转换**：当后端的成功标识不是整数（如布尔值或字符串）时，使用 `codeValueConverter`：

```kotlin
ResponseFieldMapping(
    codeKey = "success",
    codeValueConverter = { raw, mapping ->
        // raw 是 JSON 中 "success" 字段的原始值
        if (raw == true) mapping.successCode else mapping.failureCode
    }
)
```

> 映射规则全局生效，通过 `executeRequest` 发起的请求会自动应用。

## 连接池优化

默认使用 OkHttp 标准连接池（5 空闲 / 5 分钟存活），可按业务场景调整：

```kotlin
NetworkConfig(
    baseUrl = "https://api.example.com/",
    maxIdleConnections = 10,           // 高并发：增大空闲连接数
    keepAliveDurationSeconds = 600     // 长连接：延长存活时间
)
```

> 低频场景可减小 `maxIdleConnections` 以节省系统资源。

## SSL 证书固定

通过 Certificate Pinning 将域名绑定到特定证书公钥哈希，防止中间人攻击：

```kotlin
NetworkConfig(
    baseUrl = "https://api.example.com/",
    certificatePins = listOf(
        CertificatePin(
            pattern = "api.example.com",
            pins = listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // 当前证书
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="   // 备用证书
            )
        ),
        CertificatePin(
            pattern = "*.cdn.example.com",
            pins = listOf("sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=")
        )
    )
)
```

> **注意**：证书固定需要在证书轮换前更新 pin 值，建议同时配置当前和备用证书的 pin。
> 获取 pin 值：`openssl s_client -connect api.example.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64`

## 网络状态监听

`NetworkMonitor` 提供实时的网络连接状态和类型监控：

```kotlin
@Inject lateinit var networkMonitor: NetworkMonitor

// 方式一：同步判断是否在线
if (networkMonitor.isOnline()) {
    // 发起请求
}

// 方式二：StateFlow 实时观察连接状态
lifecycleScope.launch {
    networkMonitor.isConnected.collect { connected ->
        statusText.text = if (connected) "在线" else "离线"
    }
}

// 方式三：观察网络类型变化
lifecycleScope.launch {
    networkMonitor.networkType.collect { type ->
        when (type) {
            NetworkType.WIFI -> showWifiIcon()
            NetworkType.CELLULAR -> showCellularIcon()
            NetworkType.ETHERNET -> showEthernetIcon()
            NetworkType.NONE -> showOfflineIcon()
            NetworkType.OTHER -> showDefaultIcon()
        }
    }
}

// 方式四：事件流（自动管理回调生命周期）
lifecycleScope.launch {
    networkMonitor.observeNetworkEvents().collect { type ->
        Log.d("Network", "网络变化: $type")
    }
}
```

> `NetworkMonitor` 由 Hilt 自动注入，内部声明 `ACCESS_NETWORK_STATE` 权限，无需额外配置。
> 如需手动释放资源，可调用 `networkMonitor.destroy()` 注销系统级回调。

## 文件下载

```kotlin
val progressFlow = MutableSharedFlow<ProgressInfo>(replay = 1)

// 监听进度
lifecycleScope.launch {
    progressFlow.collect { info ->
        progressBar.progress = info.progress
        if (info.isDone) showToast("下载完成")
    }
}

// 发起下载
val result = executor.downloadFile(
    targetFile = File(cacheDir, "update.apk"),
    progressFlow = progressFlow,
    expectedHash = "sha256-hash-value",       // 可选：校验文件完整性
    hashStrategy = HashVerificationStrategy.DELETE_ON_MISMATCH
) { api.downloadFile("update.apk") }
```

## 文件上传

```kotlin
val progressFlow = MutableSharedFlow<ProgressInfo>(replay = 1)

val result = executor.uploadFile(
    file = selectedFile,
    partName = "file",
    progressFlow = progressFlow
) { part -> api.uploadFile(part) }
```

## WebSocket

### 基本使用

```kotlin
@Inject lateinit var wsManager: IWebSocketManager

// 连接
wsManager.connectDefault(
    url = "wss://echo.websocket.org",
    config = WebSocketManager.Config(
        heartbeatIntervalMs = 30_000L,
        reconnectEnabled = true,
        enableMessageReplay = true
    ),
    listener = object : WebSocketManager.WebSocketListener {
        override fun onOpen(connectionId: String) { /* 连接成功 */ }
        override fun onMessage(connectionId: String, text: String) { /* 收到消息 */ }
        override fun onClosed(connectionId: String, code: Int, reason: String) { /* 已关闭 */ }
        override fun onFailure(connectionId: String, t: Throwable) { /* 连接失败 */ }
        // ... 其他回调
    }
)

// 发送消息
wsManager.sendText("Hello WebSocket!")

// 断开连接
wsManager.disconnectDefault()
```

### 多连接管理

```kotlin
wsManager.connect("chat", "wss://chat.example.com", chatConfig, chatListener)
wsManager.connect("push", "wss://push.example.com", pushConfig, pushListener)

wsManager.sendMessage("chat", "你好")
wsManager.disconnect("push")
```

## NetworkResult 处理

```kotlin
val result: NetworkResult<User> = executor.executeRequest { api.getUser(1) }

// 方式一：链式回调
result
    .onSuccess { user -> }
    .onBusinessFailure { code, msg -> }
    .onTechnicalFailure { ex -> }

// 方式二：fold 统一处理
val text = result.fold(
    onSuccess = { "用户: ${it?.name}" },
    onTechnicalFailure = { "网络错误" },
    onBusinessFailure = { code, msg -> "业务错误: $msg" }
)

// 方式三：快捷取值
val user = result.getOrNull()
val userOrDefault = result.getOrDefault(defaultUser)
val userOrThrow = result.getOrThrow()

// 方式四：失败恢复
val recovered = result.recover { defaultUser }           // 失败 → 成功（默认值）
val retried = result.recoverWith { fetchFromCache() }    // 失败 → 尝试降级请求
```

## 错误码速查

### 业务码（NetCode.Biz）

| 常量 | 值 | 说明 |
|------|-----|------|
| SUCCESS | 0 | 请求成功 |
| UNAUTHORIZED | 401 | 未授权（Token 失效） |
| FORBIDDEN | 403 | 禁止访问 |
| NOT_FOUND | 404 | 资源不存在 |

### 技术码（NetCode.Tech）

| 常量 | 值 | 说明 |
|------|-----|------|
| TIMEOUT | -1 | 连接/读取/写入超时 |
| NO_NETWORK | -2 | 无网络连接 |
| SSL_ERROR | -3 | SSL 握手失败 |
| REQUEST_CANCELED | -999 | 请求已取消 |
| UNKNOWN | -1000 | 未知错误 |
| PARSE_ERROR | -1001 | JSON 解析失败 |

## 异常体系

```
BaseNetException
├── RequestException      ← 网络层：超时、DNS、连接失败
├── ServerException       ← HTTP 层：4xx、5xx
├── ParseException        ← 解析层：JSON 错误
├── BusinessFailureException ← 业务码不匹配
└── UnknownNetException   ← 未知异常
```

## 自定义错误文案

```kotlin
// Application 启动时设置
NetErrorMessage.provider = { code, defaultMsg ->
    when (code) {
        NetCode.Tech.TIMEOUT -> "请求超时，请稍后重试"
        NetCode.Tech.NO_NETWORK -> "网络不可用，请检查网络设置"
        else -> defaultMsg
    }
}
```

## 请求监控

```kotlin
// Application 启动时设置
NetTracker.delegate = object : INetTracker {
    override fun onEvent(event: NetEvent) {
        if (event.stage == NetEventStage.END) {
            Log.d("NetTracker", "${event.tag} 耗时 ${event.durationMs}ms 结果: ${event.resultType}")
        }
    }
}

// 异步分发（适用于 onEvent 有耗时操作如写数据库、上报埋点）
NetTracker.trackAsync(event) // 不阻塞调用线程
```

## 请求去重与节流

### 请求去重（RequestDedup）

当多个页面/组件并发请求同一数据时，`RequestDedup` 将相同 key 的请求合并为一次，所有等待者共享结果：

```kotlin
val dedup = RequestDedup()

// 多个协程并发调用，只会发送一次实际网络请求
val user = dedup.dedupRequest("user_info_$userId") {
    api.getUserInfo(userId)
}
```

### 请求节流（RequestThrottle）

限制同一请求的最低调用间隔，间隔内直接返回缓存结果：

```kotlin
val throttle = RequestThrottle(intervalMs = 3000) // 3秒节流间隔

// 3秒内重复调用直接返回上次结果
val list = throttle.throttleRequest("refresh_list") {
    api.getList()
}

// 手动清除缓存，强制下次重新请求
throttle.invalidate("refresh_list")
```

## 日志脱敏

### Header 脱敏

默认脱敏 `Authorization`、`Cookie`、`Set-Cookie`、`X-Auth-Token` 等敏感 Header，可扩展：

```kotlin
NetworkConfig(
    baseUrl = "...",
    sensitiveHeaders = NetworkConfig.DEFAULT_SENSITIVE_HEADERS + setOf("x-custom-secret")
)
```

### Body 字段脱敏

JSON Body 中的敏感字段（如密码、信用卡号）会被自动替换为 `****(masked)`：

```kotlin
NetworkConfig(
    baseUrl = "...",
    sensitiveBodyFields = NetworkConfig.DEFAULT_SENSITIVE_BODY_FIELDS + setOf("id_card", "phone")
)
```

> 默认脱敏字段：`password`、`pwd`、`secret`、`creditCard`、`cardNumber`、`cvv`、`ssn` 等。
> 脱敏同时支持嵌套 JSON 对象和 JSON 数组中的字段。

## 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| OkHttp | 4.12.0 | HTTP/WebSocket 传输层 |
| Retrofit | 2.11.0 | 类型安全 HTTP 接口 |
| Gson | 2.11.0 | JSON 序列化 |
| Hilt | 2.52 | 依赖注入 |
| Coroutines | 1.9.0 | 协程异步支持 |
| Kotlin | 2.0.21 | 语言版本 |

## 混淆规则

brick-net 已内置 `consumer-rules.pro`，引入后自动生效，无需额外配置。

## 最佳实践

1. **生产环境关闭详细日志**：使用 `networkLogLevel = NetworkLogLevel.BASIC` 或 `NONE`，避免 BODY 级别日志影响性能
2. **合理配置超时**：上传/下载接口使用 `@Timeout` 注解配置更长的超时，避免大文件传输被中断
3. **善用 `NetworkResult.fold()`**：在 UI 层使用 `fold()` 可以确保所有分支都被处理，避免遗漏错误场景
4. **WebSocket 离线消息**：按业务需要配置 `messageQueueCapacity`，避免过大导致内存压力
5. **Token 刷新**：`TokenProvider.refreshTokenBlocking()` 中应设置合理的超时，避免卡住 OkHttp 线程
6. **连接池调优**：高并发场景增大 `maxIdleConnections`，低频场景减小以节省资源
7. **证书固定**：安全敏感应用务必配置 `certificatePins`，并同时配置当前和备用证书 pin
8. **网络状态感知**：在发起请求前通过 `NetworkMonitor.isOnline()` 预判，避免不必要的超时等待

## 常见问题

详见 [FAQ.md](../FAQ.md#brick-net)。

## 线程模型与边界约束

### 线程安全

| 组件 | 线程安全 | 说明 |
|------|---------|------|
| `NetworkExecutor` | ✅ | 内部使用 Mutex 串行化 401 刷新，所有方法均为 suspend |
| `TokenAuthenticator` | ✅ | ReentrantLock 保护 token 刷新，同一时刻只有一个线程刷新 |
| `NetworkConfigProvider` | ✅ | AtomicReference 存储配置，监听器分发在调用线程 |
| `WebSocketManager` | ✅ | 内部使用 synchronized 保护状态机 |
| `NetworkMonitor` | ✅ | StateFlow 天然线程安全 |
| `RequestDedup` | ✅ | ConcurrentHashMap + CompletableDeferred |
| `RequestThrottle` | ✅ | ConcurrentHashMap + 时间戳比较 |

### 并发约束

- **401 刷新串行化**：多个请求同时收到 401 时，`TokenAuthenticator` 使用 `ReentrantLock` 确保仅触发一次刷新。后续请求等待刷新结果后使用新 Token 重试。
- **WebSocket 重连**：断线重连使用指数退避 + 随机抖动，最大重连次数由 `Config.maxReconnectAttempts` 控制，超限后 `state` 变为 `CLOSED`。
- **重试拦截器**：`RetryInterceptor` 在 OkHttp 链上同步执行，仅对幂等方法（GET/HEAD/PUT/DELETE/OPTIONS）重试。

### 生命周期约束

- `NetworkConfig` 提供后即通过 Hilt 注入，**不支持运行时更换 baseUrl 实例**（如需动态切换域名，请使用 `@BaseUrl` 注解）。
- `WebSocketManager.disconnect()` 应在 Activity/Application 销毁时调用，避免后台连接泄漏。
- Flow 类型的网络监听（`NetworkMonitor.isConnected`）应在 `lifecycleScope` 中 collect。

### 失败场景

| 场景 | 行为 |
|------|------|
| 网络不可用 | `NetworkResult.TechnicalFailure(RequestException(NO_NETWORK))` |
| 连接/读取超时 | `NetworkResult.TechnicalFailure(RequestException(TIMEOUT))` |
| HTTP 5xx | `NetworkResult.TechnicalFailure(ServerException(code))` |
| 业务码 ≠ successCode | `NetworkResult.BusinessFailure(code, msg)` |
| JSON 解析失败 | `NetworkResult.TechnicalFailure(ParseException)` |
| Token 刷新失败 | 触发 `UnauthorizedHandler.onUnauthorized()`，返回 null 不重试 |
| SSL 证书不匹配 | `NetworkResult.TechnicalFailure(RequestException(SSL_ERROR))` |
| 协程取消 | `NetworkResult.TechnicalFailure(RequestException(REQUEST_CANCELED))` |
| 下载 Hash 不匹配 | `HashVerificationStrategy` 决定删除或保留文件 |

## License

```
Copyright 2024 ail36413

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
