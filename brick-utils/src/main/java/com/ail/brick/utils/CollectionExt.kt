package com.ail.brick.utils

/**
 * 集合非空且有元素时执行操作
 *
 * ```kotlin
 * list?.ifNotEmpty { items -> adapter.submitList(items.toList()) }
 * ```
 */
inline fun <T> Collection<T>?.ifNotEmpty(action: (Collection<T>) -> Unit) {
    if (!isNullOrEmpty()) action(this!!)
}

/**
 * 安全的 joinToString，null 或空集合返回空字符串
 *
 * ```kotlin
 * val tags: List<String>? = null
 * tags.safeJoinToString()  // ""
 * ```
 */
fun <T> Collection<T>?.safeJoinToString(
    separator: CharSequence = ", ",
    transform: ((T) -> CharSequence)? = null
): String {
    if (isNullOrEmpty()) return ""
    return joinToString(separator, transform = transform)
}
