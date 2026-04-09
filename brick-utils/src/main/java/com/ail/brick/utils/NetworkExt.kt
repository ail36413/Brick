package com.ail.brick.utils

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 判断当前是否有可用的网络连接
 *
 * 同时校验 `INTERNET` 和 `VALIDATED` 能力，确保网络真正可用。
 *
 * @return true 网络可用，false 无网络
 */
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * 判断当前是否通过 WiFi 连接
 *
 * @return true 当前为 WiFi 网络
 */
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.isWifiConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

/**
 * 判断当前是否通过移动数据连接
 *
 * @return true 当前为移动数据网络
 */
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.isMobileDataConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

/**
 * 获取当前网络类型名称
 *
 * @return "WiFi" / "Mobile" / "Ethernet" / "None"
 */
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.getNetworkTypeName(): String {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return "None"
    val caps = cm.getNetworkCapabilities(network) ?: return "None"
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "None"
    }
}

/**
 * 观察网络连接状态变化（基于 Flow）
 *
 * 使用 [ConnectivityManager.NetworkCallback] 监听网络变化，
 * 配合 `distinctUntilChanged` 避免重复事件。
 *
 * ```kotlin
 * // 在 LifecycleOwner 中安全收集
 * lifecycleScope.launch {
 *     repeatOnLifecycle(Lifecycle.State.STARTED) {
 *         context.observeNetworkState().collect { available ->
 *             updateUI(available)
 *         }
 *     }
 * }
 * ```
 *
 * @return 发射 Boolean 的 Flow：true = 有网络，false = 无网络
 */
@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.observeNetworkState(): Flow<Boolean> = callbackFlow {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // 跟踪所有活跃网络，避免多网络设备上单网络断开时误判为离线
    val activeNetworks = mutableSetOf<Network>()
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            activeNetworks.add(network)
            trySend(true)
        }
        override fun onLost(network: Network) {
            activeNetworks.remove(network)
            trySend(activeNetworks.isNotEmpty())
        }
        override fun onUnavailable() {
            trySend(activeNetworks.isNotEmpty())
        }
    }
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    cm.registerNetworkCallback(request, callback)

    // 发送初始状态
    trySend(isNetworkAvailable())

    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
