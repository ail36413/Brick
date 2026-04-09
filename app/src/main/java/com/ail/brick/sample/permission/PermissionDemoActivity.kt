package com.ail.brick.sample.permission

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ail.brick.permission.BrickPermission
import com.ail.brick.permission.hasPermission
import com.ail.brick.permission.hasPermissions
import com.ail.brick.permission.requestPermissions
import com.ail.brick.permission.requirePermissions
import com.ail.brick.sample.databinding.ActivityPermissionDemoBinding
import kotlinx.coroutines.launch

/**
 * brick-permission 演示页面
 *
 * 展示功能：
 * - 权限检查（单个/多个）
 * - 协程方式请求权限
 * - 扩展函数方式请求权限
 * - 打开应用设置页
 */
class PermissionDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        // ---- 权限检查 ----
        binding.btnCheckCamera.setOnClickListener {
            val granted = hasPermission(Manifest.permission.CAMERA)
            showResult(
                "相机权限检查:\n" +
                "CAMERA = ${if (granted) "✅ 已授予" else "❌ 未授予"}"
            )
        }

        binding.btnCheckLocation.setOnClickListener {
            val fine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            val both = hasPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            showResult(
                "定位权限检查:\n" +
                "FINE_LOCATION = ${if (fine) "✅" else "❌"}\n" +
                "COARSE_LOCATION = ${if (coarse) "✅" else "❌"}\n" +
                "全部授予 = ${if (both) "✅" else "❌"}"
            )
        }

        // ---- 协程请求 ----
        binding.btnRequestCamera.setOnClickListener {
            lifecycleScope.launch {
                val result = BrickPermission.request(
                    this@PermissionDemoActivity,
                    Manifest.permission.CAMERA
                )
                showResult(
                    "相机权限请求结果:\n" +
                    "isAllGranted = ${result.isAllGranted}\n" +
                    "granted = ${result.granted}\n" +
                    "denied = ${result.denied}\n" +
                    "permanentlyDenied = ${result.permanentlyDenied}"
                )
            }
        }

        binding.btnRequestMultiple.setOnClickListener {
            lifecycleScope.launch {
                val result = BrickPermission.request(
                    this@PermissionDemoActivity,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
                showResult(
                    "多权限请求结果:\n" +
                    "isAllGranted = ${result.isAllGranted}\n" +
                    "hasPermanentlyDenied = ${result.hasPermanentlyDenied}\n\n" +
                    "granted: ${result.granted.joinToString { it.substringAfterLast('.') }}\n" +
                    "denied: ${result.denied.joinToString { it.substringAfterLast('.') }}\n" +
                    "permanentlyDenied: ${result.permanentlyDenied.joinToString { it.substringAfterLast('.') }}"
                )
            }
        }

        // ---- 扩展函数方式 ----
        binding.btnExtRequest.setOnClickListener {
            requirePermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                onGranted = {
                    showResult("扩展函数方式:\n✅ 定位权限已授予")
                },
                onDenied = { result ->
                    showResult(
                        "扩展函数方式:\n❌ 定位权限被拒绝\n" +
                        "permanentlyDenied = ${result.hasPermanentlyDenied}\n\n" +
                        "→ 被永久拒绝时需引导用户到设置页手动开启"
                    )
                }
            )
        }

        binding.btnOpenSettings.setOnClickListener {
            BrickPermission.openAppSettings(this)
            showResult("已打开应用设置页\n→ 用户可在此手动开启权限")
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
