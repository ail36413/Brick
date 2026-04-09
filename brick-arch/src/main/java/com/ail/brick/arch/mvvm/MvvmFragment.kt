package com.ail.brick.arch.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseFragment
import com.ail.brick.arch.base.BaseViewModel
import kotlinx.coroutines.launch

/**
 * MVVM Fragment 基类。
 *
 * 子类通过 [viewModelClass] 提供 ViewModel 类型，基类自动创建并收集通用 UI 事件。
 * 设置 [shareViewModelWithActivity] = true 可与宿主 Activity 共享 ViewModel。
 *
 * @param VB ViewBinding 类型
 * @param VM ViewModel 类型
 */
abstract class MvvmFragment<VB : ViewBinding, VM : BaseViewModel> : BaseFragment<VB>() {

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
        collectUIEvents()
    }

    private fun collectUIEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    onUIEvent(event)
                }
            }
        }
    }

    protected open fun onUIEvent(event: BaseViewModel.UIEvent) {
        when (event) {
            is BaseViewModel.UIEvent.Toast -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            is BaseViewModel.UIEvent.Loading -> onLoading(event.show)
            is BaseViewModel.UIEvent.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
            is BaseViewModel.UIEvent.Navigate -> onNavigate(event.route, event.extras)
            is BaseViewModel.UIEvent.Custom -> onCustomEvent(event.key, event.data)
        }
    }

    /** Loading 事件回调，子类按需覆写 */
    protected open fun onLoading(show: Boolean) {}

    /** 导航事件回调，子类按需覆写 */
    protected open fun onNavigate(route: String, extras: Map<String, Any>?) {}

    /** 自定义事件回调 */
    protected open fun onCustomEvent(key: String, data: Any?) {}
}
