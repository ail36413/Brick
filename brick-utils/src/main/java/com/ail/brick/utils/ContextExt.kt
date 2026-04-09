package com.ail.brick.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue

/**
 * dp 值转换为 px 值
 *
 * @param dp 密度无关像素值
 * @return 转换后的像素值（四舍五入取整）
 */
fun Context.dp2px(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}

/**
 * sp 值转换为 px 值
 *
 * @param sp 缩放无关像素值
 * @return 转换后的像素值（四舍五入取整）
 */
fun Context.sp2px(sp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics).toInt()
}

/**
 * px 值转换为 dp 值
 *
 * @param px 像素值
 * @return 转换后的 dp 值
 */
fun Context.px2dp(px: Int): Float {
    return px / resources.displayMetrics.density
}

/**
 * px 值转换为 sp 值
 *
 * @param px 像素值
 * @return 转换后的 sp 值
 */
@Suppress("DEPRECATION") // scaledDensity is the standard way to convert px<->sp; not actually deprecated for this use
fun Context.px2sp(px: Int): Float {
    return px / resources.displayMetrics.scaledDensity
}

// ==================== 不依赖 Context 的数值扩展 ====================

/**
 * 将数值作为 dp 转换为 px（取整）
 *
 * ```kotlin
 * val widthPx = 16.dp   // 16dp 对应的 px 值
 * ```
 */
val Number.dp: Int
    get() = (toFloat() * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

/** 将数值作为 dp 转换为 px（浮点） */
val Number.dpF: Float
    get() = toFloat() * Resources.getSystem().displayMetrics.density

/**
 * 将数值作为 sp 转换为 px（取整）
 *
 * ```kotlin
 * val textSizePx = 14.sp
 * ```
 */
@Suppress("DEPRECATION") // scaledDensity via Resources.getSystem() is the standard approach for sp conversion
val Number.sp: Int
    get() = (toFloat() * Resources.getSystem().displayMetrics.scaledDensity + 0.5f).toInt()

/** 将数值作为 sp 转换为 px（浮点） */
@Suppress("DEPRECATION") // Same as above: scaledDensity for sp<->px
val Number.spF: Float
    get() = toFloat() * Resources.getSystem().displayMetrics.scaledDensity

// ==================== 屏幕信息 ====================

/** 屏幕宽度（px） */
val Context.screenWidth: Int get() = resources.displayMetrics.widthPixels

/** 屏幕高度（px） */
val Context.screenHeight: Int get() = resources.displayMetrics.heightPixels

/**
 * 状态栏高度（px）
 *
 * 通过系统内部资源 `status_bar_height` 获取，获取失败返回 0。
 */
val Context.statusBarHeight: Int
    get() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

/**
 * 导航栏高度（px）
 *
 * 通过系统内部资源 `navigation_bar_height` 获取，获取失败返回 0。
 *
 * ```kotlin
 * val navBarH = context.navigationBarHeight
 * ```
 */
val Context.navigationBarHeight: Int
    get() {
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
