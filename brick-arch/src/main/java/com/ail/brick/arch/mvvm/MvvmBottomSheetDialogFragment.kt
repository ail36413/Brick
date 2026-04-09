package com.ail.brick.arch.mvvm

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseBottomSheetDialogFragment
import com.ail.brick.arch.base.BaseViewModel
import kotlinx.coroutines.launch

/**
 * MVVM BottomSheetDialogFragment 基类。
 *
 * 自动创建 ViewModel 并收集通用 UI 事件，继承 [BaseBottomSheetDialogFragment] 的所有配置能力。
 *
 * ```kotlin
 * class ShareBottomSheet : MvvmBottomSheetDialogFragment<DialogShareBinding, ShareViewModel>() {
 *     override fun viewModelClass() = ShareViewModel::class.java
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         DialogShareBinding.inflate(inflater, container, false)
 *     override fun initView() {
 *         binding.btnWeChat.setOnClickListener { viewModel.shareToWeChat() }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 * @param VM ViewModel 类型
 */
abstract class MvvmBottomSheetDialogFragment<VB : ViewBinding, VM : BaseViewModel>
    : BaseBottomSheetDialogFragment<VB>() {

    protected lateinit var viewModel: VM
        private set

    /** 返回 ViewModel 的 Class 对象 */
    abstract fun viewModelClass(): Class<VM>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass()]
        super.onViewCreated(view, savedInstanceState)
        collectUIEvents()
    }

    private fun collectUIEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { onUIEvent(it) }
            }
        }
    }

    protected open fun onUIEvent(event: BaseViewModel.UIEvent) {
        when (event) {
            is BaseViewModel.UIEvent.Toast ->
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            is BaseViewModel.UIEvent.Loading -> onLoading(event.show)
            is BaseViewModel.UIEvent.NavigateBack -> dismiss()
            is BaseViewModel.UIEvent.Navigate -> onNavigate(event.route, event.extras)
            is BaseViewModel.UIEvent.Custom -> onCustomEvent(event.key, event.data)
        }
    }

    /** Loading 事件回调 */
    protected open fun onLoading(show: Boolean) {}

    /** 导航事件回调 */
    protected open fun onNavigate(route: String, extras: Map<String, Any>?) {}

    /** 自定义事件回调 */
    protected open fun onCustomEvent(key: String, data: Any?) {}
}
