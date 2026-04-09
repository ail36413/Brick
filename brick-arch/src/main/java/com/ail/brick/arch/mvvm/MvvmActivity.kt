package com.ail.brick.arch.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.base.BaseActivity
import com.ail.brick.arch.base.BaseViewModel
import kotlinx.coroutines.launch

/**
 * MVVM Activity 基类。
 *
 * 子类通过 [viewModelClass] 提供 ViewModel 类型，基类自动创建实例并收集 [BaseViewModel.uiEvent]
 * 分发到 [onUIEvent] / [onLoading] / [onNavigate] / [onCustomEvent]。
 *
 * ```kotlin
 * class HomeActivity : MvvmActivity<ActivityHomeBinding, HomeViewModel>() {
 *     override fun viewModelClass() = HomeViewModel::class.java
 *
 *     override fun inflateBinding(inflater: LayoutInflater) =
 *         ActivityHomeBinding.inflate(inflater)
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.btnLoad.setOnClickListener { viewModel.loadData() }
 *     }
 *
 *     override fun onLoading(show: Boolean) {
 *         if (show) LoadingDialog.show(this) else LoadingDialog.dismiss()
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 * @param VM ViewModel 类型
 */
abstract class MvvmActivity<VB : ViewBinding, VM : BaseViewModel> : BaseActivity<VB>() {

    protected lateinit var viewModel: VM
        private set

    /** 返回 ViewModel 的 Class 对象，子类必须实现 */
    abstract fun viewModelClass(): Class<VM>

    override fun onPreInit(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass()]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectUIEvents()
    }

    private fun collectUIEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    onUIEvent(event)
                }
            }
        }
    }

    /**
     * 处理通用 UI 事件。
     *
     * 默认行为：Toast → 弹 Toast，Loading → 调 [onLoading]，
     * NavigateBack → finish()。子类可覆写定制行为。
     */
    protected open fun onUIEvent(event: BaseViewModel.UIEvent) {
        when (event) {
            is BaseViewModel.UIEvent.Toast -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
            }
            is BaseViewModel.UIEvent.Loading -> onLoading(event.show)
            is BaseViewModel.UIEvent.NavigateBack -> finish()
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
