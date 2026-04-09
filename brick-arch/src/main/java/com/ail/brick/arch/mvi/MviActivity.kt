package com.ail.brick.arch.mvi

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseActivity
import kotlinx.coroutines.launch

/**
 * MVI Activity 基类。
 *
 * 子类通过 [viewModelClass] 提供 ViewModel 类型，基类自动创建并在 STARTED 状态收集
 * [MviViewModel.state] 和 [MviViewModel.event]，子类只需实现 [render] 和 [handleEvent]。
 *
 * ```kotlin
 * class CounterActivity : MviActivity<
 *     ActivityCounterBinding, CounterState, CounterEvent, CounterIntent, CounterViewModel
 * >() {
 *     override fun viewModelClass() = CounterViewModel::class.java
 *
 *     override fun inflateBinding(inflater: LayoutInflater) =
 *         ActivityCounterBinding.inflate(inflater)
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.btnAdd.setOnClickListener { dispatch(CounterIntent.Increment) }
 *     }
 *
 *     override fun render(state: CounterState) {
 *         binding.tvCount.text = state.count.toString()
 *     }
 *
 *     override fun handleEvent(event: CounterEvent) { /* one-shot events */ }
 * }
 * ```
 *
 * @param VB ViewBinding
 * @param S  页面状态
 * @param E  一次性事件
 * @param I  用户意图
 * @param VM MviViewModel
 */
abstract class MviActivity<VB : ViewBinding, S : UiState, E : UiEvent, I : UiIntent, VM : MviViewModel<S, E, I>>
    : BaseActivity<VB>() {

    protected lateinit var viewModel: VM
        private set

    /** 返回 ViewModel 的 Class 对象，子类必须实现 */
    abstract fun viewModelClass(): Class<VM>

    override fun onPreInit(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass()]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectState()
        collectEvent()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun collectEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { handleEvent(it) }
            }
        }
    }

    /** 渲染 UI 状态（每次 State 变更都会触发） */
    abstract fun render(state: S)

    /** 处理一次性事件（如 Snackbar、导航） */
    abstract fun handleEvent(event: E)

    /** 快捷分发 Intent 到 ViewModel */
    protected fun dispatch(intent: I) = viewModel.dispatch(intent)

    /** 带节流的 Intent 分发 */
    protected fun dispatchThrottled(intent: I, windowMillis: Long = 300) =
        viewModel.dispatchThrottled(intent, windowMillis)
}
