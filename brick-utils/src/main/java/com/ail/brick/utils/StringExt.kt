package com.ail.brick.utils

import java.security.MessageDigest

// ==================== 编译一次的正则缓存 ====================

private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val ID_CARD_REGEX = Regex("^\\d{17}[\\dXx]$")
private val URL_REGEX = Regex("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*)*$")

// ==================== 格式校验 ====================

/**
 * 判断是否为有效中国大陆手机号（1开头，11位）
 *
 * ```kotlin
 * "13812345678".isPhoneNumber()  // true
 * "1234".isPhoneNumber()         // false
 * ```
 */
fun String.isPhoneNumber(): Boolean = matches(PHONE_REGEX)

/**
 * 判断是否为有效邮箱地址
 *
 * ```kotlin
 * "test@example.com".isEmail()  // true
 * "invalid".isEmail()           // false
 * ```
 */
fun String.isEmail(): Boolean = matches(EMAIL_REGEX)

/**
 * 判断是否为有效身份证号（简单 18 位格式校验，最后一位支持 X/x）
 *
 * ```kotlin
 * "110101199001011234".isIdCard()  // true
 * "11010119900101123X".isIdCard()  // true
 * ```
 */
fun String.isIdCard(): Boolean = matches(ID_CARD_REGEX)

/**
 * 判断字符串是否全由数字组成
 *
 * @return 空字符串返回 true（与 Kotlin stdlib 行为一致）
 */
fun String.isDigitsOnly(): Boolean = all { it.isDigit() }

/**
 * 判断是否为有效的 HTTP/HTTPS URL
 *
 * ```kotlin
 * "https://example.com".isUrl()      // true
 * "http://a.b/path?q=1".isUrl()      // true
 * "ftp://invalid.com".isUrl()         // false
 * ```
 */
fun String.isUrl(): Boolean = matches(URL_REGEX)

// ==================== 脱敏处理 ====================

/**
 * 手机号脱敏（中间4位替换为 `****`）
 *
 * ```kotlin
 * "13812345678".maskPhone()  // "138****5678"
 * ```
 *
 * @return 非11位字符串原样返回
 */
fun String.maskPhone(): String {
    if (length != 11) return this
    return replaceRange(3, 7, "****")
}

/**
 * 身份证号脱敏（保留前4位和后4位，中间用 `*` 替换）
 *
 * ```kotlin
 * "110101199001011234".maskIdCard()  // "1101**********1234"
 * ```
 *
 * @return 长度不足8位时原样返回
 */
fun String.maskIdCard(): String {
    if (length < 8) return this
    return replaceRange(4, length - 4, "*".repeat(length - 8))
}

/**
 * 邮箱脱敏（保留首字母，@前其余字符替换为 `*`）
 *
 * ```kotlin
 * "hello@example.com".maskEmail()  // "h****@example.com"
 * ```
 *
 * @return 无 `@` 或 `@` 前仅1字符时原样返回
 */
fun String.maskEmail(): String {
    val atIndex = indexOf('@')
    if (atIndex <= 1) return this
    return "${first()}${"*".repeat(atIndex - 1)}${substring(atIndex)}"
}

// ==================== 摘要加密 ====================

/**
 * 计算字符串的 MD5 摘要（小写32位十六进制）
 *
 * **注意**：MD5 已不适用于安全场景，仅建议用于校验和/指纹等非加密用途。
 *
 * ```kotlin
 * "hello".md5()  // "5d41402abc4b2a76b9719d911017c592"
 * ```
 */
fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * 计算字符串的 SHA-256 摘要（小写64位十六进制）
 *
 * ```kotlin
 * "hello".sha256()  // "2cf24dba5fb0a30e26e83b2ac5b9e29e..."
 * ```
 */
fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// ==================== 字符串工具 ====================

/**
 * 如果为 null 或空白则返回默认值
 *
 * ```kotlin
 * val name: String? = null
 * name.orDefault("匿名")  // "匿名"
 * ```
 */
fun String?.orDefault(default: String = ""): String {
    return if (isNullOrBlank()) default else this
}

/**
 * 截断字符串，超出长度的部分用省略号替代
 *
 * ```kotlin
 * "Hello World".ellipsize(5)  // "Hello…"
 * "Hi".ellipsize(5)           // "Hi"
 * ```
 *
 * @param maxLength 最大保留字符数（不含 [suffix]）
 * @param suffix 省略后缀，默认 "…"
 */
fun String.ellipsize(maxLength: Int, suffix: String = "…"): String {
    return if (length <= maxLength) this else take(maxLength) + suffix
}
