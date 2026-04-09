package com.ail.brick.utils

import android.os.Build

// ==================== 设备基本信息 ====================

/**
 * 设备品牌（如 "Xiaomi"、"HUAWEI"、"samsung"）
 *
 * ```kotlin
 * val brand = deviceBrand  // "Xiaomi"
 * ```
 */
val deviceBrand: String get() = Build.BRAND

/**
 * 设备型号（如 "Pixel 7"、"Mi 14"）
 *
 * ```kotlin
 * val model = deviceModel  // "Pixel 7"
 * ```
 */
val deviceModel: String get() = Build.MODEL

/**
 * 设备制造商（如 "Google"、"Xiaomi"）
 */
val deviceManufacturer: String get() = Build.MANUFACTURER

/**
 * Android 系统版本号（如 "14"、"13"）
 *
 * ```kotlin
 * val version = osVersion  // "14"
 * ```
 */
val osVersion: String get() = Build.VERSION.RELEASE

/**
 * Android SDK 版本号（如 34、33）
 *
 * ```kotlin
 * if (sdkVersion >= Build.VERSION_CODES.TIRAMISU) { /* Android 13+ */ }
 * ```
 */
val sdkVersion: Int get() = Build.VERSION.SDK_INT

/**
 * 获取完整的设备信息摘要（适合日志和错误上报）
 *
 * ```kotlin
 * BrickLog.i("Device", deviceSummary())
 * // "Xiaomi Mi 14 | Android 14 (SDK 34)"
 * ```
 *
 * @return 格式化的设备信息字符串
 */
fun deviceSummary(): String {
    return "$deviceBrand $deviceModel | Android $osVersion (SDK $sdkVersion)"
}
