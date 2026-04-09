package com.ail.brick.arch.mvi

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseBottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * MVI BottomSheetDialogFragment 基类。
 *
 * 自动创建 ViewModel 并收集 State / Event，继承 [BaseBottomSheetDialogFragment] 的所有配置能力。
 *
 * ```kotlin
 * class FilterBottomSheet : MviBottomSheetDialogFragment<
 *     DialogFilterBinding, FilterState, FilterEvent, FilterIntent, FilterViewModel
 * >() {
 *     override fun viewModelClass() = FilterViewModel::class.java
 *     override val peekHeight = 400
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         DialogFilterBinding.inflate(inflater, container, false)
 *     override fun initView() { /* ... */ }
 *     override fun render(state: FilterState) { /* ... */ }
 *     override fun handleEvent(event: FilterEvent) { /* ... */ }
 * }
 * ```
 */
abstract class MviBottomSheetDialogFragment<VB : ViewBinding, S : UiState, E : UiEvent, I : UiIntent, VM : MviViewModel<S, E, I>>
    : BaseBottomSheetDialogFragment<VB>() {

    protected lateinit var viewModel: VM
        private set

    /** 返回 ViewModel 的 Class 对象 */
    abstract fun viewModelClass(): Class<VM>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass()]
        super.onViewCreated(view, savedInstanceState)
        collectState()
        collectEvent()
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun collectEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { handleEvent(it) }
            }
        }
    }

    /** 渲染 UI 状态 */
    abstract fun render(state: S)

    /** 处理一次性事件 */
    abstract fun handleEvent(event: E)

    /** 分发 Intent */
    protected fun dispatch(intent: I) = viewModel.dispatch(intent)

    /** 带节流的 Intent 分发 */
    protected fun dispatchThrottled(intent: I, windowMillis: Long = 300) =
        viewModel.dispatchThrottled(intent, windowMillis)
}
