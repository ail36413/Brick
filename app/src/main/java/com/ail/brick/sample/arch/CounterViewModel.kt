package com.ail.brick.sample.arch

import com.ail.brick.arch.mvi.MviViewModel
import com.ail.brick.arch.mvi.UiEvent
import com.ail.brick.arch.mvi.UiIntent
import com.ail.brick.arch.mvi.UiState
import com.ail.brick.arch.state.LoadState
import com.ail.brick.arch.state.retryLoadState
import kotlinx.coroutines.delay

/** 计数器状态 */
data class CounterState(
    val count: Int = 0,
    /** LoadState 演示：异步加载的数据 */
    val dataState: LoadState<String> = LoadState.Success("点击按钮开始加载")
) : UiState

/** 计数器一次性事件 */
sealed class CounterEvent : UiEvent {
    data class ShowToast(val message: String) : CounterEvent()
}

/** 计数器意图 */
sealed class CounterIntent : UiIntent {
    data object Increment : CounterIntent()
    data object Decrement : CounterIntent()
    data object Reset : CounterIntent()
    /** 模拟异步加载数据（成功） */
    data object LoadData : CounterIntent()
    /** 模拟异步加载数据（失败） */
    data object LoadError : CounterIntent()
    /** 模拟异步加载 + 自动重试 */
    data object RetryLoad : CounterIntent()
}

/**
 * MVI 计数器 ViewModel，演示：
 * - 基本 MVI 数据流（Increment / Decrement / Reset）
 * - [LoadState] 状态管理（Loading / Success / Error）
 * - [launchIO] 协程调度器便捷方法
 * - [retryLoadState] 自动重试 + 指数退避
 * - [dispatchThrottled] 意图节流
 */
class CounterViewModel : MviViewModel<CounterState, CounterEvent, CounterIntent>(CounterState()) {
    override fun handleIntent(intent: CounterIntent) {
        when (intent) {
            CounterIntent.Increment -> updateState { copy(count = count + 1) }
            CounterIntent.Decrement -> updateState { copy(count = count - 1) }
            CounterIntent.Reset -> {
                updateState { copy(count = 0) }
                sendEvent(CounterEvent.ShowToast("已重置"))
            }
            CounterIntent.LoadData -> loadData()
            CounterIntent.LoadError -> loadError()
            CounterIntent.RetryLoad -> retryLoad()
        }
    }

    private fun loadData() {
        updateState { copy(dataState = LoadState.Loading) }
        launchIO {
            delay(1500) // 模拟网络请求
            updateState { copy(dataState = LoadState.Success("从服务器加载的数据 (count=$count)")) }
        }
    }

    private fun loadError() {
        updateState { copy(dataState = LoadState.Loading) }
        launchIO {
            delay(1000) // 模拟网络请求
            updateState { copy(dataState = LoadState.Error(RuntimeException("网络连接超时"))) }
        }
    }

    /** 演示 retryLoadState：自动重试 3 次，指数退避 */
    private fun retryLoad() {
        updateState { copy(dataState = LoadState.Loading) }
        launchIO {
            var attempt = 0
            val result = retryLoadState(times = 2, initialDelayMillis = 500) {
                attempt++
                delay(300) // 模拟网络耗时
                if (attempt < 3) throw RuntimeException("第 $attempt 次失败")
                "重试成功！共尝试 $attempt 次"
            }
            updateState { copy(dataState = result) }
        }
    }
}
