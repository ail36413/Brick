package com.ail.brick.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 权限相关扩展函数，提供更便捷的调用方式。
 */

/**
 * 检查指定权限是否已授予。
 *
 * ```kotlin
 * if (context.hasPermission(Manifest.permission.CAMERA)) { ... }
 * ```
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * 检查多个权限是否全部已授予。
 *
 * ```kotlin
 * if (context.hasPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) { ... }
 * ```
 */
fun Context.hasPermissions(vararg permissions: String): Boolean {
    return permissions.all { hasPermission(it) }
}

/**
 * Activity 扩展：请求权限并通过回调处理结果。
 *
 * ```kotlin
 * requestPermissions(Manifest.permission.CAMERA) { result ->
 *     if (result.isAllGranted) openCamera()
 * }
 * ```
 *
 * @param permissions 要请求的权限
 * @param callback 结果回调
 */
fun FragmentActivity.requestPermissions(
    vararg permissions: String,
    callback: (PermissionResult) -> Unit
) {
    lifecycleScope.launch {
        val result = BrickPermission.request(this@requestPermissions, *permissions)
        callback(result)
    }
}

/**
 * Fragment 扩展：请求权限并通过回调处理结果。
 *
 * ```kotlin
 * requestPermissions(Manifest.permission.CAMERA) { result ->
 *     if (result.isAllGranted) openCamera()
 * }
 * ```
 *
 * @param permissions 要请求的权限
 * @param callback 结果回调
 */
fun Fragment.requestPermissions(
    vararg permissions: String,
    callback: (PermissionResult) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        val result = BrickPermission.request(this@requestPermissions, *permissions)
        callback(result)
    }
}

/**
 * Activity 扩展：请求权限，成功则执行 [onGranted]，失败则执行 [onDenied]。
 *
 * ```kotlin
 * requirePermissions(
 *     Manifest.permission.CAMERA,
 *     onGranted = { openCamera() },
 *     onDenied = { result ->
 *         if (result.hasPermanentlyDenied) {
 *             showSettingsGuide()
 *         } else {
 *             showToast("需要相机权限")
 *         }
 *     }
 * )
 * ```
 *
 * @param permissions 要请求的权限
 * @param onGranted 全部权限授予后的回调
 * @param onDenied 任意权限被拒绝后的回调
 */
fun FragmentActivity.requirePermissions(
    vararg permissions: String,
    onGranted: () -> Unit,
    onDenied: (PermissionResult) -> Unit = {}
) {
    lifecycleScope.launch {
        val result = BrickPermission.request(this@requirePermissions, *permissions)
        if (result.isAllGranted) {
            onGranted()
        } else {
            onDenied(result)
        }
    }
}

/**
 * Fragment 扩展：请求权限，成功则执行 [onGranted]，失败则执行 [onDenied]。
 */
fun Fragment.requirePermissions(
    vararg permissions: String,
    onGranted: () -> Unit,
    onDenied: (PermissionResult) -> Unit = {}
) {
    viewLifecycleOwner.lifecycleScope.launch {
        val result = BrickPermission.request(this@requirePermissions, *permissions)
        if (result.isAllGranted) {
            onGranted()
        } else {
            onDenied(result)
        }
    }
}
