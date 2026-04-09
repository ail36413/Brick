package com.ail.brick.arch.ext

import android.view.View
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Flow 节流操作符：在 [windowMillis] 时间窗口内只发射第一个元素。
 *
 * 适用于按钮点击等场景，防止重复触发。
 *
 * ```kotlin
 * viewModel.state
 *     .throttleFirst(500)
 *     .collectOnLifecycle(this) { render(it) }
 * ```
 *
 * @param windowMillis 节流窗口（毫秒），默认 500ms
 */
fun <T> Flow<T>.throttleFirst(windowMillis: Long = 500): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= windowMillis) {
            lastEmitTime = now
            emit(value)
        }
    }
}

/**
 * Flow 防抖操作符：在 [timeoutMillis] 内没有新元素时才发射最后一个元素。
 *
 * 适用于搜索输入等场景。
 *
 * ```kotlin
 * searchFlow
 *     .debounceAction(300)
 *     .collectOnLifecycle(this) { query -> viewModel.search(query) }
 * ```
 *
 * @param timeoutMillis 防抖超时（毫秒），默认 300ms
 */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounceAction(timeoutMillis: Long = 300): Flow<T> = debounce(timeoutMillis)

/**
 * View 点击事件转为 Flow，内置防抖功能。
 *
 * 在 [intervalMillis] 内的重复点击会被忽略，有效防止重复提交。
 *
 * ```kotlin
 * binding.btnSubmit.throttleClicks(1000)
 *     .collectOnLifecycle(this) { viewModel.submit() }
 * ```
 *
 * @param intervalMillis 最小点击间隔（毫秒），默认 500ms
 */
fun View.throttleClicks(intervalMillis: Long = 500): Flow<Unit> = callbackFlow {
    var lastClickTime = 0L
    setOnClickListener {
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= intervalMillis) {
            lastClickTime = now
            trySend(Unit)
        }
    }
    awaitClose { setOnClickListener(null) }
}

/**
 * 选择 Flow 的子字段并去重，仅当选中的字段变化时才发射。
 *
 * 避免 State 中无关字段变更触发不必要的 UI 重绘。
 *
 * ```kotlin
 * // 仅在 count 变化时更新 UI
 * viewModel.state.select { it.count }
 *     .collectOnLifecycle(this) { count ->
 *         binding.tvCount.text = count.toString()
 *     }
 * ```
 *
 * @param selector 子字段提取函数
 */
fun <T, R> Flow<T>.select(selector: (T) -> R): Flow<R> =
    map(selector).distinctUntilChanged()
