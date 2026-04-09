package com.ail.brick.ui.titlebar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.ViewCompat
import com.ail.brick.ui.R
import com.google.android.material.color.MaterialColors

/**
 * 通用标题栏组件，支持 XML 属性配置和代码动态设置。
 *
 * 包含三个区域：**左侧返回按钮**、**居中标题**、**右侧文字/图标按钮**。
 * 支持右侧图标按钮和沉浸式状态栏内边距。
 *
 * ### XML 用法
 * ```xml
 * <com.ail.brick.ui.titlebar.TitleBar
 *     android:id="@+id/titleBar"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:titleBar_title="页面标题"
 *     app:titleBar_showBack="true"
 *     app:titleBar_rightText="保存"
 *     app:titleBar_rightIcon="@drawable/ic_more"
 *     app:titleBar_immersive="true"
 *     app:titleBar_bgColor="#FFFFFF" />
 * ```
 *
 * ### 代码用法
 * ```kotlin
 * titleBar.title = "详情"
 * titleBar.setOnBackClickListener { onBackPressedDispatcher.onBackPressed() }
 * titleBar.setRightText("编辑") { startEditMode() }
 * titleBar.setRightIcon(R.drawable.ic_more) { showMenu() }
 * ```
 *
 * | XML 属性 | 说明 |
 * |---|---|
 * | `titleBar_title` | 标题文字 |
 * | `titleBar_showBack` | 是否显示返回按钮（默认 true） |
 * | `titleBar_leftIcon` | 自定义左侧图标资源 |
 * | `titleBar_rightText` | 右侧按钮文字 |
 * | `titleBar_rightIcon` | 右侧图标资源 |
 * | `titleBar_titleColor` | 标题颜色（默认黑色） |
 * | `titleBar_bgColor` | 背景颜色（默认白色） |
 * | `titleBar_immersive` | 是否适配沉浸式状态栏（默认 false） |
 */
class TitleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tvTitle: TextView
    private val ivBack: ImageView
    private val tvRight: TextView
    private val ivRight: ImageView
    private val rightContainer: LinearLayout
    private var immersivePaddingApplied = false

    /** 标题文字，支持读写 */
    var title: CharSequence
        get() = tvTitle.text
        set(value) { tvTitle.text = value }

    init {
        val density = resources.displayMetrics.density
        val barHeight = (48 * density).toInt()
        minimumHeight = barHeight

        ivBack = ImageView(context).apply {
            layoutParams = LayoutParams((48 * density).toInt(), barHeight).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            scaleType = ImageView.ScaleType.CENTER
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            setImageResource(android.R.drawable.ic_menu_revert)
            contentDescription = "Back"
        }
        addView(ivBack)

        tvTitle = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.BLACK)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            ViewCompat.setAccessibilityHeading(this, true)
        }
        addView(tvTitle)

        // 右侧容器（包含文字按钮和图标按钮）
        rightContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, barHeight).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = (8 * density).toInt()
            }
            this.gravity = Gravity.CENTER_VERTICAL
        }

        tvRight = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, barHeight)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            this.gravity = Gravity.CENTER
            val dp8 = (8 * density).toInt()
            setPadding(dp8, 0, dp8, 0)
            visibility = GONE
        }
        rightContainer.addView(tvRight)

        ivRight = ImageView(context).apply {
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, barHeight)
            scaleType = ImageView.ScaleType.CENTER
            val dp12 = (12 * density).toInt()
            setPadding(dp12, 0, dp12, 0)
            visibility = GONE
            contentDescription = "Action"
        }
        rightContainer.addView(ivRight)
        addView(rightContainer)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.TitleBar)
        title = ta.getString(R.styleable.TitleBar_titleBar_title) ?: ""
        val showBack = ta.getBoolean(R.styleable.TitleBar_titleBar_showBack, true)
        val leftIcon = ta.getResourceId(R.styleable.TitleBar_titleBar_leftIcon, 0)
        val rightText = ta.getString(R.styleable.TitleBar_titleBar_rightText)
        val rightIcon = ta.getResourceId(R.styleable.TitleBar_titleBar_rightIcon, 0)
        val titleColor = ta.getColor(R.styleable.TitleBar_titleBar_titleColor,
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
        val bgColor = ta.getColor(R.styleable.TitleBar_titleBar_bgColor,
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.WHITE))
        val immersive = ta.getBoolean(R.styleable.TitleBar_titleBar_immersive, false)
        ta.recycle()

        if (leftIcon != 0) ivBack.setImageResource(leftIcon)
        ivBack.visibility = if (showBack) VISIBLE else GONE
        tvTitle.setTextColor(titleColor)
        setBackgroundColor(bgColor)

        if (!rightText.isNullOrEmpty()) {
            tvRight.text = rightText
            tvRight.visibility = VISIBLE
        }
        if (rightIcon != 0) {
            ivRight.setImageResource(rightIcon)
            ivRight.visibility = VISIBLE
        }

        if (immersive) applyImmersivePadding()

        // 默认点击返回按钮关闭 Activity，可通过 setOnBackClickListener 覆盖
        ivBack.setOnClickListener {
            findActivity(context)?.finish()
        }
    }

    private fun findActivity(ctx: Context): Activity? {
        var context = ctx
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

    /**
     * 设置返回按钮点击监听器，覆盖默认的 finish() 行为。
     *
     * ```kotlin
     * titleBar.setOnBackClickListener { onBackPressedDispatcher.onBackPressed() }
     * ```
     */
    fun setOnBackClickListener(listener: OnClickListener) {
        ivBack.setOnClickListener(listener)
    }

    /**
     * 设置右侧文字按钮及其点击事件。
     *
     * [text] 为空时右侧按钮自动隐藏。
     *
     * @param text     按钮文字
     * @param listener 点击回调
     */
    fun setRightText(text: String, listener: OnClickListener? = null) {
        tvRight.text = text
        tvRight.visibility = if (text.isEmpty()) GONE else VISIBLE
        listener?.let { tvRight.setOnClickListener(it) }
    }

    /** 获取右侧 [TextView]，可用于进一步自定义样式 */
    fun getRightTextView(): TextView = tvRight

    /** 获取右侧 [ImageView]，可用于进一步自定义 */
    fun getRightImageView(): ImageView = ivRight

    /** 获取左侧返回按钮 [View]，可用于进一步自定义 */
    fun getBackView(): View = ivBack

    /**
     * 设置右侧图标按钮及其点击事件。
     *
     * @param iconRes  图标资源 ID
     * @param listener 点击回调
     */
    fun setRightIcon(@DrawableRes iconRes: Int, listener: OnClickListener? = null) {
        ivRight.setImageResource(iconRes)
        ivRight.visibility = if (iconRes != 0) VISIBLE else GONE
        listener?.let { ivRight.setOnClickListener(it) }
    }

    /**
     * 适配沉浸式状态栏，自动添加状态栏高度的顶部内边距。
     * 调用后标题栏会延伸到状态栏区域。重复调用不会累加内边距。
     */
    fun applyImmersivePadding() {
        if (immersivePaddingApplied) return
        immersivePaddingApplied = true
        post {
            val statusBarHeight = getStatusBarHeight()
            setPadding(paddingLeft, paddingTop + statusBarHeight, paddingRight, paddingBottom)
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
}
