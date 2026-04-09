package com.ail.brick.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment

// ==================== Activity 启动 ====================

/**
 * 通过泛型启动 Activity
 *
 * ```kotlin
 * context.startActivity<DetailActivity>()
 * context.startActivity<DetailActivity>(bundleOf("id" to 123))
 * ```
 *
 * @param extras 可选的 Bundle 参数
 */
inline fun <reified T : Activity> Context.startActivity(extras: Bundle? = null) {
    val intent = Intent(this, T::class.java)
    extras?.let { intent.putExtras(it) }
    startActivity(intent)
}

/**
 * 通过泛型启动 Activity（Lambda 配置 Intent）
 *
 * ```kotlin
 * context.startActivity<DetailActivity> {
 *     putExtra("id", 123)
 *     addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
 * }
 * ```
 */
inline fun <reified T : Activity> Context.startActivity(block: Intent.() -> Unit) {
    startActivity(Intent(this, T::class.java).apply(block))
}

/**
 * Fragment 通过泛型启动 Activity
 */
inline fun <reified T : Activity> Fragment.startActivity(extras: Bundle? = null) {
    requireContext().startActivity<T>(extras)
}

// ==================== 结果返回 ====================

/**
 * 设置结果码并关闭 Activity
 *
 * @param resultCode 结果码（如 [Activity.RESULT_OK]）
 * @param data 可选返回数据
 */
fun Activity.finishWithResult(resultCode: Int, data: Intent? = null) {
    setResult(resultCode, data)
    finish()
}

// ==================== 参数获取 ====================

/**
 * 安全获取 Activity Intent 中的 Extra 参数
 *
 * ```kotlin
 * val userId = extraOrNull<String>("user_id")
 * ```
 *
 * @return 指定类型的参数值，不存在或类型不匹配时返回 null
 */
@Suppress("DEPRECATION") // Bundle.get() is the only way to retrieve untyped extras for reified cast
inline fun <reified T> Activity.extraOrNull(key: String): T? {
    return intent?.extras?.get(key) as? T
}

/**
 * 安全获取 Fragment arguments 中的参数
 *
 * ```kotlin
 * val title = argumentOrNull<String>("title")
 * ```
 *
 * @return 指定类型的参数值，不存在或类型不匹配时返回 null
 */
@Suppress("DEPRECATION") // Bundle.get() is the only way to retrieve untyped extras for reified cast
inline fun <reified T> Fragment.argumentOrNull(key: String): T? {
    return arguments?.get(key) as? T
}
