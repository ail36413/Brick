package com.ail.brick.ui.anim

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Item 入场动画工具。
 *
 * ### 用法
 * ```kotlin
 * recyclerView.itemAnimator = BrickItemAnimator.fadeSlideUp()
 * // 或手动在 onBindViewHolder 中调用
 * BrickItemAnimator.animateItem(holder.itemView, position)
 * ```
 *
 * ### 在 Adapter 中使用
 * ```kotlin
 * override fun onBindViewHolder(holder: ViewHolder, position: Int) {
 *     bind(holder, getItem(position))
 *     BrickItemAnimator.animateItem(holder.itemView, position)
 * }
 * ```
 */
object BrickItemAnimator {

    /**
     * Item 入场类型。
     */
    enum class AnimType {
        /** 淡入 + 上滑 */
        FADE_SLIDE_UP,
        /** 淡入 + 左滑 */
        FADE_SLIDE_LEFT,
        /** 淡入 + 右滑 */
        FADE_SLIDE_RIGHT,
        /** 淡入 */
        FADE_IN,
        /** 缩放弹入 */
        SCALE_IN
    }

    /**
     * 为 item View 执行入场动画。
     *
     * 每个 item 会根据 [position] 产生递增延迟，营造逐个出现的效果。
     *
     * @param itemView      RecyclerView 的 item 视图
     * @param position      adapter position
     * @param type          动画类型，默认 [AnimType.FADE_SLIDE_UP]
     * @param duration      单个 item 动画时长（毫秒），默认 300ms
     * @param delayPerItem  每个 item 的递增延迟（毫秒），默认 50ms
     */
    fun animateItem(
        itemView: View,
        position: Int,
        type: AnimType = AnimType.FADE_SLIDE_UP,
        duration: Long = 300L,
        delayPerItem: Long = 50L
    ) {
        val delay = position * delayPerItem

        when (type) {
            AnimType.FADE_SLIDE_UP -> {
                itemView.alpha = 0f
                itemView.translationY = 80f
                itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            AnimType.FADE_SLIDE_LEFT -> {
                itemView.alpha = 0f
                itemView.translationX = 80f
                itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            AnimType.FADE_SLIDE_RIGHT -> {
                itemView.alpha = 0f
                itemView.translationX = -80f
                itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            AnimType.FADE_IN -> {
                itemView.alpha = 0f
                itemView.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            AnimType.SCALE_IN -> {
                itemView.scaleX = 0.8f
                itemView.scaleY = 0.8f
                itemView.alpha = 0f
                itemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    /**
     * 重置 item 动画状态（在 onViewRecycled 中调用，防止复用问题）。
     */
    fun resetItem(itemView: View) {
        itemView.animate().cancel()
        itemView.alpha = 1f
        itemView.translationX = 0f
        itemView.translationY = 0f
        itemView.scaleX = 1f
        itemView.scaleY = 1f
    }
}
