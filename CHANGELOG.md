# Changelog

本文件记录 Brick 的所有重要变更。格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

## [Unreleased]

### 新增
- **brick-arch**: `BrickArch` 全局配置入口 + `BrickLogger` 可插拔日志接口，替代硬编码 `Log.e`
- **brick-arch**: `SavedStateHandle` 集成 — `BaseViewModel` / `MviViewModel` 支持可选的进程恢复
- **brick-arch**: `dispatchThrottled()` Intent 节流分发，防止重复提交
- **brick-arch**: `Flow.select {}` 局部渲染操作符，仅指定字段变化时触发收集
- **brick-arch**: `retryLoadState()` 带指数退避的自动重试
- **brick-arch**: `BrickTestRule` JUnit 4 TestRule，消除 ViewModel 测试样板代码
- **brick-arch**: `MvvmDialogFragment` / `MvvmBottomSheetDialogFragment` — MVVM 弹窗基类
- **brick-arch**: `MviDialogFragment` / `MviBottomSheetDialogFragment` — MVI 弹窗基类
- **brick-arch**: FlowExt 单元测试 — throttleFirst / debounceAction / select 全覆盖

### 改进
- **CI**: 在 `.github/workflows/ci.yml` 中补齐 `android-tests` job，使用 Android Emulator 执行 `brick-permission`、`brick-ui`、`brick-arch` 的 `connectedDebugAndroidTest`

## [1.0.0] - 2026-04-04

### 改进
- **发布链路**: `publish.yml` 调整为更准确的 Release Validation 工作流，保留 tag 触发的 Release 构建校验并上传 Release AAR/APK artifact
- **贡献文档**: `CONTRIBUTING.md` 增加 `connectedDebugAndroidTest` 说明，明确 UI / permission / nav 相关改动的最低验证要求

## [1.0.0] - 2026-04-04

### 修复
- **brick-startup**: `executeBackgroundTask()` 在执行器并发关闭时自动重试投递，修复 `reset()` 后快速再次 `start()` 的竞态窗口

### 改进
- **CI**: GitHub Actions 新增 Android Emulator 仪器测试任务，执行 `brick-permission`、`brick-ui`、`brick-arch` 的 `connectedDebugAndroidTest`
- **测试配置**: `brick-permission`、`brick-ui`、`brick-arch` 增加 `testInstrumentationRunner` 和 `androidTest` 依赖
- **全局文档**: 根 README、FAQ 和模块 README 依赖示例版本同步更新为 `1.0.0`

### 新增测试
- **brick-permission**: 新增 `BrickPermissionInstrumentedTest`，覆盖真实设备环境下的已授权权限直返和空权限参数校验
- **brick-ui**: 新增 `StateLayoutInstrumentedTest`，覆盖真实设备环境下的状态切换和重试回调
- **brick-arch**: 新增 `BrickNavInstrumentedTest`，覆盖真实设备环境下的导航/返回栈行为

## [1.0.0] - 2026-04-04

### 修复
- **brick-startup**: `backgroundExecutor` 改为可重建生命周期，避免 `shutdown()` 后 `reset()` 再 `start()` 时复用已关闭线程池
- **brick-startup**: 清理 `TopologicalSortTest` 中误混入的无关文本，恢复测试文件完整性

### 改进
- **brick-permission**: `BrickPermission` / `PermissionFragment` / README 中的并发说明统一为“全局串行化”，与当前实现一致
- **全局文档**: 根 README、FAQ 及各模块 README 的依赖示例统一改为具体版本变量，不再保留 `latest.release`
- **工程配置**: `.gitignore` 增加 `.kotlin/`，避免 Kotlin daemon 错误日志进入版本控制
- **gradle.properties**: 删除仓库内 `org.gradle.java.home` 私有路径，只保留 JDK 17+ 配置说明

### 新增测试
- **brick-startup**: `BrickStartupRobolectricTest` 新增 `reset()` 后重建 BACKGROUND 执行器回归测试

## [1.0.0] - 2026-04-04

### 修复
- **brick-permission**: 重写 `PermissionFragment` 并发模型 — 每次请求创建独立实例（唯一 TAG），避免多个 `request()` 共享 continuation 导致挂起丢失。新增 `onDestroy()` 安全取消 continuation
- **brick-arch**: 修复 `BrickNav.navigate()` 首次调用防连点误拦截（`uptimeMillis < 300` 场景）

### 改进
- **brick-permission**: `BrickPermission` 类文档新增并发安全说明
- **brick-arch**: `FlowEventBus` 文档新增线程安全和生命周期要求说明
- **brick-arch**: `BrickNav` 文档新增线程要求和生命周期边界说明
- **brick-ui**: `SimpleAdapter` 文档新增 `submitList` 主线程约束说明
- **brick-ui**: `MultiTypeAdapter` 文档新增线程要求说明
- **brick-log**: `FileTree` 文档新增线程安全和 daemon 线程数据丢失边界说明
- **gradle.properties**: JDK 路径增加注释说明，提示在 `~/.gradle/gradle.properties` 中覆盖
- **README.md / FAQ.md**: 依赖示例从 `latest.release` / `$latestVersion` 改为具体版本号模式

### 新增测试（Robolectric 设备级测试）
- **brick-permission**: `BrickPermissionTest`（11 Tests）— 使用 ShadowApplication 验证 isGranted / isAllGranted / 扩展函数
- **brick-ui**: `StateLayoutTest`（11 Tests）— 状态切换、监听器、动画配置
- **brick-ui**: `AdapterTest`（10 Tests）— SimpleAdapter / MultiTypeAdapter 数据提交、点击、空视图、DSL
- **brick-startup**: `BrickStartupRobolectricTest`（10 Tests）— 完整初始化生命周期、DSL、错误隔离、依赖排序
- **brick-arch**: `BrickNavTest`（14 Tests）— 导航、返回栈、singleTop、拦截器、参数传递

## [1.0.0] - 2026-04-04

### 修复
- **brick-log**: `FileTree.rotateFile()` 检查 `renameTo()` 返回值，失败时记录警告而非静默忽略
- **brick-log**: `FileTree.cleanOldFiles()` 检查 `delete()` 返回值，失败时记录警告
- **brick-data**: `batchInsert()` 失败条目从静默跳过改为 `Log.w` 记录原因
- **brick-data**: `BrickConverters.fromStringMap()` JSON 解析失败时记录警告日志
- **brick-store**: `BrickStore.init()` 使用 `synchronized` 双重检查锁保证线程安全
- **brick-startup**: `runInitializer()` 将 `onCompleted()` 与 `onCreate()` 分离 try-catch，避免异常遮蔽
- **brick-arch**: `MviViewModelTest.launchIO` 用轮询+超时替代固定 `Thread.sleep(200)`，消除不稳定因素

### 改进
- **brick-utils**: `View.onClick()` 新增 `require(interval > 0)` 校验
- **brick-data**: `build.gradle.kts` 添加 `testOptions.unitTests.isReturnDefaultValues = true`

## [1.0.0] - 2026-04-04

### 改进
- **全局**: 所有 `@Suppress` 注解添加理由注释（17 处），提升代码可审查性
- **brick-log**: `LogConfig.maxFileSize` / `maxFileCount` 新增 `require > 0` 输入校验
- **brick-net**: `DefaultRetryStrategy` 构造函数新增四项参数校验（`maxRetries >= 0`、`initialBackoffMillis > 0`、`maxBackoffMillis >= initial`、`factor >= 1.0`）
- **brick-data**: `migration()` 新增 `startVersion >= 1` 和 `endVersion > startVersion` 校验
- **brick-permission**: `BrickPermission.request()` 新增权限数组非空校验和权限名非空白校验
- **brick-image**: `ImageView.loadRounded()` 新增 `radiusPx >= 0` 校验

### 新增测试（275 个，+69）
- **brick-log**: `LogConfigTest`（8 Tests）、`LogFileManagerTest`（16 Tests） — 首次覆盖
- **brick-data**: `MigrationHelperTest`（6 Tests）— 输入校验测试
- **brick-net**: `RetryStrategyValidationTest`（9 Tests）— 构造参数边界测试
- **brick-utils**: `FileExtTest`（16 Tests）— 文件操作全覆盖
- **brick-permission**: `PermissionResultTest`（7 Tests）— 首次覆盖
- **brick-store**: `BrickStoreTest`（2 Tests）— 首次覆盖
- **brick-startup**: `TopologicalSortTest` 扩展（+5 Tests）— StartupConfig DSL 测试

## [1.0.0] - 2026-04-04

### 修复
- **brick-startup**: 修复 `BACKGROUND` 任务跨优先级依赖可能导致死锁的问题 — `latchMap` 仅等待同为 BACKGROUND 的依赖
- **brick-startup**: BACKGROUND 线程池在所有任务完成后自动 `shutdown()`，避免线程泄漏
- **brick-startup**: `results` 改用 `Collections.synchronizedList` 保证多线程写入安全
- **brick-log**: 修复 `FileTree.cleanOldFiles()` off-by-one 错误 — `>= maxFileCount` 改为 `> maxFileCount`，正确保留指定数量的日志文件
- **brick-net**: `ProgressRequestBody` 修复 `contentLength` 已知时可能发射两次 `done=true` 回调的问题
- **brick-arch**: `BrickNav` 防连点时钟从 `System.currentTimeMillis()` 改为 `SystemClock.uptimeMillis()`，不受系统时间调整影响
- **brick-ui**: `LoadMoreAdapter.loadState` 添加 `@Volatile` 保证多线程可见性
- **README.md**: 技术栈表格补充 KSP 版本号

## [1.0.4] - 2026-04-04

### 新增
- **brick-net**: `WebSocketManager.Config` 新增 `maxReconnectAttempts` 参数，限制最大重连次数（0 表示无限制），防止无限重连耗尽电量

### 修复
- **brick-startup**: 修复 `BACKGROUND` 任务依赖顺序不保证的问题 — 改用 `CountDownLatch` DAG 调度，确保子线程异步执行时依赖任务先完成
- **brick-startup**: `register()` 新增名称唯一性校验，重复注册立即抛出 `IllegalArgumentException`
- **brick-permission**: `BrickPermission.request()` 增加 `Mutex` 串行化，防止同一 Activity 并发请求权限时 continuation 被覆盖
- **brick-net**: `DownloadExecutor` 修复 `CancellationException` 被捕获未重抛的问题，导致协程取消无法正常传播
- **brick-net**: `WebSocketClientImpl.disconnect(permanent=true)` 新增 `mainHandler.removeCallbacksAndMessages(null)` 清理，防止已 post 的回调在断开后继续执行
- **brick-net**: `TokenAuthenticator` token 刷新异常不再静默吞没，改为 `Log.w` 输出，便于排查认证问题
- **brick-net**: `NetworkModule` 四处 `try/catch` 不再静默忽略异常，改为 `Log.w` 输出配置错误（重试、认证、缓存、证书固定）
- **brick-ui**: `LoadMoreAdapter` 修复 `OnScrollListener` 泄漏 — 添加 `onDetachedFromRecyclerView` 反注册，防止 adapter 重复 attach 时监听器累积导致 `onLoadMore` 多次触发
- **brick-store**: `SpMigration` 迁移后清除 SP 数据改用 `commit()` 同步提交，避免 `apply()` 异步导致进程异常终止时数据重复
- **brick-data**: `BrickConverters` Map 转换从 `key=value;` 分隔符格式改为 JSON 格式，修复 value 含 `;` 或 `=` 时的解析错误

### 文档修正
- **brick-store**: `BrickStore.kt` KDoc 移除不存在的 `logLevel` 参数示例
- **brick-startup**: `AppInitializer.onCompleted()` KDoc 修正为"当前初始化器执行完毕后立即调用"（而非"同优先级全部完成后"）
- **brick-startup**: README 修正为"自研调度器"（而非"基于 AndroidX Startup"）
- **FAQ.md**: 模块数量从"五个"更新为"十个"，依赖版本号改为 `$latestVersion`
- **FAQ.md**: `BrickImage` 缓存清理 API 修正为 `imageLoader(context)` 调用方式
- **README.md**: 模块版本号统一改为 `latest.release`，不再硬编码版本号
- **README.md**: Hilt 要求描述修正为仅 `brick-net` 需要
- **CHANGELOG.md**: 修正 `1.0.0` 日期为 `2025-01-14`（原错误标记为 `2026-04-03`）
- **CONTRIBUTING.md**: 项目结构补齐 5 个新增模块，scope 列表补全

## [1.0.0] - 2025-01-16

### 新增
- **brick-arch**: `BrickNav` 纯代码路由 Fragment 导航管理器
  - 零 XML 导航图，代码注册路由 `register<Fragment>("route")`
  - `NavAnim` 四种内置转场动画（SLIDE_HORIZONTAL / SLIDE_VERTICAL / FADE / NONE）
  - `NavOptions` DSL（addToBackStack / singleTop / 自定义动画）
  - `NavInterceptor` 导航拦截器（登录检查、埋点等）
  - 返回栈管理 `back()` / `backTo()` / `clearStack()`
  - 8 套内置动画资源（水平滑动、垂直弹出、淡入淡出）
  - `OnBackPressedCallback` 自动处理系统返回键
  - 防连点保护（300ms 内忽略重复导航）
  - State-loss 安全（`isStateSaved` 检查，不会在 onSaveInstanceState 后崩溃）
  - 内部维护 `currentRoute`（不依赖 FragmentManager 异步状态）
  - 所有导航/返回操作使用同步 commit（`commit` + `executePendingTransactions` / `popBackStackImmediate`）
- **brick-startup**: 新增模块 — 四级优先级启动优化框架
  - `InitPriority` 四级初始化优先级（IMMEDIATELY / NORMAL / DEFERRED / BACKGROUND）
  - `AppInitializer` 初始化器接口（名称、优先级、依赖声明）
  - `BrickStartup` 启动管理器（DSL 配置、拓扑排序、性能报告）
  - `InitResult` 初始化结果数据类（耗时统计、异常捕获）
  - DEFERRED 空闲初始化（IdleHandler）、BACKGROUND 线程池异步初始化
- **brick-ui**: `BrickAnim` View 动画扩展函数（fadeIn / fadeOut / slideIn / slideOut / scaleIn / pulse / shake / bounce / rotate / fadeSlideIn / fadeSlideOut）
- **brick-ui**: `BrickItemAnimator` RecyclerView Item 入场动画工具（5 种动效 + 逐个延迟）

### 修复
- **brick-arch**: BrickNav `commitNow` + `addToBackStack` 崩溃修复（AndroidX 禁止此组合，改用 `commit` + `executePendingTransactions`）
- **brick-arch**: BrickNav Fragment 实例化改用 `FragmentFactory`，兼容 Hilt `@AndroidEntryPoint` Fragment
- **brick-arch**: BrickNav 新增 `OnBackPressedCallback` 自动处理系统返回键（防止返回键错乱）
- **brick-arch**: BrickNav `currentRoute` 改为内部维护，避免依赖 `isVisible` 异步状态导致不准确
- **brick-arch**: BrickNav 所有操作增加 `isStateSaved` 检查，防止 `onSaveInstanceState` 后崩溃
- **brick-arch**: BrickNav 增加 300ms 防连点保护，防止快速点击重复压栈
- **brick-arch**: BrickNav 配置变更/进程恢复后自动同步当前路由状态
- **brick-startup**: 拓扑排序 `visiting` 集合增加 try-finally 保护，防止异常时状态污染
- **brick-image**: `BlurTransformation` 重构 — 抽取 `StackBlur` 为独立对象，水平/垂直扫描抽取为独立方法，消除重复代码
- **brick-permission**: `requestWithRationale` 新增 `cancel` 回调参数，修复用户关闭 dialog 不调用 proceed 导致协程永久挂起
- **brick-data**: `BaseDao.upsertAll()` 改用批量 `insertAllOrIgnore` + 批量 `updateAll`，修复 N+1 查询性能问题
- **brick-startup**: 新增循环依赖检测，避免 `sortByDependencies` 无限递归
- **brick-startup**: `resultCallback` 在 BACKGROUND 线程执行时自动切回主线程
- **brick-startup**: 跨优先级依赖引用时输出警告日志（而非静默忽略）
- **brick-utils**: 移除 `isVisible` / `isGone` 扩展属性，避免与 AndroidX core-ktx 冲突

### 变更
- **app**: `SampleApp` 重构为 `BrickStartup` DSL 初始化（替换手动逐个初始化）
- **app**: 新增 `StartupDemoActivity` — 启动报告、优先级信息、延迟初始化演示
- **app**: 新增 `AnimDemoActivity` — View 动画效果演示（淡入淡出、滑入滑出、缩放、抖动、组合）
- **app**: 新增 `NavDemoActivity` — BrickNav 导航演示（路由注册、转场动画、返回栈管理）
- **app**: `MainActivity` 新增 brick-startup、动画、导航 演示入口
- **README.md**: 更新模块总览表（10 个模块），新增 brick-startup 快速上手示例

## [1.0.0] - 2025-01-15

### 新增
- **brick-store**: 新增模块 — 基于 MMKV 的高性能键值存储
  - `BrickStore` 初始化入口，支持自定义存储目录
  - `MmkvDelegate` 属性委托（string/int/long/float/double/boolean/bytes/stringSet/parcelable）
  - `SpMigration` SharedPreferences → MMKV 一键迁移
  - 支持 AES-CFB 加密存储
- **brick-log**: 新增模块 — 基于 Timber 的增强日志系统
  - `BrickLogger` 日志入口，DSL 配置
  - `BrickDebugTree` 自动 Tag + 方法名 + 行号
  - `FileTree` 异步文件日志（按大小轮转、自动清理）
  - `CrashTree` ERROR+ 崩溃日志收集（支持自定义上报）
  - `LogFileManager` 日志文件管理（压缩/导出/清理）
  - JSON 格式化输出、Lambda 延迟拼接
- **brick-data**: 新增模块 — 基于 Room 的数据库封装
  - `BrickDatabase` DSL 构建器（支持内存数据库）
  - `BaseDao<T>` 泛型基类（insert/update/delete/upsert/insertOrIgnore）
  - `DbResult<T>` 结果包装（Loading/Success/Failure + 操作符）
  - `BrickConverters` 类型转换器（Date↔Long、List↔String、Map↔String）
  - `TransactionHelper` 事务辅助（原子操作、批量插入）
  - `MigrationHelper` DSL 迁移构建
- **brick-permission**: 新增模块 — 基于协程的运行时权限请求
  - `BrickPermission` 协程挂起请求（request/requestWithRationale）
  - `PermissionResult` 三态结果（granted/denied/permanentlyDenied）
  - `PermissionFragment` 隐藏 Fragment 代理
  - `PermissionExt` Activity/Fragment 扩展函数
- **brick-arch**: `BaseBottomSheetDialogFragment<VB>` — 可配置 peekHeight、maxHeightRatio、isDraggable 等
- **brick-ui**: `LoadMoreAdapter<VB, T>` — 分页加载适配器，支持 LOADING/NO_MORE/FAILED 状态

### 变更
- **app**: `SampleApp` 完善初始化（BrickLogger、BrickImage、BrickStore）
- **app**: `MainActivity` 新增 brick-store / brick-log / brick-data / brick-permission 演示入口
- **app**: 新增 `StoreDemoActivity` — MMKV 读写、加密存储、存储管理演示
- **app**: 新增 `LogDemoActivity` — 日志级别、Lambda 日志、JSON 格式化、文件日志演示
- **app**: 新增 `DataDemoActivity` — Room CRUD、Upsert、DbResult 演示
- **app**: 新增 `PermissionDemoActivity` — 权限检查、协程请求、扩展函数演示
- **README.md**: 更新模块总览表（9 个模块），新增各模块快速上手示例

## [1.0.0] - 2025-01-14

### 新增
- **brick-arch**: `LoadState<T>` 通用加载状态密封类（Loading / Success / Error），附带 `map` / `fold` / `getOrNull` / `getOrDefault` / `onSuccess` / `onError` / `onLoading` / `loadStateCatching` 等操作符
- **brick-arch**: `launchIO {}` / `launchDefault {}` / `withMain {}` — BaseViewModel 和 MviViewModel 均新增协程调度器便捷方法
- **brick-arch**: `throttleFirst()` / `debounceAction()` — Flow 节流与防抖操作符
- **brick-arch**: `View.throttleClicks()` — View 点击事件转 Flow，内置节流防重复
- **brick-arch**: `getViewModel(factory)` / `getActivityViewModel(factory)` — 支持自定义 ViewModelProvider.Factory
- **brick-arch**: `LoadStateTest` — LoadState 完整单元测试覆盖

### 变更
- **brick-arch**: `MviViewModelTest` 新增 `launchIO` 协程调度测试用例
- **brick-arch**: README 新增 LoadState 状态管理、协程调度器、Flow 操作符文档
- **app**: ArchDemoActivity 演示新增 LoadState fold 渲染、launchIO 异步加载、throttleClicks 节流点击

## [1.0.0] - 2024-12-01

### 新增
- **brick-net**: HTTP 网络请求模块
  - 统一结果包装 `NetworkResult<T>`（Success / TechnicalFailure / BusinessFailure）
  - 文件下载（SHA-256 校验 + 进度回调）、文件上传（单文件/多文件/Multipart）
  - Token 鉴权（自动刷新、401 拦截、并发安全）
  - 指数退避重试、动态 BaseUrl（`@BaseUrl`）、按接口超时（`@Timeout`）
  - 响应字段映射、JSON 美化日志（敏感信息脱敏）
  - `NetEvent` 请求监控、`pollingFlow()` 轮询工具
- **brick-net**: WebSocket 模块
  - 多连接管理、心跳检测、断线重连（指数退避 + 随机抖动）
  - 离线消息队列（可配置容量与溢出策略）
- **brick-utils**: 通用 Kotlin 扩展函数
  - 尺寸转换（`dp`/`dpF`）、防抖点击（`onClick{}`）、SP 属性委托（`SpDelegate`）
  - 字符串校验（手机号/邮箱）、脱敏、MD5/SHA-256
  - 日期格式化、友好时间、文件操作、剪贴板、Toast
  - 网络状态监听 Flow（`observeNetworkState()`）
- **brick-ui**: 通用 UI 组件
  - `StateLayout`（四态布局，懒加载）、`TitleBar`（标题栏）
  - `RoundLayout`（圆角容器）、`FlowLayout`（流式布局）、`BadgeView`（角标）
  - `SimpleAdapter` / `MultiTypeAdapter`（ViewBinding + DiffUtil）
  - `BrickDialog`（confirm/alert/input/list/bottomList）、`LoadingDialog`
  - `DividerDecoration`（可配置分割线）
- **brick-image**: 基于 Coil 的图片加载
  - DSL API、圆形/圆角、预加载（`ImagePreloader`）
  - 内置变换：灰度、颜色滤镜、边框
- **brick-arch**: 架构基座
  - MVVM 模式：`BaseViewModel` / `MvvmActivity` / `MvvmFragment`
  - MVI 模式：`MviViewModel` / `MviActivity` / `MviFragment` + `UiState` / `UiEvent` / `UiIntent`
  - `FlowEventBus`（普通事件 + 粘性事件）
  - ViewBinding 委托、Lifecycle 扩展（`collectOnLifecycle`/`launchOnStarted`/`launchOnResumed`）
