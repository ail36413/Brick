# brick-startup

Android 应用启动优化模块，支持按优先级分级初始化，有效缩短 `Application.onCreate()` 的同步阻塞时间。

## 设计背景

传统方式在 `Application.onCreate()` 中顺序初始化所有组件，随着组件增多，启动时间线性增长。`brick-startup` 将初始化任务分为四个优先级，关键组件同步初始化，非关键组件延迟或异步初始化。

## 特性

- **四级优先级**：IMMEDIATELY / NORMAL / DEFERRED / BACKGROUND
- **依赖排序**：同一优先级内支持声明依赖关系，自动拓扑排序
- **启动报告**：记录每个初始化器的耗时、成功/失败状态
- **DSL 配置**：简洁的 DSL 注册初始化器
- **零侵入**：不需要 ContentProvider，不修改 Manifest
- **自研调度器**：独立实现的启动调度框架，不依赖 AndroidX Startup

## 四级优先级

| 优先级 | 时机 | 线程 | 适用场景 |
|--------|------|------|----------|
| **IMMEDIATELY** | 最先执行 | 主线程同步 | 崩溃收集、日志框架 |
| **NORMAL** | 紧随其后 | 主线程同步 | 网络、图片加载、键值存储 |
| **DEFERRED** | 主线程空闲后 | 主线程异步 | 统计上报、推送注册、预加载 |
| **BACKGROUND** | 子线程 | IO 线程 | 缓存清理、数据预热 |

> **关键收益**：将 DEFERRED 和 BACKGROUND 的组件从同步路径移除后，`Application.onCreate()` 的阻塞时间可显著缩短。

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-startup:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 基本用法

### 1. 定义初始化器

```kotlin
class LogInitializer : AppInitializer {
    override val name: String = "BrickLogger"
    override val priority: InitPriority = InitPriority.IMMEDIATELY

    override fun onCreate(context: Context) {
        BrickLogger.init {
            debug = BuildConfig.DEBUG
            fileLog = true
            fileDir = "${context.cacheDir}/logs"
        }
    }
}

class ImageInitializer : AppInitializer {
    override val name: String = "BrickImage"
    override val priority: InitPriority = InitPriority.NORMAL
    override val dependencies: List<String> = listOf("BrickLogger")

    override fun onCreate(context: Context) {
        BrickImage.init(context) { diskCacheSize(128L * 1024 * 1024) }
    }
}

class AnalyticsInitializer : AppInitializer {
    override val name: String = "Analytics"
    override val priority: InitPriority = InitPriority.DEFERRED

    override fun onCreate(context: Context) {
        AnalyticsSDK.init(context)
    }
}

class CacheCleanInitializer : AppInitializer {
    override val name: String = "CacheClean"
    override val priority: InitPriority = InitPriority.BACKGROUND

    override fun onCreate(context: Context) {
        CacheManager.cleanExpired(context)
    }
}
```

### 2. 在 Application 中启动

```kotlin
@HiltAndroidApp
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BrickStartup.init(this) {
            // IMMEDIATELY — 必须最先
            add(LogInitializer())

            // NORMAL — 常规
            add(StoreInitializer())
            add(ImageInitializer())

            // DEFERRED — 空闲时
            add(AnalyticsInitializer())

            // BACKGROUND — 子线程
            add(CacheCleanInitializer())

            // 可选：监听结果
            onResult { result ->
                Log.d("Startup", "${result.name}: ${result.costMillis}ms")
            }
        }
    }
}
```

### 3. 查看启动报告

```kotlin
val report = BrickStartup.getReport()
report.forEach { result ->
    Log.d("Startup", "${result.name} [${result.priority}] ${result.costMillis}ms ${if (result.success) "✓" else "✗"}")
}

// 同步总耗时（仅 IMMEDIATELY + NORMAL）
Log.d("Startup", "同步耗时: ${BrickStartup.getSyncCostMillis()}ms")
```

## 依赖排序

同一优先级内可声明依赖关系，被依赖的初始化器会先执行：

```kotlin
class NetworkInitializer : AppInitializer {
    override val name = "Network"
    override val priority = InitPriority.NORMAL
    override val dependencies = listOf("BrickLogger", "BrickStore")

    override fun onCreate(context: Context) {
        // BrickLogger 和 BrickStore 已经初始化完成
        NetworkClient.init(context)
    }
}
```

## 手动注册方式

除了 DSL 方式，也支持手动注册：

```kotlin
BrickStartup.register(LogInitializer())
BrickStartup.register(StoreInitializer())
BrickStartup.register(ImageInitializer())
BrickStartup.start(context)
```

## InitResult 说明

| 属性 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 初始化器名称 |
| `priority` | `InitPriority` | 优先级 |
| `costMillis` | `Long` | 耗时（毫秒） |
| `success` | `Boolean` | 是否成功 |
| `error` | `Throwable?` | 失败异常 |

## 与 AndroidX App Startup 的区别

| 特性 | AndroidX App Startup | brick-startup |
|------|---------------------|---------------|
| 优先级分级 | ❌ | ✅ 四级优先级 |
| 延迟初始化 | 手动 lazy | ✅ 自动空闲执行 |
| 后台异步 | ❌ | ✅ 子线程执行 |
| 启动报告 | ❌ | ✅ 耗时统计 |
| ContentProvider | 需要 | 不需要 |
| 依赖排序 | ✅ | ✅ |
| DSL 配置 | ❌ | ✅ |

## 最佳实践

1. **日志/崩溃收集** 放 `IMMEDIATELY`，确保其他组件初始化异常也能被记录
2. **首屏必需** 的组件放 `NORMAL`（网络、图片、存储）
3. **非首屏** 组件放 `DEFERRED`（统计、推送、广告 SDK）
4. **耗时且不依赖主线程** 的任务放 `BACKGROUND`（缓存扫描、数据库预热）
5. 组件之间有依赖关系时，通过 `dependencies` 声明而非靠注册顺序

## 线程模型与边界约束

### 线程安全

| 方法 | 线程要求 | 说明 |
|------|---------|------|
| `BrickStartup.init()` | **主线程** | 必须在 `Application.onCreate()` 中调用 |
| `BrickStartup.register()` | **主线程** | 仅在 `init` 之前或 DSL 块内调用 |
| `BrickStartup.getReport()` | 任意线程 | 返回不可变快照，线程安全 |
| `AppInitializer.onCreate()` | 取决于优先级 | IMMEDIATELY/NORMAL/DEFERRED 在主线程；BACKGROUND 在 IO 线程 |

### 生命周期约束

- **必须在首个 Activity 启动前调用** `BrickStartup.init()`，否则依赖组件可能未就绪。
- `init()` 只能调用一次，重复调用会被忽略。
- BACKGROUND 任务运行在模块内部维护的固定线程池上，**不能访问 View 或 UI 组件**。
- 所有 BACKGROUND 任务完成后线程池会被关闭；再次 `reset()` 后重新 `start()` 时会自动重建，仅测试场景需要依赖该行为。
- DEFERRED 任务在主线程 Handler 空闲时执行，**不保证在特定 Activity 创建前完成**。

### 失败场景

| 场景 | 行为 |
|------|------|
| 初始化器 `onCreate()` 抛异常 | 捕获异常，`InitResult.success = false`，**不影响后续初始化器** |
| 循环依赖 | 拓扑排序检测到环，抛出 `IllegalStateException` |
| 依赖的初始化器不存在 | 忽略该依赖，正常执行（不阻塞） |
| 重复 name 注册 | 抛出 `IllegalArgumentException` |

## FAQ

### Q: BACKGROUND 任务的依赖是否保证执行顺序？

是的。BACKGROUND 优先级内部使用 CountDownLatch 实现依赖等待——被依赖的任务执行完毕后，依赖方才会开始执行，等效于同步依赖保证。

### Q: 初始化器的 name 有什么要求？

`name` 在全局必须唯一，重复注册会抛出 `IllegalArgumentException`。建议使用模块名作为 name（如 `"BrickLogger"`、`"BrickStore"`）。

### Q: DEFERRED 任务什么时候执行？

DEFERRED 任务通过 `Handler.post()` 投递到主线程消息队列末尾，在 IMMEDIATELY 和 NORMAL 任务全部完成后、首帧渲染空隙中执行。
