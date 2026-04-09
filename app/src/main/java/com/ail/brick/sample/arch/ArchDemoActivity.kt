package com.ail.brick.sample.arch

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ail.brick.arch.event.FlowEventBus
import com.ail.brick.arch.ext.collectOnLifecycle
import com.ail.brick.arch.ext.observeEvent
import com.ail.brick.arch.ext.select
import com.ail.brick.arch.ext.throttleClicks
import com.ail.brick.arch.mvi.MviActivity
import com.ail.brick.arch.state.LoadState
import com.ail.brick.arch.state.fold
import com.ail.brick.sample.databinding.ActivityArchDemoBinding
import kotlinx.coroutines.launch

/**
 * brick-arch 演示页面
 *
 * 展示功能：
 * - MVI 单向数据流（计数器）
 * - [LoadState] 状态管理（Loading / Success / Error + fold 渲染）
 * - [launchIO] 协程调度器（模拟异步加载）
 * - [retryLoadState][com.ail.brick.arch.state.retryLoadState] 自动重试 + 指数退避
 * - [select] 局部渲染（仅 count 变化时更新标题）
 * - [dispatchThrottled] Intent 节流
 * - [throttleClicks] 节流点击
 * - [FlowEventBus] 事件总线
 */
class ArchDemoActivity : MviActivity<
        ActivityArchDemoBinding,
        CounterState,
        CounterEvent,
        CounterIntent,
        CounterViewModel>() {

    override fun viewModelClass() = CounterViewModel::class.java

    private var eventLog = StringBuilder("事件日志\n")
    private var throttleCount = 0

    override fun inflateBinding(inflater: LayoutInflater) =
        ActivityArchDemoBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // MVI 计数器（使用 dispatchThrottled 防止重复点击）
        binding.btnIncrement.setOnClickListener { dispatchThrottled(CounterIntent.Increment) }
        binding.btnDecrement.setOnClickListener { dispatchThrottled(CounterIntent.Decrement) }
        binding.btnReset.setOnClickListener { dispatch(CounterIntent.Reset) }

        // LoadState 演示
        binding.btnLoadData.setOnClickListener { dispatch(CounterIntent.LoadData) }
        binding.btnLoadError.setOnClickListener { dispatch(CounterIntent.LoadError) }
        binding.btnRetryLoad.setOnClickListener { dispatch(CounterIntent.RetryLoad) }

        // select 局部渲染 — 仅 count 变化时更新标题栏
        viewModel.state.select { it.count }.collectOnLifecycle(this) { count ->
            binding.toolbar.subtitle = "当前计数: $count"
        }

        // 节流点击演示 — throttleClicks 500ms 内只响应一次
        binding.btnThrottle.throttleClicks(500).collectOnLifecycle(this) {
            throttleCount++
            binding.tvThrottleCount.text = "实际触发次数: $throttleCount"
        }

        // FlowEventBus 演示 — 使用推荐的生命周期安全 API
        binding.btnSendEvent.setOnClickListener {
            lifecycleScope.launch {
                FlowEventBus.post(SampleBusEvent(msg = "Hello from EventBus @ ${System.currentTimeMillis()}"))
            }
        }

        observeEvent<SampleBusEvent> { event ->
            eventLog.append("收到: ${event.msg}\n")
            binding.tvEventLog.text = eventLog.toString()
        }
    }

    override fun render(state: CounterState) {
        binding.tvCount.text = state.count.toString()

        // 使用 LoadState.fold 渲染数据状态
        binding.tvLoadState.text = state.dataState.fold(
            onLoading = { "状态: ⏳ 加载中..." },
            onSuccess = { "状态: ✅ $it" },
            onError = { "状态: ❌ ${it.message}" }
        )
    }

    override fun handleEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    /** EventBus 演示事件 */
    data class SampleBusEvent(val msg: String)
}
