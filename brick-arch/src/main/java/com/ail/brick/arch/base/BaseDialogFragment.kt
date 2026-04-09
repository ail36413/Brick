package com.ail.brick.arch.base

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

/**
 * DialogFragment 基类，自动管理 ViewBinding 并支持位置/宽度配置。
 *
 * 子类通过覆写 [dialogGravity] 和 [fullWidth] 控制弹窗行为。
 *
 * ### 居中弹窗（默认）
 * ```kotlin
 * class ConfirmDialog : BaseDialogFragment<DialogConfirmBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         DialogConfirmBinding.inflate(inflater, container, false)
 *     override fun initView() { binding.btnOk.setOnClickListener { dismiss() } }
 * }
 * ```
 *
 * ### 底部全宽弹窗
 * ```kotlin
 * class BottomSheet : BaseDialogFragment<DialogSheetBinding>() {
 *     override val dialogGravity = Gravity.BOTTOM
 *     override val fullWidth = true
 *     // ...
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 */
abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null

    /** ViewBinding 实例，仅在 [onCreateView]–[onDestroyView] 之间有效 */
    protected val binding: VB
        get() = _binding ?: error("ViewBinding 在 onCreateView 之前或 onDestroyView 之后不可访问")

    /** 由子类实现：创建 ViewBinding */
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /** 初始化视图 */
    abstract fun initView()

    /** 对话框位置，默认 [Gravity.CENTER]，可设为 [Gravity.BOTTOM] */
    open val dialogGravity: Int = Gravity.CENTER

    /** 宽度是否铺满屏幕，默认 false */
    open val fullWidth: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setGravity(dialogGravity)
            if (fullWidth) {
                setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
