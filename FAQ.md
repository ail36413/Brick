# 常见问题 (FAQ)

## 通用

### Q: 各模块可以单独使用吗？

可以。十个模块互相独立，按需引入即可：

```kotlin
// 请替换为 JitPack Release 页面的最新版本号
val brickVersion = "1.0.0"

// 只使用网络模块
implementation("com.github.ail36413.Brick:brick-net:$brickVersion")
// 只使用工具模块
implementation("com.github.ail36413.Brick:brick-utils:$brickVersion")
```

唯一的约束是 `brick-net` 依赖 Hilt，使用前需集成 Hilt。

### Q: 支持的最低 Android 版本是多少？

`minSdk = 24`（Android 7.0）。

### Q: 需要额外配置 ProGuard 规则吗？

不需要。每个模块都内置了 `consumer-rules.pro`，会自动传递到使用方。

---

## brick-net

### Q: 如何彻底关闭网络日志？

```kotlin
NetworkConfig(
    baseUrl = "https://api.example.com/",
    isLogEnabled = false,
    networkLogLevel = NetworkLogLevel.NONE
)
```

### Q: Token 刷新时其他请求会阻塞吗？

是的。`TokenAuthenticator` 使用 `ReentrantLock` 串行化 401 处理。第一个线程执行刷新，后续线程等锁后会发现 token 已更新，直接复用新 token 重试——不会重复调用 `refreshTokenBlocking()`。

### Q: 如何自定义日志脱敏的 Header？

```kotlin
NetworkConfig(
    baseUrl = "https://api.example.com/",
    sensitiveHeaders = NetworkConfig.DEFAULT_SENSITIVE_HEADERS + setOf(
        "x-custom-secret",
        "x-internal-token"
    )
)
```

### Q: baseUrl 必须以 `/` 结尾吗？

是的。这是 Retrofit 的要求。库会在初始化阶段通过 `require` 校验，不合规会立即抛出异常。

### Q: WebSocket 离线消息队列有上限吗？

有。默认容量为 100 条（`messageQueueCapacity = 100`），队列满时默认丢弃最旧消息（`dropOldestWhenQueueFull = true`）。可通过 `WebSocketManager.Config` 自定义：

```kotlin
WebSocketManager.Config(
    enableMessageReplay = true,
    messageQueueCapacity = 500,
    dropOldestWhenQueueFull = false  // 改为丢弃新消息
)
```

### Q: 重试机制和 Token 刷新会冲突吗？

不会。`RetryInterceptor` 作为应用拦截器插入，处理网络层重试（超时、连接失败等）。Token 刷新由 OkHttp 的 `Authenticator` 在收到 401 后触发，两者在不同阶段生效。

---

## brick-utils

### Q: SpDelegate 必须在 Application 中初始化吗？

推荐在 `Application.onCreate()` 中调用 `init(context)`。未初始化就访问属性会抛出 `IllegalStateException`（消息明确告知原因）。

### Q: `observeNetworkState()` 会在后台持续运行吗？

不会。它返回的是冷 Flow，需要使用 `repeatOnLifecycle` 配合收集，当 Activity/Fragment 进入后台时会自动停止。

---

## brick-ui

### Q: StateLayout 状态视图什么时候 inflate？

首次切换到该状态时才 inflate（懒加载），未展示的状态不占内存。

### Q: StateLayout 切换动画可以关闭吗？

可以。通过 XML 或代码：

```xml
app:enableAnimation="false"
```

```kotlin
stateLayout.enableAnimation = false
```

### Q: SimpleAdapter 的 DiffUtil 必须配置吗？

`diff` 参数用于 `areItemsTheSame` 判断。如果不传，默认使用 `==` 比较，但推荐提供唯一标识比较以获得更好的动画效果。

---

## brick-image

### Q: 支持 GIF 吗？

Coil 本身支持 GIF。`brick-image` 的 `build.gradle.kts` 中已经引入 `coil-gif`，可直接使用 `imageView.loadImage(gifUrl)` 加载。

### Q: 如何清除图片缓存？

```kotlin
// 清除内存缓存
BrickImage.imageLoader(context).memoryCache?.clear()
// 清除磁盘缓存
BrickImage.imageLoader(context).diskCache?.clear()
```

---

## brick-arch

### Q: MVVM 和 MVI 应该选哪个？

| 场景 | 推荐 |
|------|------|
| 简单 CRUD 页面（列表、详情） | MVVM |
| 复杂交互（多状态联动、撤销/重做） | MVI |
| 团队不熟悉函数式编程 | MVVM |
| 需要严格的状态可追溯性 | MVI |

### Q: FlowEventBus 会内存泄漏吗？

不会，前提是正确使用。推荐使用生命周期安全的扩展方法：

```kotlin
// 推荐（自动管理生命周期）
observeEvent<LoginSuccessEvent> { event ->
    refreshUI(event.userId)
}

// 不推荐（需手动管理）
lifecycleScope.launch {
    FlowEventBus.observe<LoginSuccessEvent>().collect { ... }
}
```

### Q: FlowEventBus 的粘性事件如何清除？

```kotlin
FlowEventBus.removeSticky(ThemeChangedEvent::class.java.name)
// 或清除全部
FlowEventBus.clear()
```

---

## brick-store

### Q: MMKV 和 SharedPreferences 能共存吗？

可以。`SpMigration.migrate()` 只是将 SP 数据拷贝到 MMKV，原 SP 文件不会被删除。迁移完成后可以安全移除 SP 相关代码。

### Q: cryptKey 不应该硬编码吗？

是的。`cryptKey` 应从 [Android Keystore](https://developer.android.com/training/articles/keystore) 或密钥派生函数（如 PBKDF2）获取，避免反编译后密钥泄露。

### Q: 多进程环境下怎么办？

MMKV 原生支持多进程，需要在创建实例时指定 `MMKV.MULTI_PROCESS_MODE`。`MmkvDelegate` 目前使用单进程模式，如需多进程请直接使用 MMKV API。

---

## brick-log

### Q: 文件日志会影响性能吗？

不会。文件写入在 `SingleThreadExecutor` 独立线程池执行，调用线程仅提交任务即返回。

### Q: 如何在 Release 包中只保留文件日志？

```kotlin
BrickLogger.init {
    debug = false      // 关闭控制台输出
    fileLog = true      // 保留文件日志
    fileDir = "..."
}
```

### Q: BrickLog 和 BrickLogger 该选哪个？

| 场景 | 推荐 |
|------|------|
| 零依赖、简单项目 | `BrickLog`（brick-utils） |
| 文件日志、崩溃收集、JSON 格式化 | `BrickLogger`（brick-log） |

---

## brick-data

### Q: BaseDao 的 upsert 和 Room 2.5+ 的 @Upsert 有什么区别？

`BaseDao.upsert()` 使用 `@Insert(onConflict = REPLACE)` 实现，语义为"存在则替换整行"。Room 2.5+ 的 `@Upsert` 注解在冲突时只更新非主键列，不会触发 DELETE + INSERT。如果使用 Room 2.5+，建议优先使用 `@Upsert` 注解。

### Q: BrickConverters 是否需要手动注册？

是的。在 `@Database` 注解的类上添加 `@TypeConverters(BrickConverters::class)` 即可全局生效。

### Q: BrickConverters 支持哪些类型？

| 类型 | 存储格式 |
|------|---------|
| `Date` | `Long`（毫秒时间戳） |
| `List<String>` | `String`（JSON 数组） |
| `Map<String, String>` | `String`（JSON 对象） |

---

## brick-permission

### Q: Fragment 中调用会泄漏吗？

不会。内部使用隐藏 Fragment 观测宿主 Activity 生命周期，Activity 销毁后自动清理。配合 `lifecycleScope` 可确保协程自动取消。

### Q: 多个协程同时请求权限会冲突吗？

不会。`BrickPermission.request()` 内部使用 `Mutex` 串行化，确保同一时刻只有一个权限弹窗。后续请求会挂起等待前一个完成后再弹出。

### Q: 为什么 permanently denied 在部分设备上不准？

Android 未提供"永久拒绝"的公开 API，`brick-permission` 通过 `shouldShowRequestPermissionRationale()` 推断。部分国产 ROM 对此返回值有魔改，可能导致判断不准确。

---

## brick-startup

### Q: BACKGROUND 任务的依赖是否保证执行顺序？

是的。BACKGROUND 优先级内部使用 `CountDownLatch` 实现依赖等待——被依赖的任务执行完毕后，依赖方才会开始执行。

### Q: 初始化器的 name 有什么要求？

`name` 在全局必须唯一，重复注册会抛出 `IllegalArgumentException`。建议使用模块名作为 name（如 `"BrickLogger"`、`"BrickStore"`）。

### Q: DEFERRED 任务什么时候执行？

DEFERRED 任务通过 `Handler.post()` 投递到主线程消息队列末尾，在 IMMEDIATELY 和 NORMAL 任务全部完成后、首帧渲染空隙中执行。

### Q: 和 AndroidX App Startup 的区别？

| 特性 | AndroidX App Startup | brick-startup |
|------|---------------------|---------------|
| 优先级分级 | ❌ | ✅ 四级优先级 |
| 延迟初始化 | 手动 lazy | ✅ 自动空闲执行 |
| 后台异步 | ❌ | ✅ 子线程执行 |
| 启动报告 | ❌ | ✅ 耗时统计 |
| ContentProvider | 需要 | 不需要 |
