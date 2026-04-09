package com.ail.brick.arch.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Fragment 基类，自动管理 ViewBinding 生命周期并支持懒加载。
 *
 * 生命周期回调顺序：
 * 1. [inflateBinding] → 2. [initView] → 3. [initObservers] → 4. [onLazyLoad]（首次可见时）
 *
 * ```kotlin
 * class HomeFragment : BaseFragment<FragmentHomeBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         FragmentHomeBinding.inflate(inflater, container, false)
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.tvTitle.text = "Hello"
 *     }
 *
 *     override fun onLazyLoad() {
 *         viewModel.loadData()
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    /**
     * 当前 ViewBinding 实例。
     *
     * 仅在 [onCreateView]–[onDestroyView] 之间有效，
     * 其他时刻访问抛出 [IllegalStateException]。
     */
    protected val binding: VB
        get() = _binding ?: error("ViewBinding 在 onCreateView 之前或 onDestroyView 之后不可访问")

    private var isFirstLoad = true

    companion object {
        private const val KEY_IS_FIRST_LOAD = "brick_base_fragment_is_first_load"
    }

    /** 由子类实现：创建 ViewBinding 实例 */
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /** 初始化视图 */
    abstract fun initView(savedInstanceState: Bundle?)

    /** 可选：初始化数据观察 */
    open fun initObservers() {}

    /**
     * 懒加载回调，Fragment 首次可见（[onResume]）时触发一次。
     *
     * 适合在此处发起首次数据请求。
     */
    open fun onLazyLoad() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isFirstLoad = savedInstanceState.getBoolean(KEY_IS_FIRST_LOAD, true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        if (isFirstLoad) {
            isFirstLoad = false
            onLazyLoad()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_FIRST_LOAD, isFirstLoad)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
