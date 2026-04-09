package com.ail.brick.arch.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Activity 基类，自动管理 ViewBinding 生命周期。
 *
 * 子类只需实现 [inflateBinding] 和 [initView]，生命周期调用顺序：
 * 1. [inflateBinding] → 2. `setContentView` → 3. [initView] → 4. [initObservers]
 *
 * ```kotlin
 * class HomeActivity : BaseActivity<ActivityHomeBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater) =
 *         ActivityHomeBinding.inflate(inflater)
 *
 *     override fun initView(savedInstanceState: Bundle?) {
 *         binding.tvTitle.text = "Hello"
 *     }
 *
 *     override fun initObservers() {
 *         viewModel.data.observe(this) { /* ... */ }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null

    /**
     * 当前 ViewBinding 实例。
     *
     * 仅在 [onCreate]–[onDestroy] 之间有效，其他时刻访问抛出 [IllegalStateException]。
     */
    protected val binding: VB
        get() = _binding ?: error("ViewBinding 在 onCreate 之前或 onDestroy 之后不可访问")

    /** 由子类实现：创建 ViewBinding 实例 */
    abstract fun inflateBinding(inflater: LayoutInflater): VB

    /** 初始化视图，在 `setContentView` 之后回调 */
    abstract fun initView(savedInstanceState: Bundle?)

    /** 可选：初始化数据观察（在 [initView] 之后调用） */
    open fun initObservers() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        onPreInit(savedInstanceState)
        initView(savedInstanceState)
        initObservers()
    }

    /**
     * 在 [initView] 之前调用的钩子方法。
     *
     * 子类（如 [MvvmActivity]、[MviActivity]）可在此初始化 ViewModel 等依赖，
     * 保证 [initView] 和 [initObservers] 中可安全访问 ViewModel。
     */
    protected open fun onPreInit(savedInstanceState: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
