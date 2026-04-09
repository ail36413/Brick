package com.ail.brick.sample.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.sample.databinding.ActivityUtilsDemoBinding
import com.ail.brick.utils.*

/**
 * brick-utils 演示页面
 */
class UtilsDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUtilsDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUtilsDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnDpConvert.setOnClickListener {
            val px16dp = 16.dp
            val px14sp = sp2px(14f)
            showResult(
                "16.dp = ${px16dp}px\n" +
                "sp2px(14f) = ${px14sp}px"
            )
        }

        binding.btnScreenInfo.setOnClickListener {
            showResult(
                "屏幕宽度: ${screenWidth}px\n" +
                "屏幕高度: ${screenHeight}px\n" +
                "状态栏高度: ${statusBarHeight}px\n" +
                "导航栏高度: ${navigationBarHeight}px"
            )
        }

        binding.btnStringValidate.setOnClickListener {
            val phone = "13812345678"
            val email = "test@example.com"
            val badPhone = "1234"
            showResult(
                "\"$phone\".isPhoneNumber = ${phone.isPhoneNumber()}\n" +
                "\"$badPhone\".isPhoneNumber = ${badPhone.isPhoneNumber()}\n" +
                "\"$email\".isEmail = ${email.isEmail()}"
            )
        }

        binding.btnStringMask.setOnClickListener {
            val phone = "13812345678"
            val idCard = "110101199001011234"
            val email = "hello@example.com"
            showResult(
                "手机脱敏: ${phone.maskPhone()}\n" +
                "身份证脱敏: ${idCard.maskIdCard()}\n" +
                "邮箱脱敏: ${email.maskEmail()}\n" +
                "MD5: ${phone.md5().take(16)}..."
            )
        }

        binding.btnClipboard.setOnClickListener {
            copyToClipboard("Brick 剪贴板测试")
            toast("已复制到剪贴板")
            showResult("已将 \"Brick 剪贴板测试\" 复制到剪贴板")
        }

        binding.btnNetwork.setOnClickListener {
            showResult(
                "网络可用: ${isNetworkAvailable()}\n" +
                "WiFi 连接: ${isWifiConnected()}\n" +
                "移动数据: ${isMobileDataConnected()}\n" +
                "网络类型: ${getNetworkTypeName()}"
            )
        }

        binding.btnDate.setOnClickListener {
            val now = currentTimeMillis()
            val fiveMinAgo = now - 5 * 60 * 1000L
            val yesterday = now - 24 * 60 * 60 * 1000L
            showResult(
                "当前时间: ${now.formatDate()}\n" +
                "5分钟前: ${fiveMinAgo.toFriendlyTime()}\n" +
                "昨天: ${yesterday.formatDate("MM-dd HH:mm")}\n" +
                "isToday: ${now.isToday()}\n" +
                "isYesterday(昨天): ${yesterday.isYesterday()}\n" +
                "isSameDay(now,now): ${now.isSameDay(now)}"
            )
        }

        binding.btnCollection.setOnClickListener {
            val list = listOf(1, 2, 3, 2, 4, 1, 5)
            val emptyList = emptyList<Int>()
            val sb = StringBuilder()
            sb.appendLine("原始: $list")
            sb.appendLine("distinctBy: ${list.distinctBy { it }}")
            list.ifNotEmpty { sb.appendLine("ifNotEmpty: size=${it.size}") }
            emptyList.ifNotEmpty { sb.appendLine("不会执行") }
            sb.appendLine("safeJoinToString: ${list.safeJoinToString()}")
            val map = mutableMapOf<String, Int>()
            map.getOrPut("key") { 42 }
            sb.append("getOrPut: $map")
            showResult(sb.toString())
        }

        binding.btnDeviceInfo.setOnClickListener {
            showResult(
                "品牌: $deviceBrand\n" +
                "型号: $deviceModel\n" +
                "制造商: $deviceManufacturer\n" +
                "Android 版本: $osVersion\n" +
                "SDK 版本: $sdkVersion\n" +
                "应用版本: ${appVersionName()} (${appVersionCode()})\n" +
                "摘要: ${deviceSummary()}"
            )
        }

        binding.btnEncode.setOnClickListener {
            val original = "Brick Utils"
            val base64 = original.encodeBase64()
            val decoded = base64.decodeBase64String()
            val bytes = original.toByteArray()
            val hex = bytes.toHexString()
            val hexDecoded = hex.hexToByteArray()
            showResult(
                "原文: $original\n" +
                "Base64 编码: $base64\n" +
                "Base64 解码: $decoded\n" +
                "Hex 编码: $hex\n" +
                "Hex 解码: ${String(hexDecoded)}\n" +
                "URL校验 https://example.com: ${"https://example.com".isUrl()}"
            )
        }

        binding.btnShareText.setOnClickListener {
            shareText("Brick 工具库演示 - 来自 brick-utils", "分享到")
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
        BrickLog.d("UtilsDemo", text)
    }
}
