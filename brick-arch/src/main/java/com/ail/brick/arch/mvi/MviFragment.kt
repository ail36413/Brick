package com.ail.brick.arch.mvi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseFragment
import kotlinx.coroutines.launch

/**
 * MVI Fragment 基类。
 *
 * 子类通过 [viewModelClass] 提供 ViewModel 类型，基类自动创建并收集 State / Event。
 * 设置 [shareViewModelWithActivity] = true 可与宿主 Activity 共享 ViewModel。
 *
 * @see MviActivity
 */
abstract class MviFragment<VB : ViewBinding, S : UiState, E : UiEvent, I : UiIntent, VM : MviViewModel<S, E, I>>
    : BaseFragment<VB>() {

    protected lateinit var viewModel: VM
        private set

    /** 返回 ViewModel 的 Class 对象，子类必须实现 */
    abstract fun viewModelClass(): Class<VM>

    /**
     * 是否与宿主 Activity 共享 ViewModel（默认 false = Fragment 级别）
     */
    open val shareViewModelWithActivity: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = if (shareViewModelWithActivity) {
            ViewModelProvider(requireActivity())[viewModelClass()]
        } else {
            ViewModelProvider(this)[viewModelClass()]
        }
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
