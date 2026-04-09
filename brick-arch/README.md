# brick-arch

[![](https://jitpack.io/v/ail36413/Brick.svg)](https://jitpack.io/#ail36413/Brick)

Android 架构基础库，提供 MVVM + MVI 双模式支持，基于 AndroidX ViewModel / Lifecycle / Flow 构建。

## 引入

```kotlin
val brickVersion = "1.0.0"
implementation("com.github.ail36413.Brick:brick-arch:$brickVersion")
```

请将版本号替换为 JitPack Release 页面的最新版本。

## 模块结构

```
brick-arch
├── base/       BaseActivity / BaseFragment / BaseDialogFragment / BaseViewModel
├── config/     BrickArch — 全局配置（可插拔日志等）
├── mvvm/       MvvmActivity / MvvmFragment / MvvmDialogFragment / MvvmBottomSheetDialogFragment
├── mvi/        MviViewModel / MviActivity / MviFragment / MviDialogFragment / MviBottomSheetDialogFragment
├── nav/        BrickNav — 纯代码路由 Fragment 导航（零 XML）
├── state/      LoadState — 通用加载状态（Loading / Success / Error）+ 重试支持
├── event/      FlowEventBus — SharedFlow 事件总线
├── test/       BrickTestRule — ViewModel 单元测试工具
└── ext/        ViewBinding 委托 / Lifecycle 扩展 / ViewModel 扩展 / Flow 操作符
```

## BrickNav — 纯代码路由导航

轻量级 Fragment 导航管理器，**零 XML 导航图**。告别复杂的 navigation graph，用纯代码注册路由、导航、管理返回栈。

### 初始化

```kotlin
class HomeActivity : AppCompatActivity() {
    private lateinit var nav: BrickNav

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        nav = BrickNav.init(this, R.id.container)
            .register<HomeFragment>("home")
            .register<ProfileFragment>("profile")
            .register<SettingsFragment>("settings")
            .addInterceptor { from, to, args ->
                Log.d("Nav", "$from → $to")
                true // 返回 false 拦截导航
            }

        if (savedInstanceState == null) {
            nav.navigate("home") { addToBackStack = false }
        }
    }
}
```

### 导航操作

```kotlin
// 基本导航（默认水平滑动动画 + 加入返回栈）
nav.navigate("profile")

// 带参数
nav.navigate("profile", bundleOf("userId" to 123))

// 指定动画
nav.navigate("profile") { anim = NavAnim.FADE }
nav.navigate("settings") { anim = NavAnim.SLIDE_VERTICAL }
nav.navigate("settings") { anim = NavAnim.NONE }

// SingleTop — 栈顶去重
nav.navigate("profile") { singleTop = true }

// 自定义动画资源
nav.navigate("profile") {
    setCustomAnim(
        enter = R.anim.my_enter,
        exit = R.anim.my_exit,
        popEnter = R.anim.my_pop_enter,
        popExit = R.anim.my_pop_exit,
    )
}
```

### 返回栈管理

```kotlin
nav.back()                          // 返回上一页
nav.backTo("profile")               // 弹出到指定路由
nav.backTo("profile", inclusive = true) // 连同目标一起弹出
nav.clearStack()                    // 清空整个返回栈
nav.currentRoute                    // 当前路由名
nav.stackDepth                      // 返回栈深度
```

### 从 Fragment 中导航

```kotlin
class ProfileFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        btnSettings.setOnClickListener {
            BrickNav.from(this).navigate("settings")
        }
        btnBack.setOnClickListener {
            BrickNav.from(this).back()
        }
    }
}
```

### 内置动画预设

| NavAnim | 效果 | 场景 |
|---------|------|------|
| `SLIDE_HORIZONTAL` | 左右推入推出（默认） | 常规页面导航 |
| `SLIDE_VERTICAL` | 底部弹出 / 下拉关闭 | 模态页面 |
| `FADE` | 淡入淡出 | 无方向感的切换 |
| `NONE` | 无动画 | 即时切换 |

## 架构选型

| | MVVM | MVI |
|---|------|-----|
| 数据流 | ViewModel → LiveData/Flow → View | Intent → reduce → State → View |
| 状态管理 | 多个可变字段 | 单一不可变 State |
| 适用场景 | 常规 CRUD 页面 | 复杂交互、多状态联动 |
| 基类 | `MvvmActivity/Fragment` | `MviActivity/Fragment` |

## MVVM 模式

子类通过 `viewModelClass()` 提供 ViewModel 类型，基类自动创建实例，无需手动 `ViewModelProvider`。

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

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnLoad.setOnClickListener { viewModel.loadData() }
    }

    override fun initObservers() {
        viewModel.items.observe(this) { adapter.submitList(it) }
    }

    override fun onLoading(show: Boolean) {
        if (show) LoadingDialog.show(this) else LoadingDialog.dismiss()
    }
}
```

## MVI 模式

**单向数据流**: `dispatch(Intent)` → `handleIntent()` → `updateState {}` → `render(state)`

```kotlin
// 1. 定义契约
data class ListState(
    val isLoading: Boolean = false,
    val items: List<String> = emptyList(),
    val error: String? = null
) : UiState

sealed class ListEvent : UiEvent {
    data class ShowSnackbar(val msg: String) : ListEvent()
}

sealed class ListIntent : UiIntent {
    data object Refresh : ListIntent()
    data class Delete(val index: Int) : ListIntent()
}

// 2. ViewModel
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
            is ListIntent.Delete -> {
                val newList = currentState.items.toMutableList().apply { removeAt(intent.index) }
                updateState { copy(items = newList) }
                sendEvent(ListEvent.ShowSnackbar("已删除"))
            }
        }
    }
}

// 3. Activity
class ListActivity : MviActivity<ActivityListBinding, ListState, ListEvent, ListIntent, ListViewModel>() {
    override fun viewModelClass() = ListViewModel::class.java

    override fun inflateBinding(inflater: LayoutInflater) = ActivityListBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        dispatch(ListIntent.Refresh)
    }

    override fun render(state: ListState) {
        binding.progressBar.isVisible = state.isLoading
        adapter.submitList(state.items)
    }

    override fun handleEvent(event: ListEvent) {
        when (event) {
            is ListEvent.ShowSnackbar -> Snackbar.make(binding.root, event.msg, Snackbar.LENGTH_SHORT).show()
        }
    }
}
```

## FlowEventBus

基于 `SharedFlow` 的轻量事件总线，替代 EventBus / LiveDataBus。支持普通事件与粘性事件。

```kotlin
// 发送
viewModelScope.launch {
    FlowEventBus.post(LoginSuccessEvent(userId))
}

// 接收 — lifecycle 安全（推荐方式）
observeEvent<LoginSuccessEvent> { event ->
    refreshUserInfo(event.userId)
}

// 粘性事件 — lifecycle 安全
observeStickyEvent<ThemeChangedEvent> { event ->
    applyTheme(event.dark)
}

// 也可手动管理（不推荐，需自行处理 lifecycle）
launchOnStarted {
    FlowEventBus.observe<LoginSuccessEvent>().collect { event ->
        refreshUserInfo(event.userId)
    }
}

// 粘性事件 — 新订阅者立即收到最后一次值
FlowEventBus.postSticky(ThemeChangedEvent(dark = true))
```

> 事件通道按类全限定名隔离，不同类型互不干扰。可选 `key` 参数实现同类型多通道。

## 扩展工具

### ViewBinding 委托

零反射、自动释放（Fragment `onDestroyView` 时置空）。

```kotlin
// Activity — LazyThreadSafetyMode.NONE（单线程安全）
class HomeActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityHomeBinding::inflate)
}

// Fragment — onDestroyView 时自动置空，防止泄漏
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val binding by viewBinding(FragmentHomeBinding::bind)
}
```

### Lifecycle 安全收集

```kotlin
// 安全收集 Flow — 生命周期低于 minState 时自动暂停
viewModel.stateFlow.collectOnLifecycle(viewLifecycleOwner) { state -> render(state) }

// 快捷启动
launchOnStarted { /* repeatOnLifecycle(STARTED) */ }
launchOnResumed { /* repeatOnLifecycle(RESUMED) */ }
```

### ViewModel 快捷获取

```kotlin
// Fragment/Activity scope
val vm = getViewModel<HomeViewModel>()

// Activity scope 共享 — Fragment 间共享同一实例
val sharedVm = getActivityViewModel<SharedViewModel>()

// 自定义 Factory
val vm = getViewModel<HomeViewModel>(HomeViewModelFactory(repo))
```

### Flow 操作符

```kotlin
// 节流 — 500ms 内只发射第一个元素，适合按钮防重复点击
viewModel.state
    .throttleFirst(500)
    .collectOnLifecycle(this) { render(it) }

// 防抖 — 300ms 内无新元素才发射，适合搜索输入
searchFlow
    .debounceAction(300)
    .collectOnLifecycle(this) { query -> viewModel.search(query) }

// View 点击节流 — 直接将点击事件转为 Flow
binding.btnSubmit.throttleClicks(1000)
    .collectOnLifecycle(this) { viewModel.submit() }

// 局部渲染 — 仅在指定字段变化时才触发收集，避免无关字段变更引起的重绘
viewModel.state.select { it.count }
    .collectOnLifecycle(this) { count -> binding.tvCount.text = count.toString() }
```

## LoadState 状态管理

通用加载状态密封类，三种状态：`Loading`、`Success<T>`、`Error`。

```kotlin
// 在 MVI State 中使用
data class HomeState(
    val items: LoadState<List<String>> = LoadState.Loading
) : UiState

// 在 ViewModel 中
updateState { copy(items = LoadState.Loading) }
launchIO {
    val data = repository.fetchItems()
    updateState { copy(items = LoadState.Success(data)) }
}

// 在 UI 中渲染 — fold 三态匹配
binding.tvStatus.text = state.items.fold(
    onLoading = { "加载中..." },
    onSuccess = { "共 ${it.size} 条数据" },
    onError   = { "错误: ${it.message}" }
)

// 操作符
val names = state.items.map { it.map { item -> item.name } }  // 转换
val safeData = state.items.getOrNull()                          // 安全取值
val fallback = state.items.getOrDefault(emptyList())            // 默认值

// 链式回调
state.items
    .onLoading { showProgressBar() }
    .onSuccess { adapter.submitList(it) }
    .onError   { showErrorView(it) }

// 在协程中快速包装
val state = loadStateCatching { repository.fetchItems() }

// 带重试的加载（指数退避）3 次重试，初始延迟 1s，递增因子 2x
val state = retryLoadState(times = 3, initialDelayMillis = 1000) {
    repository.fetchItems()
}
```

## 协程调度器

`BaseViewModel` 和 `MviViewModel` 均提供便捷的协程启动方法：

| 方法 | 调度器 | 适用场景 |
|------|--------|----------|
| `launch {}` | Main | UI 操作、状态更新 |
| `launchIO {}` | IO | 网络请求、数据库、文件读写 |
| `launchDefault {}` | Default | CPU 密集型计算 |
| `withMain {}` | Main | 在 IO/Default 协程中切回主线程 |

> `updateState {}` 是线程安全的原子操作，在任意调度器上均可直接调用，无需 `withMain`。

### SavedStateHandle 进程恢复

`BaseViewModel` 和 `MviViewModel` 均支持可选的 `SavedStateHandle`，系统杀死进程后可恢复关键数据：

```kotlin
class DetailViewModel(
    savedStateHandle: SavedStateHandle
) : MviViewModel<DetailState, DetailEvent, DetailIntent>(DetailState(), savedStateHandle) {

    // 读取已保存的数据
    private val userId: String? = getSavedState("userId")

    // 保存数据（进程被杀后可恢复）
    fun onPageSelected(page: Int) {
        setSavedState("currentPage", page)
    }

    // 获取可观察的 StateFlow
    val scrollPosition = savedStateFlow("scrollY", 0)
}
```

> 构造函数接受 `SavedStateHandle` 的 ViewModel 会由 `ViewModelProvider` 默认的 `SavedStateViewModelFactory` 自动注入。

### Intent 节流分发

`dispatchThrottled` 在指定时间窗口内对相同类型的 Intent 去重，防止重复提交：

```kotlin
// 在 Activity / Fragment 中使用
binding.btnSubmit.setOnClickListener {
    dispatchThrottled(SubmitIntent.Confirm)  // 300ms 内只处理第一次
}

// 自定义窗口
dispatchThrottled(intent, windowMillis = 500)
```

```kotlin
class HomeViewModel : MviViewModel<...>(...) {
    fun loadData() {
        updateState { copy(isLoading = true) }
        launchIO {
            val data = repository.fetch()          // IO 线程
            updateState { copy(isLoading = false, data = data) }  // 线程安全，无需切线程
        }
    }

    fun processData() = launchDefault {
        val result = heavyComputation(data)        // Default 线程
        withMain { showToast("处理完成") }          // 显式切回主线程（仅 UI 操作需要）
    }
}
```

## 基类生命周期

```
BaseActivity.onCreate
  ├── inflateBinding()      → ViewBinding 创建
  ├── onPreInit()           → ViewModel 初始化（MvvmActivity/MviActivity）
  ├── initView()            → 初始化视图
  └── initObservers()       → 订阅数据

BaseFragment.onViewCreated
  ├── initView()
  ├── initObservers()
  └── onLazyLoad()          → 首次可见时触发（仅一次，屏幕旋转不会重复触发）
```

> Binding 访问安全：在 `onCreate` 之前或 `onDestroy` 之后访问 `binding` 会抛出 `IllegalStateException`（而非 NPE）。

## BaseDialogFragment

支持位置和宽度配置的 DialogFragment 基类。另提供带 ViewModel 自动绑定的变体：

| 基类 | ViewModel 模式 | 用途 |
|------|----------------|------|
| `BaseDialogFragment` | 无 | 纯 UI 弹窗 |
| `MvvmDialogFragment` | MVVM | 带业务逻辑的弹窗 |
| `MviDialogFragment` | MVI | 复杂状态弹窗 |
| `BaseBottomSheetDialogFragment` | 无 | 纯 UI 底部弹窗 |
| `MvvmBottomSheetDialogFragment` | MVVM | 带业务逻辑的底部弹窗 |
| `MviBottomSheetDialogFragment` | MVI | 复杂状态底部弹窗 |

```kotlin
// MVVM 弹窗
class ConfirmDialog : MvvmDialogFragment<DialogConfirmBinding, ConfirmViewModel>() {
    override fun viewModelClass() = ConfirmViewModel::class.java
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogConfirmBinding.inflate(inflater, container, false)
    override fun initView() {
        binding.btnOk.setOnClickListener { viewModel.confirm() }
    }
}

// MVI 底部弹窗
class FilterSheet : MviBottomSheetDialogFragment<
    DialogFilterBinding, FilterState, FilterEvent, FilterIntent, FilterViewModel
>() {
    override fun viewModelClass() = FilterViewModel::class.java
    override val peekHeight = 400
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogFilterBinding.inflate(inflater, container, false)
    override fun initView() { /* ... */ }
    override fun render(state: FilterState) { /* ... */ }
    override fun handleEvent(event: FilterEvent) { /* ... */ }
}

```kotlin
// 居中弹窗（默认）
class ConfirmDialog : BaseDialogFragment<DialogConfirmBinding>() {
    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogConfirmBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.btnOk.setOnClickListener { dismiss() }
    }
}

// 底部全宽弹窗
class BottomSheet : BaseDialogFragment<DialogSheetBinding>() {
    override val dialogGravity = Gravity.BOTTOM
    override val fullWidth = true

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        DialogSheetBinding.inflate(inflater, container, false)

    override fun initView() { /* ... */ }
}
```

## 最佳实践

1. **MVVM vs MVI**：简单 CRUD 用 MVVM，复杂交互/多状态联动用 MVI。团队不熟悉函数式编程时优先选 MVVM
2. **FlowEventBus 使用生命周期安全 API**：优先使用 `observeEvent<T>{}` / `observeStickyEvent<T>{}`，不要直接在 `lifecycleScope.launch` 中 collect
3. **单向数据流**：MVI 模式下，所有状态变更都应通过 `dispatch(Intent)` 触发，避免直接修改 State
4. **一次性事件**：导航、Toast、Snackbar 等一次性 UI 事件用 `UiEvent + sendEvent()`，不要放在 `UiState` 中
5. **ViewBinding 委托**：Fragment 中使用 `viewBinding()` 委托可自动在 `onDestroyView` 时清理引用，无需手动置空

## BrickArch 全局配置

可在 `Application.onCreate()` 中替换默认日志实现（如 Timber）：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        BrickArch.init {
            logger = object : BrickLogger {
                override fun d(tag: String, message: String) = Timber.tag(tag).d(message)
                override fun w(tag: String, message: String, throwable: Throwable?) = Timber.tag(tag).w(throwable, message)
                override fun e(tag: String, message: String, throwable: Throwable?) = Timber.tag(tag).e(throwable, message)
            }
        }
    }
}
```

## BrickTestRule 测试工具

消除 ViewModel 单元测试中重复的 `Dispatchers.setMain/resetMain` 样板代码：

```kotlin
class MyViewModelTest {
    @get:Rule
    val brickTestRule = BrickTestRule()

    @Test
    fun `test state update`() = runTest {
        val vm = MyViewModel()
        vm.dispatch(MyIntent.Load)
        advanceUntilIdle()
        assertEquals(expected, vm.state.value)
    }
}
```

> 需在测试依赖中添加：`testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")`

## 常见问题

详见 [FAQ.md](../FAQ.md#brick-arch)。

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
