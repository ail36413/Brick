package com.ail.brick.utils

import android.view.View

/**
 * 设置防抖点击监听器，在 [interval] 毫秒内多次点击只触发一次
 *
 * ```kotlin
 * binding.btnSubmit.onClick { submitForm() }
 * binding.btnSubmit.onClick(interval = 1000L) { submitForm() }
 * ```
 *
 * @param interval 防抖间隔（毫秒），默认 500ms
 * @param onClick 点击回调
 */
fun View.onClick(interval: Long = 500L, onClick: (View) -> Unit) {
    require(interval > 0L) { "Debounce interval must be > 0ms, got $interval" }
    var lastClickTime = 0L
    setOnClickListener { v ->
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= interval) {
            lastClickTime = now
            onClick(v)
        }
    }
}

// ==================== 可见性控制 ====================

/** 设置为可见 [View.VISIBLE] */
fun View.visible() { visibility = View.VISIBLE }

/** 设置为不可见但占位 [View.INVISIBLE] */
fun View.invisible() { visibility = View.INVISIBLE }

/** 设置为隐藏且不占位 [View.GONE] */
fun View.gone() { visibility = View.GONE }

// 注意：isVisible / isGone 已由 AndroidX core-ktx 提供（var 版本，可读可写），
// 请使用 `import androidx.core.view.isVisible` / `import androidx.core.view.isGone`

/**
 * 根据条件切换 View 可见性
 *
 * ```kotlin
 * binding.tvEmpty.setVisible(list.isEmpty())
 * binding.tvEmpty.setVisible(list.isEmpty(), goneIfFalse = false) // 不可见时使用 INVISIBLE
 * ```
 *
 * @param visible true → VISIBLE，false → GONE（默认）或 INVISIBLE
 * @param goneIfFalse false 时使用 GONE（true）还是 INVISIBLE（false）
 */
fun View.setVisible(visible: Boolean, goneIfFalse: Boolean = true) {
    visibility = when {
        visible -> View.VISIBLE
        goneIfFalse -> View.GONE
        else -> View.INVISIBLE
    }
}

/**
 * 批量设置多个 View 的可见性
 *
 * ```kotlin
 * setVisible(binding.tvA, binding.tvB, visible = false)
 * ```
 */
fun setVisible(vararg views: View, visible: Boolean, goneIfFalse: Boolean = true) {
    views.forEach { it.setVisible(visible, goneIfFalse) }
}

/**
 * 延迟执行操作（参数顺序更符合 Kotlin 习惯）
 *
 * ```kotlin
 * view.postDelayed(300L) { doSomething() }
 * ```
 *
 * @param delayMillis 延迟毫秒数
 * @param action 要执行的操作
 */
fun View.postDelayed(delayMillis: Long, action: () -> Unit) {
    postDelayed(action, delayMillis)
}
