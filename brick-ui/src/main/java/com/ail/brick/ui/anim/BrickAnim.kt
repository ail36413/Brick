package com.ail.brick.ui.anim

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator

/**
 * View 动画扩展函数集，覆盖常见的 UI 动画场景。
 *
 * 所有动画函数均返回 [Animator]，可链式调用 [Animator.start]，也可用于组合动画。
 *
 * ### 淡入淡出
 * ```kotlin
 * view.fadeIn()
 * view.fadeOut()
 * ```
 *
 * ### 滑入滑出
 * ```kotlin
 * view.slideInFromBottom()
 * view.slideOutToTop()
 * ```
 *
 * ### 缩放
 * ```kotlin
 * view.scaleIn()                          // 从 0 → 1 弹入
 * view.scaleIn(interpolator = OvershootInterpolator())  // 带回弹
 * view.pulse()                            // 脉冲效果
 * ```
 *
 * ### 抖动
 * ```kotlin
 * view.shake()                            // 水平抖动（表单校验）
 * ```
 *
 * ### 组合动画
 * ```kotlin
 * view.fadeSlideIn()                      // 淡入 + 上滑
 * view.fadeSlideOut()                     // 淡出 + 下滑
 * ```
 */

// ==================== 淡入淡出 ====================

/**
 * 淡入动画：alpha 0 → 1。
 *
 * @param duration 动画时长（毫秒），默认 300ms
 * @param onEnd    动画结束回调
 * @return [Animator] 实例
 */
fun View.fadeIn(
    duration: Long = 300L,
    onEnd: (() -> Unit)? = null
): Animator {
    alpha = 0f
    visibility = View.VISIBLE
    return ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 淡出动画：alpha 1 → 0。
 *
 * 动画结束后自动将 View 设为 [View.GONE]。
 *
 * @param duration    动画时长（毫秒），默认 300ms
 * @param goneOnEnd   结束后是否设为 GONE，默认 true
 * @param onEnd       动画结束回调
 * @return [Animator] 实例
 */
fun View.fadeOut(
    duration: Long = 300L,
    goneOnEnd: Boolean = true,
    onEnd: (() -> Unit)? = null
): Animator {
    return ObjectAnimator.ofFloat(this, View.ALPHA, alpha, 0f).apply {
        this.duration = duration
        interpolator = AccelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (goneOnEnd) visibility = View.GONE
                onEnd?.invoke()
            }
        })
        start()
    }
}

// ==================== 滑入滑出 ====================

/**
 * 从底部滑入。
 *
 * @param duration      动画时长（毫秒），默认 300ms
 * @param translationY  起始偏移量（px），默认 View 高度（0 则使用 200px）
 * @param onEnd         动画结束回调
 */
fun View.slideInFromBottom(
    duration: Long = 300L,
    translationY: Float = 0f,
    onEnd: (() -> Unit)? = null
): Animator {
    val offset = if (translationY != 0f) translationY else (height.takeIf { it > 0 }?.toFloat() ?: 200f)
    this.translationY = offset
    visibility = View.VISIBLE
    alpha = 1f
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset, 0f).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 从顶部滑入。
 */
fun View.slideInFromTop(
    duration: Long = 300L,
    translationY: Float = 0f,
    onEnd: (() -> Unit)? = null
): Animator {
    val offset = if (translationY != 0f) -translationY else -(height.takeIf { it > 0 }?.toFloat() ?: 200f)
    this.translationY = offset
    visibility = View.VISIBLE
    alpha = 1f
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, offset, 0f).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 从左侧滑入。
 */
fun View.slideInFromLeft(
    duration: Long = 300L,
    translationX: Float = 0f,
    onEnd: (() -> Unit)? = null
): Animator {
    val offset = if (translationX != 0f) -translationX else -(width.takeIf { it > 0 }?.toFloat() ?: 200f)
    this.translationX = offset
    visibility = View.VISIBLE
    alpha = 1f
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_X, offset, 0f).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 从右侧滑入。
 */
fun View.slideInFromRight(
    duration: Long = 300L,
    translationX: Float = 0f,
    onEnd: (() -> Unit)? = null
): Animator {
    val offset = if (translationX != 0f) translationX else (width.takeIf { it > 0 }?.toFloat() ?: 200f)
    this.translationX = offset
    visibility = View.VISIBLE
    alpha = 1f
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_X, offset, 0f).apply {
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 向上滑出。
 *
 * @param goneOnEnd 结束后是否设为 GONE
 */
fun View.slideOutToTop(
    duration: Long = 300L,
    goneOnEnd: Boolean = true,
    onEnd: (() -> Unit)? = null
): Animator {
    val target = -(height.takeIf { it > 0 }?.toFloat() ?: 200f)
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, target).apply {
        this.duration = duration
        interpolator = AccelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (goneOnEnd) visibility = View.GONE
                translationY = 0f
                onEnd?.invoke()
            }
        })
        start()
    }
}

/**
 * 向下滑出。
 */
fun View.slideOutToBottom(
    duration: Long = 300L,
    goneOnEnd: Boolean = true,
    onEnd: (() -> Unit)? = null
): Animator {
    val target = height.takeIf { it > 0 }?.toFloat() ?: 200f
    return ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, target).apply {
        this.duration = duration
        interpolator = AccelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (goneOnEnd) visibility = View.GONE
                translationY = 0f
                onEnd?.invoke()
            }
        })
        start()
    }
}

// ==================== 缩放 ====================

/**
 * 缩放弹入（从 0 → 1）。
 *
 * @param duration     动画时长（毫秒），默认 300ms
 * @param interpolator 插值器，默认 OvershootInterpolator 带回弹效果
 * @param onEnd        动画结束回调
 */
fun View.scaleIn(
    duration: Long = 300L,
    interpolator: Interpolator = OvershootInterpolator(),
    onEnd: (() -> Unit)? = null
): Animator {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE
    alpha = 1f
    return AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(this@scaleIn, View.SCALE_X, 0f, 1f),
            ObjectAnimator.ofFloat(this@scaleIn, View.SCALE_Y, 0f, 1f)
        )
        this.duration = duration
        this.interpolator = interpolator
        addEndListener(onEnd)
        start()
    }
}

/**
 * 缩放弹出（从 1 → 0）。
 */
fun View.scaleOut(
    duration: Long = 300L,
    goneOnEnd: Boolean = true,
    onEnd: (() -> Unit)? = null
): Animator {
    return AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(this@scaleOut, View.SCALE_X, 1f, 0f),
            ObjectAnimator.ofFloat(this@scaleOut, View.SCALE_Y, 1f, 0f)
        )
        this.duration = duration
        interpolator = AccelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (goneOnEnd) visibility = View.GONE
                scaleX = 1f
                scaleY = 1f
                onEnd?.invoke()
            }
        })
        start()
    }
}

/**
 * 脉冲效果（先放大再缩小），常用于点赞、收藏等场景。
 *
 * @param scaleTo 最大放大比例，默认 1.2
 * @param duration 总动画时长（毫秒），默认 400ms
 */
fun View.pulse(
    scaleTo: Float = 1.2f,
    duration: Long = 400L,
    onEnd: (() -> Unit)? = null
): Animator {
    return AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(this@pulse, View.SCALE_X, 1f, scaleTo, 1f),
            ObjectAnimator.ofFloat(this@pulse, View.SCALE_Y, 1f, scaleTo, 1f)
        )
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

// ==================== 抖动 ====================

/**
 * 水平抖动动画，常用于表单校验失败提示。
 *
 * @param amplitude 抖动幅度（px），默认 10
 * @param duration  动画时长（毫秒），默认 400ms
 * @param onEnd     动画结束回调
 */
fun View.shake(
    amplitude: Float = 10f,
    duration: Long = 400L,
    onEnd: (() -> Unit)? = null
): Animator {
    return ObjectAnimator.ofFloat(
        this, View.TRANSLATION_X,
        0f, amplitude, -amplitude, amplitude, -amplitude, amplitude / 2, -amplitude / 2, 0f
    ).apply {
        this.duration = duration
        addEndListener(onEnd)
        start()
    }
}

/**
 * 垂直弹跳动画。
 *
 * @param bounceHeight 弹跳高度（px），默认 20
 * @param duration     动画时长（毫秒），默认 500ms
 */
fun View.bounce(
    bounceHeight: Float = 20f,
    duration: Long = 500L,
    onEnd: (() -> Unit)? = null
): Animator {
    return ObjectAnimator.ofFloat(
        this, View.TRANSLATION_Y,
        0f, -bounceHeight, 0f, -bounceHeight / 2, 0f
    ).apply {
        this.duration = duration
        addEndListener(onEnd)
        start()
    }
}

// ==================== 组合动画 ====================

/**
 * 淡入 + 上滑组合（常用于列表 item 进入动画）。
 *
 * @param duration     动画时长（毫秒），默认 400ms
 * @param translationY 起始 Y 偏移（px），默认 100
 */
fun View.fadeSlideIn(
    duration: Long = 400L,
    translationY: Float = 100f,
    onEnd: (() -> Unit)? = null
): Animator {
    alpha = 0f
    this.translationY = translationY
    visibility = View.VISIBLE
    return AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(this@fadeSlideIn, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(this@fadeSlideIn, View.TRANSLATION_Y, translationY, 0f)
        )
        this.duration = duration
        interpolator = DecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

/**
 * 淡出 + 下滑组合。
 *
 * @param duration     动画时长（毫秒），默认 400ms
 * @param translationY 结束 Y 偏移（px），默认 100
 * @param goneOnEnd    结束后是否设为 GONE
 */
fun View.fadeSlideOut(
    duration: Long = 400L,
    translationY: Float = 100f,
    goneOnEnd: Boolean = true,
    onEnd: (() -> Unit)? = null
): Animator {
    return AnimatorSet().apply {
        playTogether(
            ObjectAnimator.ofFloat(this@fadeSlideOut, View.ALPHA, alpha, 0f),
            ObjectAnimator.ofFloat(this@fadeSlideOut, View.TRANSLATION_Y, 0f, translationY)
        )
        this.duration = duration
        interpolator = AccelerateInterpolator()
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (goneOnEnd) visibility = View.GONE
                this@fadeSlideOut.translationY = 0f
                onEnd?.invoke()
            }
        })
        start()
    }
}

/**
 * 旋转动画。
 *
 * @param degrees  旋转角度，默认 360°
 * @param duration 动画时长（毫秒），默认 500ms
 */
fun View.rotate(
    degrees: Float = 360f,
    duration: Long = 500L,
    onEnd: (() -> Unit)? = null
): Animator {
    return ObjectAnimator.ofFloat(this, View.ROTATION, 0f, degrees).apply {
        this.duration = duration
        interpolator = AccelerateDecelerateInterpolator()
        addEndListener(onEnd)
        start()
    }
}

// ==================== 工具方法 ====================

private fun Animator.addEndListener(onEnd: (() -> Unit)?) {
    if (onEnd != null) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd.invoke()
            }
        })
    }
}
