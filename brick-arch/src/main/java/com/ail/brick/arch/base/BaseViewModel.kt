package com.ail.brick.arch.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ail.brick.arch.config.BrickArch
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVVM 模式的 ViewModel 基类。
 *
 * 提供：
 * - **协程快捷启动** [launch]：自动捕获异常并分发给 [handleException]
 * - **通用 UI 事件** [UIEvent]：Toast / Loading / Navigate / 自定义
 *
 * ### 子类示例
 * ```kotlin
 * class HomeViewModel : BaseViewModel() {
 *     fun loadData() = launch {
 *         showLoading()
 *         val data = repository.fetch()
 *         showLoading(false)
 *         // update LiveData / StateFlow ...
 *     }
 * }
 * ```
 *
 * ### Activity 中收集事件
 * ```kotlin
 * lifecycleScope.launch {
 *     repeatOnLifecycle(Lifecycle.State.STARTED) {
 *         viewModel.uiEvent.collect { event -> onUIEvent(event) }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel(
    protected val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    /** 通用 UI 事件流（使用 Channel 确保事件不丢失，一次性消费） */
    private val _uiEvent = Channel<UIEvent>(Channel.BUFFERED)
    val uiEvent: Flow<UIEvent> = _uiEvent.receiveAsFlow()

    /**
     * 启动协程并自动处理异常。
     *
     * @param onError 自定义异常回调；为 null 时走 [handleException]
     * @param block   协程体
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

    /** 发送一次性 UI 事件 */
    protected fun sendEvent(event: UIEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }

    /** 显示 Toast */
    protected fun showToast(message: String) = sendEvent(UIEvent.Toast(message))

    /** 显示/隐藏全局 Loading */
    protected fun showLoading(show: Boolean = true) = sendEvent(UIEvent.Loading(show))

    /** 触发导航 */
    protected fun navigate(route: String, extras: Map<String, Any>? = null) =
        sendEvent(UIEvent.Navigate(route, extras))

    /** 触发返回 */
    protected fun navigateBack() = sendEvent(UIEvent.NavigateBack)

    /**
     * 在 IO 线程启动协程，适合网络请求、数据库操作等耗时任务。
     *
     * > [sendEvent]、[showToast] 等方法内部会自动切到主线程发送事件，可在任意调度器上安全调用。
     *
     * ```kotlin
     * fun loadData() = launchIO {
     *     val data = repository.fetch()   // IO 线程
     *     showToast("加载完成")             // 内部自动切回主线程
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
     *
     * ```kotlin
     * launchIO {
     *     val data = repository.fetch()
     *     withMain { showToast("完成") }  // 切回主线程
     * }
     * ```
     */
    protected suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.Main, block)

    // ── SavedStateHandle 便捷方法 ──────────────────────

    /** 从 [SavedStateHandle] 读取已保存的数据 */
    protected inline fun <reified T> getSavedState(key: String): T? =
        savedStateHandle?.get<T>(key)

    /** 向 [SavedStateHandle] 写入数据（进程恢复时可读取） */
    protected fun <T> setSavedState(key: String, value: T) {
        savedStateHandle?.set(key, value)
    }

    /** 从 [SavedStateHandle] 获取可观察的 [kotlinx.coroutines.flow.StateFlow] */
    protected fun <T> savedStateFlow(key: String, initialValue: T): kotlinx.coroutines.flow.StateFlow<T> =
        savedStateHandle?.getStateFlow(key, initialValue)
            ?: kotlinx.coroutines.flow.MutableStateFlow(initialValue)

    /**
     * 默认异常处理，子类可覆写以统一错误上报/展示逻辑。
     *
     * 默认通过 [BrickArch.logger] 记录日志并弹出 Toast。
     */
    protected open fun handleException(throwable: Throwable) {
        BrickArch.logger.e("BaseViewModel", "Unhandled exception in ${this::class.simpleName}", throwable)
        showToast(throwable.message ?: "未知错误")
    }

    /**
     * 通用 UI 事件密封类。
     *
     * | 类型 | 说明 |
     * |---|---|
     * | [Toast] | 弹出短提示 |
     * | [Loading] | 显示/隐藏 Loading |
     * | [Navigate] | 页面跳转（附带 route + extras） |
     * | [NavigateBack] | 返回上一页 |
     * | [Custom] | 自定义扩展事件 |
     */
    sealed class UIEvent {
        data class Toast(val message: String) : UIEvent()
        data class Loading(val show: Boolean) : UIEvent()
        data class Navigate(val route: String, val extras: Map<String, Any>? = null) : UIEvent()
        data object NavigateBack : UIEvent()
        data class Custom(val key: String, val data: Any? = null) : UIEvent()
    }
}
