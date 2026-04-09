package com.ail.brick.arch.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.viewbinding.ViewBinding
import com.ail.brick.arch.R

/**
 * BottomSheetDialogFragment 基类，自动管理 ViewBinding 并支持常用配置。
 *
 * ### 基本用法
 * ```kotlin
 * class ShareBottomSheet : BaseBottomSheetDialogFragment<DialogShareBinding>() {
 *     override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         DialogShareBinding.inflate(inflater, container, false)
 *     override fun initView() {
 *         binding.btnWeChat.setOnClickListener { shareToWeChat() }
 *         binding.btnCancel.setOnClickListener { dismiss() }
 *     }
 * }
 * ```
 *
 * ### 自定义配置
 * ```kotlin
 * class DetailBottomSheet : BaseBottomSheetDialogFragment<DialogDetailBinding>() {
 *     override val peekHeight = 400            // 初始窥视高度（px）
 *     override val maxHeightRatio = 0.8f       // 最大高度为屏幕 80%
 *     override val isDraggable = true           // 可拖拽
 *     override val isHideable = true            // 可下滑隐藏
 *     override val dimAmount = 0.5f             // 背景蒙层透明度
 *     override val skipCollapsed = false        // 关闭时是否跳过折叠态
 *     // ...
 * }
 * ```
 *
 * ### 展开状态启动
 * ```kotlin
 * class FullBottomSheet : BaseBottomSheetDialogFragment<DialogFullBinding>() {
 *     override val initialState = BottomSheetBehavior.STATE_EXPANDED
 *     override val skipCollapsed = true
 *     // ...
 * }
 * ```
 *
 * @param VB ViewBinding 类型
 */
abstract class BaseBottomSheetDialogFragment<VB : ViewBinding> : BottomSheetDialogFragment() {

    private var _binding: VB? = null

    /** ViewBinding 实例，仅在 [onCreateView]–[onDestroyView] 之间有效 */
    protected val binding: VB
        get() = _binding ?: error("ViewBinding 在 onCreateView 之前或 onDestroyView 之后不可访问")

    /** 由子类实现：创建 ViewBinding */
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /** 初始化视图 */
    abstract fun initView()

    // ==================== 可配置属性 ====================

    /**
     * 初始窥视高度（px）。
     * 设为 [BottomSheetBehavior.PEEK_HEIGHT_AUTO] 自动计算，默认自动。
     */
    open val peekHeight: Int = BottomSheetBehavior.PEEK_HEIGHT_AUTO

    /**
     * 最大高度占屏幕高度的比例，取值 0.0~1.0，默认不限制。
     * 设为 0.8f 表示底部弹窗最高为屏幕 80%。
     */
    @FloatRange(from = 0.0, to = 1.0)
    open val maxHeightRatio: Float = 0f

    /** 初始状态，默认 [BottomSheetBehavior.STATE_COLLAPSED] */
    open val initialState: Int = BottomSheetBehavior.STATE_COLLAPSED

    /** 是否可拖拽，默认 true */
    open val isDraggable: Boolean = true

    /** 是否可向下滑动隐藏，默认 true */
    open val isHideable: Boolean = true

    /** 关闭时是否跳过折叠态直接隐藏，默认 false */
    open val skipCollapsed: Boolean = false

    /** 背景蒙层透明度，取值 0.0（全透明）~1.0（全黑），默认 0.5 */
    @FloatRange(from = 0.0, to = 1.0)
    open val dimAmount: Float = 0.5f

    /** 点击外部区域是否可取消，默认 true */
    open val canceledOnTouchOutside: Boolean = true

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
        configureBottomSheet()
        configureDialog()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configureBottomSheet() {
        val bottomSheet = (dialog as? BottomSheetDialog)
            ?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            ?: return

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.peekHeight = peekHeight
        behavior.state = initialState
        behavior.isDraggable = isDraggable
        behavior.isHideable = isHideable
        behavior.skipCollapsed = skipCollapsed

        // 最大高度限制
        if (maxHeightRatio > 0f) {
            val screenHeight = resources.displayMetrics.heightPixels
            val maxHeight = (screenHeight * maxHeightRatio).toInt()
            bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
                height = maxHeight
            }
            behavior.maxHeight = maxHeight
        }
    }

    private fun configureDialog() {
        dialog?.apply {
            setCanceledOnTouchOutside(canceledOnTouchOutside)
            window?.setDimAmount(dimAmount)
        }
    }
}
