package com.ail.brick.utils

import android.util.Base64

// ==================== Base64 编解码 ====================

/**
 * 将字节数组编码为 Base64 字符串
 *
 * ```kotlin
 * "hello".toByteArray().toBase64()  // "aGVsbG8="
 * ```
 *
 * @param flags Base64 标志位，默认 [Base64.NO_WRAP]（不换行）
 * @return Base64 编码的字符串
 */
fun ByteArray.toBase64(flags: Int = Base64.NO_WRAP): String {
    return Base64.encodeToString(this, flags)
}

/**
 * 将字符串进行 Base64 编码
 *
 * ```kotlin
 * "hello".encodeBase64()  // "aGVsbG8="
 * ```
 */
fun String.encodeBase64(flags: Int = Base64.NO_WRAP): String {
    return toByteArray().toBase64(flags)
}

/**
 * 将 Base64 字符串解码为字节数组
 *
 * ```kotlin
 * "aGVsbG8=".decodeBase64()  // [104, 101, 108, 108, 111]
 * ```
 *
 * @param flags Base64 标志位，默认 [Base64.NO_WRAP]
 * @return 解码后的字节数组
 */
fun String.decodeBase64(flags: Int = Base64.NO_WRAP): ByteArray {
    return Base64.decode(this, flags)
}

/**
 * 将 Base64 字符串解码为普通字符串
 *
 * ```kotlin
 * "aGVsbG8=".decodeBase64String()  // "hello"
 * ```
 */
fun String.decodeBase64String(flags: Int = Base64.NO_WRAP): String {
    return String(decodeBase64(flags))
}

// ==================== Hex 编解码 ====================

/**
 * 将字节数组编码为十六进制字符串（小写）
 *
 * ```kotlin
 * byteArrayOf(0x0A, 0xFF.toByte()).toHexString()  // "0aff"
 * ```
 */
fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

/**
 * 将十六进制字符串解码为字节数组
 *
 * ```kotlin
 * "0aff".hexToByteArray()  // [10, -1]
 * ```
 *
 * @throws IllegalArgumentException 长度为奇数或包含非法字符
 */
fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length, got $length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
