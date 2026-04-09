package com.ail.brick.arch.mvi

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseDialogFragment
import kotlinx.coroutines.launch

/**
 * MVI DialogFragment 基类。
 *
 * 自动创建 ViewModel 并收集 State / Event，子类实现 [render] 和 [handleEvent]。
 *
 * ```kotlin
 * class EditDialog : MviDialogFragment<
 *     DialogEditBinding, EditState, EditEvent, EditIntent, EditViewModel
 * >() {
 *     override fun viewModelClass() = EditViewModel::class.java
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         DialogEditBinding.inflate(inflater, container, false)
 *     override fun initView() {
 *         binding.btnSave.setOnClickListener { dispatch(EditIntent.Save) }
 *     }
 *     override fun render(state: EditState) { /* ... */ }
 *     override fun handleEvent(event: EditEvent) { /* ... */ }
 * }
 * ```
 */
abstract class MviDialogFragment<VB : ViewBinding, S : UiState, E : UiEvent, I : UiIntent, VM : MviViewModel<S, E, I>>
    : BaseDialogFragment<VB>() {

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
