package com.ail.brick.ui.statelayout

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import com.ail.brick.ui.R

/**
 * 四态页面容器：**内容 / 加载中 / 空数据 / 错误**。
 *
 * XML 中第一个子 View 自动被识别为「内容视图」，其余三种状态视图
 * 通过 XML 属性或代码指定（默认使用内置布局）。
 *
 * ### XML 用法
 * ```xml
 * <com.ail.brick.ui.statelayout.StateLayout
 *     android:id="@+id/stateLayout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:loadingLayout="@layout/custom_loading"
 *     app:emptyLayout="@layout/custom_empty"
 *     app:enableAnimation="true"
 *     app:animationDuration="200">
 *
 *     <!-- 第一个子 View 被用作内容视图 -->
 *     <RecyclerView ... />
 * </com.ail.brick.ui.statelayout.StateLayout>
 * ```
 *
 * ### 代码切换状态
 * ```kotlin
 * stateLayout.showLoading()
 * // 数据加载成功
 * stateLayout.showContent()
 * // 或空数据
 * stateLayout.showEmpty()
 * // 或失败（带重试）
 * stateLayout.showError { loadData() }
 * ```
 *
 * ### 切换动画
 * 默认启用 200ms 淡入淡出动画，可通过 XML 属性或代码控制：
 * ```kotlin
 * stateLayout.enableAnimation = false       // 关闭动画
 * stateLayout.animationDuration = 300L      // 自定义时长
 * ```
 *
 * 状态视图使用懒加载策略——首次切换到某状态时才 inflate 对应布局。
 */
class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 当前页面状态 */
    @Volatile
    var currentState: State = State.CONTENT
        private set

    /** 支持的四种页面状态 */
    enum class State { CONTENT, LOADING, EMPTY, ERROR }

    /** 状态变更监听器 */
    fun interface OnStateChangeListener {
        /**
         * 状态发生变更时回调。
         *
         * @param oldState 变更前的状态
         * @param newState 变更后的状态
         */
        fun onStateChanged(oldState: State, newState: State)
    }

    private var contentView: View? = null
    private var loadingView: View? = null
    private var emptyView: View? = null
    private var errorView: View? = null
    private var stateChangeListener: OnStateChangeListener? = null

    @LayoutRes private var loadingLayoutRes: Int = R.layout.brick_state_loading
    @LayoutRes private var emptyLayoutRes: Int = R.layout.brick_state_empty
    @LayoutRes private var errorLayoutRes: Int = R.layout.brick_state_error

    private var onRetryListener: (() -> Unit)? = null

    /** 是否启用状态切换的淡入淡出动画，默认 true */
    var enableAnimation: Boolean = true

    /** 动画时长（毫秒），默认 200ms */
    var animationDuration: Long = 200L

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.StateLayout)
        loadingLayoutRes = ta.getResourceId(R.styleable.StateLayout_loadingLayout, R.layout.brick_state_loading)
        emptyLayoutRes = ta.getResourceId(R.styleable.StateLayout_emptyLayout, R.layout.brick_state_empty)
        errorLayoutRes = ta.getResourceId(R.styleable.StateLayout_errorLayout, R.layout.brick_state_error)
        enableAnimation = ta.getBoolean(R.styleable.StateLayout_enableAnimation, true)
        animationDuration = ta.getInt(R.styleable.StateLayout_animationDuration, 200).toLong()
        ta.recycle()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            contentView = getChildAt(0)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable("superState", super.onSaveInstanceState())
            putString("state", currentState.name)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            @Suppress("DEPRECATION")
            super.onRestoreInstanceState(state.getParcelable("superState"))
            val saved = state.getString("state", State.CONTENT.name)
            val restoredState = try { State.valueOf(saved) } catch (_: Exception) { State.CONTENT }
            if (restoredState != currentState) {
                switchState(restoredState)
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /** 切换到内容状态，显示第一个子 View */
    fun showContent() {
        switchState(State.CONTENT)
    }

    /** 切换到加载中状态 */
    fun showLoading() {
        switchState(State.LOADING)
    }

    /** 切换到空数据状态 */
    fun showEmpty() {
        switchState(State.EMPTY)
    }

    /**
     * 切换到错误状态。
     *
     * 错误布局中 id 为 `btnRetry` 的 View 会自动绑定 [onRetry] 回调。
     *
     * @param onRetry 点击重试按钮的回调，null 表示不处理
     */
    fun showError(onRetry: (() -> Unit)? = null) {
        this.onRetryListener = onRetry
        switchState(State.ERROR)
    }

    // ==================== 自定义状态视图 ====================

    /** 通过布局资源 ID 设置自定义加载中视图（会在下次 showLoading 时重新 inflate） */
    fun setLoadingView(@LayoutRes layoutRes: Int) {
        loadingLayoutRes = layoutRes
        loadingView?.let { removeView(it) }
        loadingView = null
    }

    /** 通过布局资源 ID 设置自定义空数据视图 */
    fun setEmptyView(@LayoutRes layoutRes: Int) {
        emptyLayoutRes = layoutRes
        emptyView?.let { removeView(it) }
        emptyView = null
    }

    /** 通过布局资源 ID 设置自定义错误视图 */
    fun setErrorView(@LayoutRes layoutRes: Int) {
        errorLayoutRes = layoutRes
        errorView?.let { removeView(it) }
        errorView = null
    }

    /** 直接设置加载中视图实例 */
    fun setLoadingView(view: View) { loadingView = view }

    /** 直接设置空数据视图实例 */
    fun setEmptyView(view: View) { emptyView = view }

    /** 直接设置错误视图实例 */
    fun setErrorView(view: View) { errorView = view }

    /**
     * 设置状态变更监听器。
     *
     * ```kotlin
     * stateLayout.setOnStateChangeListener { oldState, newState ->
     *     Log.d("StateLayout", "$oldState -> $newState")
     * }
     * ```
     *
     * @param listener 状态变更回调，null 表示移除监听
     */
    fun setOnStateChangeListener(listener: OnStateChangeListener?) {
        stateChangeListener = listener
    }

    private fun switchState(state: State) {
        if (currentState == state) return
        val oldState = currentState
        currentState = state
        stateChangeListener?.onStateChanged(oldState, state)
        announceStateForAccessibility(state)
        showOrHide(contentView, state == State.CONTENT)
        showOrHide(state, State.LOADING) { ensureLoadingView() }
        showOrHide(state, State.EMPTY) { ensureEmptyView() }
        showOrHide(state, State.ERROR) { ensureErrorView() }
    }

    /**
     * 直接控制 contentView 可见性（带动画）
     */
    private fun showOrHide(view: View?, show: Boolean) {
        view ?: return
        if (show) {
            if (view.visibility != VISIBLE) {
                view.visibility = VISIBLE
                if (enableAnimation) view.fadeIn()
            }
        } else {
            view.visibility = GONE
        }
    }

    /**
     * 状态视图的显示/隐藏控制（带懒加载+动画）
     */
    private inline fun showOrHide(current: State, target: State, create: () -> View?) {
        if (current == target) {
            val view = create() ?: return
            if (view.parent == null) addView(view)
            if (view.visibility != VISIBLE) {
                view.visibility = VISIBLE
                if (enableAnimation) view.fadeIn()
            }
        } else {
            when (target) {
                State.LOADING -> loadingView
                State.EMPTY -> emptyView
                State.ERROR -> errorView
                else -> null
            }?.visibility = GONE
        }
    }

    private fun announceStateForAccessibility(state: State) {
        val message = when (state) {
            State.CONTENT -> "内容已加载"
            State.LOADING -> "加载中"
            State.EMPTY -> "暂无数据"
            State.ERROR -> "加载失败"
        }
        announceForAccessibility(message)
    }

    /** 淡入动画（使用 ViewPropertyAnimator） */
    private fun View.fadeIn() {
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(animationDuration)
            .start()
    }

    private fun ensureLoadingView(): View? {
        if (loadingView == null) {
            loadingView = LayoutInflater.from(context).inflate(loadingLayoutRes, this, false)
        }
        return loadingView
    }

    private fun ensureEmptyView(): View? {
        if (emptyView == null) {
            emptyView = LayoutInflater.from(context).inflate(emptyLayoutRes, this, false)
        }
        return emptyView
    }

    private fun ensureErrorView(): View? {
        if (errorView == null) {
            errorView = LayoutInflater.from(context).inflate(errorLayoutRes, this, false)
            errorView?.findViewById<View>(R.id.btnRetry)?.setOnClickListener {
                onRetryListener?.invoke()
            }
        }
        return errorView
    }
}
