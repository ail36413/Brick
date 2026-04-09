package com.ail.brick.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 线程安全的 [SimpleDateFormat] 缓存，避免高频调用时重复创建对象。
 *
 * 每个线程独立持有一份 pattern → formatter 的映射，无需加锁。
 */
private val threadLocalFormatters = object : ThreadLocal<MutableMap<String, SimpleDateFormat>>() {
    override fun initialValue(): MutableMap<String, SimpleDateFormat> = mutableMapOf()
}

private fun getFormatter(pattern: String): SimpleDateFormat {
    val cache = threadLocalFormatters.get()!!
    return cache.getOrPut(pattern) { SimpleDateFormat(pattern, Locale.getDefault()) }
}

/**
 * 时间戳格式化为日期字符串
 *
 * ```kotlin
 * System.currentTimeMillis().formatDate()                 // "2026-04-02 10:30:00"
 * System.currentTimeMillis().formatDate("yyyy/MM/dd")     // "2026/04/02"
 * ```
 *
 * @param pattern 日期格式，默认 `yyyy-MM-dd HH:mm:ss`
 * @return 格式化后的日期字符串
 */
fun Long.formatDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return getFormatter(pattern).format(Date(this))
}

/**
 * Date 对象格式化为字符串
 *
 * @param pattern 日期格式，默认 `yyyy-MM-dd HH:mm:ss`
 * @return 格式化后的日期字符串
 */
fun Date.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return getFormatter(pattern).format(this)
}

/**
 * 将日期字符串解析为 [Date] 对象
 *
 * ```kotlin
 * "2026-04-02".parseDate("yyyy-MM-dd")  // Date?
 * ```
 *
 * @param pattern 日期格式，需与字符串匹配
 * @return 解析成功返回 [Date]，失败返回 null
 */
fun String.parseDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): Date? {
    return try {
        getFormatter(pattern).parse(this)
    } catch (_: Exception) {
        null
    }
}

/**
 * 获取当前系统时间戳（毫秒）
 */
fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * 判断时间戳是否为今天
 *
 * ```kotlin
 * System.currentTimeMillis().isToday()  // true
 * ```
 */
fun Long.isToday(): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = this@isToday }
    val cal2 = Calendar.getInstance()
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * 判断时间戳是否为昨天
 *
 * ```kotlin
 * (System.currentTimeMillis() - 86_400_000L).isYesterday()  // true
 * ```
 */
fun Long.isYesterday(): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = this@isYesterday }
    val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * 判断两个时间戳是否为同一天
 *
 * ```kotlin
 * time1.isSameDay(time2)
 * ```
 */
fun Long.isSameDay(other: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = this@isSameDay }
    val cal2 = Calendar.getInstance().apply { timeInMillis = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * 将时间戳转换为友好的中文时间描述
 *
 * 规则：
 * - < 1分钟 → "刚刚"
 * - < 1小时 → "X分钟前"
 * - < 24小时 → "X小时前"
 * - < 48小时 → "昨天"
 * - 更早 → "MM-dd HH:mm"
 *
 * ```kotlin
 * (System.currentTimeMillis() - 30_000).toFriendlyTime()  // "刚刚"
 * ```
 */
fun Long.toFriendlyTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 0L -> formatDate("MM-dd HH:mm") // 未来时间直接显示日期
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L}分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L}小时前"
        diff < 172_800_000L -> "昨天"
        else -> formatDate("MM-dd HH:mm")
    }
}
