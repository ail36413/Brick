# Brick

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)
[![CI](https://github.com/ail36413/Brick/actions/workflows/ci.yml/badge.svg)](https://github.com/ail36413/Brick/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)

**Brick** 是一套模块化的 Android 基础开发框架，基于 Kotlin 构建，提供网络请求、工具扩展、UI 组件、图片加载、架构基座、键值存储、日志系统、数据库、权限管理和启动优化十大能力。每个模块独立发布、按需引入，像搭积木一样组合使用。

## 技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 2.0.21 |
| AGP | 8.7.3 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| JVM Target | 17 |
| Hilt | 2.52 |
| KSP | 2.0.21-1.0.28 |
| OkHttp | 4.12.0 |
| Retrofit | 2.11.0 |
| Coroutines | 1.9.0 |
| Coil | 2.7.0 |
| Lifecycle | 2.8.7 |
| Timber | 5.0.1 |
| MMKV | 2.0.1 |
| Room | 2.6.1 |
| AndroidX Startup | 1.2.0 |

## 模块总览

| 模块 | 功能 | 说明 |
|------|------|------|
| **brick-net** | 网络请求 | HTTP + WebSocket，基于 OkHttp/Retrofit/Hilt，统一结果包装、Token 鉴权、文件上传下载、重试、轮询 |
| **brick-utils** | 工具扩展 | 高频 Kotlin 扩展函数：尺寸转换、SP 委托、字符串校验、日期格式化、网络状态监听等 |
| **brick-ui** | UI 组件 | 多状态布局、标题栏、流式布局、RecyclerView 适配器、对话框工具 |
| **brick-image** | 图片加载 | 基于 Coil 的 DSL 封装、圆形/圆角、预加载、自定义变换 |
| **brick-arch** | 架构基座 | MVVM + MVI 双模式、FlowEventBus 事件总线、ViewBinding 委托、Lifecycle 扩展 |
| **brick-store** | 键值存储 | 基于 MMKV 的高性能键值存储，属性委托、加密存储、SP 迁移 |
| **brick-log** | 日志系统 | 基于 Timber 的增强日志，文件日志、崩溃收集、JSON 格式化、自动 Tag |
| **brick-data** | 数据库 | 基于 Room 的数据库封装，BaseDao 泛型基类、DSL 构建器、迁移助手、结果包装 |
| **brick-permission** | 权限管理 | 基于协程的运行时权限请求，支持 Rationale、永久拒绝检测、应用设置跳转 |
| **brick-startup** | 启动优化 | 四级初始化优先级（IMMEDIATELY/NORMAL/DEFERRED/BACKGROUND）、依赖排序、启动报告 |

## 引入方式

### 1. 添加 JitPack 仓库

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. 添加依赖

```kotlin
// app/build.gradle.kts
dependencies {
    // 全部引入（请替换为 JitPack Release 页面的最新版本号）
    val brickVersion = "1.0.0"
    implementation("com.github.ail36413.Brick:brick-net:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-utils:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-ui:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-image:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-arch:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-store:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-log:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-data:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-permission:$brickVersion")
    implementation("com.github.ail36413.Brick:brick-startup:$brickVersion")

    // 或按需引入单个模块
    implementation("com.github.ail36413.Brick:brick-net:$brickVersion")
}
```

### 3. Hilt 配置（brick-net 需要）

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

```kotlin
@HiltAndroidApp
class MyApp : Application()
```

## 快速上手

### brick-net — 网络请求

只需提供 `baseUrl`，即可发起类型安全的网络请求：

```kotlin
// 1. 配置
@Module @InstallIn(SingletonComponent::class)
object AppNetworkModule {
    @Provides @Singleton
    fun provideNetworkConfig() = NetworkConfig(
        baseUrl = "https://api.example.com/"
    )
}

// 2. 定义接口
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): GlobalResponse<User>
}

// 3. 发起请求
@Inject lateinit var executor: NetworkExecutor
@Inject lateinit var retrofit: Retrofit

val result = executor.executeRequest { retrofit.create(UserApi::class.java).getUser(1) }
result
    .onSuccess { user -> showUser(user) }
    .onBusinessFailure { code, msg -> showToast("业务错误: $msg") }
    .onTechnicalFailure { ex -> showToast("网络错误: ${ex.message}") }
```

更多功能：Token 鉴权、文件下载/上传（带进度）、WebSocket 多连接、响应字段映射、动态 BaseUrl、重试退避、轮询 → [brick-net/README.md](brick-net/README.md)

### brick-utils — 工具扩展

```kotlin
// 尺寸转换
val px = 16.dp

// 防抖点击
button.onClick(interval = 500) { submit() }

// SP 属性委托
object AppPrefs : SpDelegate("app_prefs") {
    var token by string("token", "")
    var isFirst by boolean("first_launch", true)
}

// 字符串校验
"13812345678".isPhoneNumber()  // true
"13812345678".maskPhone()      // "138****5678"
"hello".md5()                  // "5d41402abc..."

// 网络状态监听
context.observeNetworkState().collect { isAvailable -> updateUI(isAvailable) }
```

→ [brick-utils/README.md](brick-utils/README.md)

### brick-ui — UI 组件

```kotlin
// 多状态布局
stateLayout.showLoading()
stateLayout.showContent()
stateLayout.showError { retryLoad() }

// 单类型适配器（ViewBinding + DiffUtil）
val adapter = SimpleAdapter<ItemBinding, Item>(
    inflate = ItemBinding::inflate,
    diffCallback = object : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
) { binding, item, _ -> binding.tvName.text = item.name }

// 快捷对话框
BrickDialog.confirm(this, "提示", "确定删除？") { deleteItem() }
BrickDialog.input(this, "修改昵称", hint = "请输入") { text -> update(text) }
```

→ [brick-ui/README.md](brick-ui/README.md)

### brick-image — 图片加载

```kotlin
// 初始化
BrickImage.init(this) {
    memoryCacheSize(0.3)
    diskCacheSize(256L * 1024 * 1024)
}

// 加载
imageView.loadImage(url)
imageView.loadCircle(avatarUrl)
imageView.loadRounded(url, radiusPx = 12f)

// DSL
imageView.loadImage(url) {
    placeholder(R.drawable.loading)
    crossfade(300)
    transform(GrayscaleTransformation())
}

// 预加载
ImagePreloader.preloadAll(context, urls)
```

→ [brick-image/README.md](brick-image/README.md)

### brick-arch — 架构基座

**MVVM 模式** — 通过 `viewModelClass()` 声明 ViewModel 类型，开箱即用：

```kotlin
class HomeViewModel : BaseViewModel() {
    val items = MutableLiveData<List<String>>()
    fun loadData() = launch {
        showLoading()
        items.value = repository.fetchItems()
        showLoading(false)
    }
}

class HomeActivity : MvvmActivity<ActivityHomeBinding, HomeViewModel>() {
    override fun viewModelClass() = HomeViewModel::class.java
    override fun inflateBinding(inflater: LayoutInflater) = ActivityHomeBinding.inflate(inflater)
    override fun initView(savedInstanceState: Bundle?) { viewModel.loadData() }
    override fun initObservers() { viewModel.items.observe(this) { render(it) } }
}
```

**MVI 模式** — 单向数据流、不可变状态：

```kotlin
class ListViewModel : MviViewModel<ListState, ListEvent, ListIntent>(ListState()) {
    override fun handleIntent(intent: ListIntent) {
        when (intent) {
            ListIntent.Refresh -> {
                updateState { copy(isLoading = true) }
                launch {
                    val data = repo.load()
                    updateState { copy(isLoading = false, items = data) }
                }
            }
        }
    }
}
```

**FlowEventBus** — 基于 SharedFlow 的事件总线：

```kotlin
FlowEventBus.post(LoginSuccessEvent(userId))
launchOnStarted { FlowEventBus.observe<LoginSuccessEvent>().collect { refreshUI(it) } }
```

→ [brick-arch/README.md](brick-arch/README.md)

### brick-store — 键值存储

基于 MMKV，性能远超 SharedPreferences：

```kotlin
// 初始化
BrickStore.init(this)

// 定义存储
object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
}

// 读写
UserStore.token = "abc123"
val t = UserStore.token  // "abc123"

// 加密存储
object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
    var password by string("password", "")
}

// 从 SP 迁移
SpMigration.migrate(this, "app_prefs")
```

→ [brick-store/README.md](brick-store/README.md)

### brick-log — 日志系统

基于 Timber，支持文件日志、崩溃收集：

```kotlin
// 初始化
BrickLogger.init {
    debug = BuildConfig.DEBUG
    fileLog = true
    fileDir = "${cacheDir.absolutePath}/logs"
    crashLog = true
    crashHandler = { tag, throwable, message ->
        // 上报到 Firebase / Bugly
    }
}

// 输出日志（自动获取类名为 Tag，带方法名+行号）
BrickLogger.d("请求成功")
BrickLogger.e(exception, "请求失败")

// Lambda 延迟拼接（关闭日志时零开销）
BrickLogger.d { "响应: ${response.body}" }

// JSON 格式化输出
BrickLogger.json(responseJson)
```

→ [brick-log/README.md](brick-log/README.md)

### brick-data — 数据库

基于 Room，提供 BaseDao、DSL 构建器、结果包装：

```kotlin
// 定义 Dao（继承 BaseDao 获得完整 CRUD）
@Dao
abstract class UserDao : BaseDao<UserEntity>() {
    @Query("SELECT * FROM users ORDER BY name ASC")
    abstract fun observeAll(): Flow<List<UserEntity>>
}

// DSL 构建数据库
val db = BrickDatabase.build<AppDatabase>(context, "app.db") {
    addMigrations(MIGRATION_1_2)
    fallbackToDestructiveMigration()
}

// 简洁的迁移 DSL
val MIGRATION_1_2 = migration(1, 2) {
    execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0")
}

// 结果包装
val result = dbResultOf { userDao.getById(1) }
result.onSuccess { user -> showUser(user) }
      .onFailure { error -> showError(error) }

// Flow 转 DbResult
userDao.observeAll().asDbResult().collect { result ->
    result.onSuccess { users -> showList(users) }
}
```

→ [brick-data/README.md](brick-data/README.md)

### brick-permission — 权限管理

基于协程的运行时权限请求，无需覆写 `onRequestPermissionsResult`：

```kotlin
// 协程方式请求（推荐）
lifecycleScope.launch {
    val result = BrickPermission.request(this@MainActivity, Manifest.permission.CAMERA)
    if (result.isAllGranted) {
        openCamera()
    } else if (result.hasPermanentlyDenied) {
        // 引导跳转设置页
        BrickPermission.openAppSettings(this@MainActivity)
    }
}

// 扩展函数方式（更简洁）
requirePermissions(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    onGranted = { startRecording() },
    onDenied = { result -> showDeniedDialog(result) }
)

// 检查权限
if (context.hasPermission(Manifest.permission.CAMERA)) { ... }
```

→ [brick-permission/README.md](brick-permission/README.md)

### brick-startup — 启动优化

四级优先级初始化，优化应用启动速度：

```kotlin
// 定义初始化器
class LogInitializer : AppInitializer {
    override val name = "BrickLogger"
    override val priority = InitPriority.IMMEDIATELY
    override fun onCreate(context: Context) {
        BrickLogger.init { debug = BuildConfig.DEBUG }
    }
}

class AnalyticsInitializer : AppInitializer {
    override val name = "Analytics"
    override val priority = InitPriority.DEFERRED  // 空闲时才执行
    override fun onCreate(context: Context) { AnalyticsSDK.init(context) }
}

// 在 Application 中启动
BrickStartup.init(this) {
    add(LogInitializer())       // IMMEDIATELY — 立即
    add(StoreInitializer())     // NORMAL — 正常
    add(ImageInitializer())     // NORMAL — 正常
    add(AnalyticsInitializer()) // DEFERRED — 空闲
    onResult { Log.d("Startup", "${it.name}: ${it.costMillis}ms") }
}
```

→ [brick-startup/README.md](brick-startup/README.md)

## 项目结构

```
Brick/
├── app/                    Demo 示例应用
├── brick-net/              网络模块（HTTP + WebSocket）
├── brick-utils/            工具扩展模块
├── brick-ui/               UI 组件模块
├── brick-image/            图片加载模块
├── brick-arch/             架构基座模块
├── brick-store/            键值存储模块（MMKV）
├── brick-log/              日志系统模块（Timber）
├── brick-data/             数据库模块（Room）
├── brick-permission/       权限管理模块
├── brick-startup/         启动优化模块
├── gradle/
│   ├── libs.versions.toml  版本目录
│   └── publish.gradle      Maven 发布脚本
└── .github/workflows/      CI/CD 配置
```

## 混淆

各模块已内置 `consumer-rules.pro`，使用方无需额外配置混淆规则。

## 文档

| 文档 | 说明 |
|------|------|
| [CHANGELOG.md](CHANGELOG.md) | 版本变更历史 |
| [FAQ.md](FAQ.md) | 常见问题解答 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 贡献指南 |

## License

```
Copyright 2024 ail36413

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
