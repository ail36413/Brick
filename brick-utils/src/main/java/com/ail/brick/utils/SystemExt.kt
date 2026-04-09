package com.ail.brick.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast

// ==================== 剪贴板 ====================

/**
 * 复制文本到系统剪贴板
 *
 * @param text 要复制的文本
 * @param label 剪贴板标签（用户不可见），默认 "text"
 */
fun Context.copyToClipboard(text: String, label: String = "text") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

/**
 * 获取系统剪贴板中的文本
 *
 * @return 剪贴板文本，无内容时返回 null
 */
fun Context.getClipboardText(): String? {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
}

// ==================== 软键盘 ====================

/**
 * 显示软键盘并聚焦当前 EditText
 */
fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * 隐藏软键盘
 */
fun Activity.hideKeyboard() {
    val view = currentFocus ?: window.decorView
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

// ==================== Toast ====================

/**
 * 显示短时 Toast
 *
 * ```kotlin
 * context.toast("保存成功")
 * ```
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * 显示长时 Toast
 */
fun Context.toastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// ==================== 系统 Intent ====================

/**
 * 跳转系统拨号盘
 *
 * @param phone 电话号码
 */
fun Context.dial(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
}

/**
 * 使用系统浏览器打开 URL
 *
 * @param url 网页地址
 */
fun Context.openBrowser(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/**
 * 调用系统分享面板分享文本
 *
 * ```kotlin
 * context.shareText("推荐一个好用的库", "分享到")
 * ```
 *
 * @param text 要分享的文本内容
 * @param title 分享面板标题，默认 "分享"
 */
fun Context.shareText(text: String, title: String = "分享") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

/**
 * 跳转当前应用的系统设置页（常用于权限被拒后引导用户手动开启）
 *
 * ```kotlin
 * context.openAppSettings()
 * ```
 */
fun Context.openAppSettings() {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}

// ==================== 应用信息 ====================

/**
 * 获取当前应用版本名（如 "1.0.0"）
 */
fun Context.appVersionName(): String {
    return packageManager.getPackageInfo(packageName, 0).versionName ?: ""
}

/**
 * 获取当前应用版本号
 *
 * API 28+ 使用 [longVersionCode]，低版本使用 [versionCode]。
 */
fun Context.appVersionCode(): Long {
    val info = packageManager.getPackageInfo(packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION") // PackageInfo.versionCode is needed for API < 28 compat
        info.versionCode.toLong()
    }
}
