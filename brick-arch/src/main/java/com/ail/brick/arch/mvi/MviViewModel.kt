package com.ail.brick.arch.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ail.brick.arch.config.BrickArch
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * MVI ViewModel 基类
 *
 * 驱动单向数据流：Intent → reduce → State，同时支持一次性 Event。
 *
 * ```kotlin
 * class HomeViewModel : MviViewModel<HomeState, HomeEvent, HomeIntent>(HomeState()) {
 *     override fun handleIntent(intent: HomeIntent) {
 *         when (intent) {
 *             HomeIntent.Refresh -> {
 *                 updateState { copy(isLoading = true) }
 *                 launch {
 *                     val items = repo.load()
 *                     updateState { copy(isLoading = false, items = items) }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param S 页面状态
 * @param E 一次性事件
 * @param I 用户意图
 */
abstract class MviViewModel<S : UiState, E : UiEvent, I : UiIntent>(
    initialState: S,
    protected val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /** 当前页面状态 */
    val state: StateFlow<S> = _state.asStateFlow()

    /** 当前状态快照 */
    protected val currentState: S get() = _state.value

    private val _event = Channel<E>(Channel.BUFFERED)

    /** 一次性事件流（配合 Lifecycle 收集） */
    val event = _event.receiveAsFlow()

    /**
     * 处理用户意图（子类实现）
     */
    abstract fun handleIntent(intent: I)

    /**
     * 分发意图（View 层调用）
     */
    fun dispatch(intent: I) {
        handleIntent(intent)
    }

    private val intentThrottleMap = ConcurrentHashMap<String, Long>()

    /**
     * 带节流的意图分发。相同类型的 Intent 在 [windowMillis] 内只处理第一次。
     *
     * 适用于按钮绑定等场景，防止重复触发副作用（如重复提交）。
     *
     * ```kotlin
     * binding.btnSubmit.setOnClickListener {
     *     viewModel.dispatchThrottled(SubmitIntent)
     * }
     * ```
     *
     * @param intent 用户意图
     * @param windowMillis 节流窗口（毫秒），默认 300ms
     */
    fun dispatchThrottled(intent: I, windowMillis: Long = 300) {
        val key = intent::class.java.name
        val now = System.nanoTime() / 1_000_000L
        val last = intentThrottleMap[key] ?: 0L
        if (now - last >= windowMillis) {
            intentThrottleMap[key] = now
            handleIntent(intent)
        }
    }

    /**
     * 更新状态（使用 reduce 函数，线程安全的原子操作）
     */
    protected fun updateState(reduce: S.() -> S) {
        _state.update { it.reduce() }
    }

    /**
     * 发送一次性事件
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch { _event.send(event) }
    }

    /**
     * 带异常处理的协程启动
     */
    protected fun launch(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            onError?.invoke(throwable) ?: handleException(throwable)
        }
        viewModelScope.launch(handler, block = block)
    }

    /**
     * 在 IO 线程启动协程，适合网络请求、数据库操作等耗时任务。
     *
     * > [updateState] 是线程安全的，可以在任意调度器上直接调用，无需 `withMain`。
     *
     * ```kotlin
     * fun loadList() = launchIO {
     *     val data = repository.fetchList()   // IO 线程
     *     updateState { copy(items = data) }  // 线程安全，无需切线程
     * }
     * ```
     *
     * @param onError 自定义异常回调；为 null 时走 [handleException]
     * @param block   协程体（运行在 [Dispatchers.IO]）
     */
    protected fun launchIO(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            onError?.invoke(throwable) ?: handleException(throwable)
        }
        viewModelScope.launch(Dispatchers.IO + handler, block = block)
    }

    /**
     * 在 Default 线程启动协程，适合 CPU 密集型计算。
     *
     * @param onError 自定义异常回调；为 null 时走 [handleException]
     * @param block   协程体（运行在 [Dispatchers.Default]）
     */
    protected fun launchDefault(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val handler = CoroutineExceptionHandler { _, throwable ->
            onError?.invoke(throwable) ?: handleException(throwable)
        }
        viewModelScope.launch(Dispatchers.Default + handler, block = block)
    }

    /**
     * 切换到主线程执行（在 [launchIO] / [launchDefault] 内部使用）。
     */
    protected suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.Main, block)

    // ── SavedStateHandle 便捷方法 ──────────────────────

    /**
     * 从 [SavedStateHandle] 读取已保存的数据。
     *
     * 构造函数需传入 `savedStateHandle` 后才可使用，否则返回 null。
     */
    protected inline fun <reified T> getSavedState(key: String): T? =
        savedStateHandle?.get<T>(key)

    /**
     * 向 [SavedStateHandle] 写入数据（进程恢复时可读取）。
     */
    protected fun <T> setSavedState(key: String, value: T) {
        savedStateHandle?.set(key, value)
    }

    /**
     * 从 [SavedStateHandle] 获取可观察的 [StateFlow]。
     *
     * @param key 存储键
     * @param initialValue 默认值
     */
    protected fun <T> savedStateFlow(key: String, initialValue: T): StateFlow<T> =
        savedStateHandle?.getStateFlow(key, initialValue)
            ?: MutableStateFlow(initialValue)

    /**
     * 默认异常处理（子类可覆写）。
     *
     * 默认实现通过 [BrickArch.logger] 打印错误日志，子类应覆写此方法以实现自定义错误处理，
     * 例如 `updateState { copy(error = throwable.message) }`。
     */
    protected open fun handleException(throwable: Throwable) {
        BrickArch.logger.e("MviViewModel", "Unhandled exception in ${this::class.simpleName}", throwable)
    }
}
