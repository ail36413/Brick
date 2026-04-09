package com.ail.brick.arch.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 [SharedFlow] 的轻量事件总线，替代 EventBus / LiveDataBus。
 *
 * 支持普通事件和粘性事件，协程安全，配合 [repeatOnLifecycle] 自动感知生命周期。
 *
 * ### 线程安全
 * - 内部使用 [ConcurrentHashMap] 管理事件通道，[post] / [observe] 可在任意线程调用。
 * - [post] 和 [postSticky] 是 `suspend` 函数，背压时会挂起（缓冲区默认 16 条）。
 *
 * ### 生命周期要求
 * - **必须**在 `repeatOnLifecycle(STARTED)` 或等价作用域中 collect，
 *   否则 Activity/Fragment 销毁后协程不会自动取消，导致内存泄漏。
 * - 粘性事件通道持有最后一条消息引用，如需释放请调用 [removeSticky]。
 *
 * ### 发送事件
 * ```kotlin
 * viewModelScope.launch {
 *     FlowEventBus.post(LoginSuccessEvent("user123"))
 * }
 * ```
 *
 * ### 接收事件
 * ```kotlin
 * lifecycleScope.launch {
 *     repeatOnLifecycle(Lifecycle.State.STARTED) {
 *         FlowEventBus.observe<LoginSuccessEvent>().collect { event ->
 *             updateUI(event.userId)
 *         }
 *     }
 * }
 * ```
 *
 * ### 粘性事件（新订阅者自动收到最近一条）
 * ```kotlin
 * FlowEventBus.postSticky(ThemeChangedEvent(darkMode = true))
 * FlowEventBus.observeSticky<ThemeChangedEvent>().collect { ... }
 * ```
 *
 * > **注意**：事件通道按 class name 或自定义 key 隔离。
 */
object FlowEventBus {

    private val flows = ConcurrentHashMap<String, MutableSharedFlow<Any>>()
    private val stickyFlows = ConcurrentHashMap<String, MutableSharedFlow<Any>>()

    /**
     * 发送普通事件。
     *
     * @param event 事件对象
     * @param key   事件通道 key，默认为事件类名
     */
    suspend fun post(event: Any, key: String = event.javaClass.name) {
        getFlow(key).emit(event)
    }

    /**
     * 发送粘性事件（新订阅者自动收到最后一条）。
     *
     * @param event 事件对象
     * @param key   事件通道 key
     */
    suspend fun postSticky(event: Any, key: String = event.javaClass.name) {
        getStickyFlow(key).emit(event)
    }

    /** 观察普通事件流 */
    inline fun <reified T : Any> observe(key: String = T::class.java.name) =
        getFlow(key).asSharedFlow()

    /** 观察粘性事件流（新订阅者会立即收到最近一条） */
    inline fun <reified T : Any> observeSticky(key: String = T::class.java.name) =
        getStickyFlow(key).asSharedFlow()

    /** 移除指定 key 的粘性事件通道 */
    fun removeSticky(key: String) {
        stickyFlows.remove(key)
    }

    /**
     * 清除所有事件通道（普通 + 粘性）。
     *
     * **警告**：已通过 [observe] / [observeSticky] 获取引用的订阅者不受影响，
     * 后续调用 [observe] 会得到全新的 Flow 实例，与旧订阅者互相隔离。
     * 仅应在应用退出或完全重置时调用。
     */
    fun clear() {
        flows.clear()
        stickyFlows.clear()
    }

    @PublishedApi
    internal fun getFlow(key: String): MutableSharedFlow<Any> {
        return flows.computeIfAbsent(key) { MutableSharedFlow(extraBufferCapacity = 16) }
    }

    @PublishedApi
    internal fun getStickyFlow(key: String): MutableSharedFlow<Any> {
        return stickyFlows.computeIfAbsent(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 16) }
    }
}
