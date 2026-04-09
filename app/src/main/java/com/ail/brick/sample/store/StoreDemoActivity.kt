package com.ail.brick.sample.store

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.sample.databinding.ActivityStoreDemoBinding
import com.ail.brick.store.MmkvDelegate

/**
 * brick-store 演示页面
 *
 * 展示功能：
 * - MMKV 属性委托读写（String / Int / Boolean）
 * - 加密存储（AES-CFB）
 * - 存储信息查看与清空
 */
class StoreDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStoreDemoBinding
    private var insertCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoreDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        // ---- MMKV 读写 ----
        binding.btnWriteString.setOnClickListener {
            insertCount++
            DemoStore.username = "Brick_$insertCount"
            DemoStore.loginCount = insertCount
            DemoStore.isVip = insertCount % 2 == 0
            showResult(
                "写入成功:\n" +
                "username = \"${DemoStore.username}\"\n" +
                "loginCount = ${DemoStore.loginCount}\n" +
                "isVip = ${DemoStore.isVip}"
            )
        }

        binding.btnReadAll.setOnClickListener {
            showResult(
                "读取结果:\n" +
                "username = \"${DemoStore.username}\"\n" +
                "loginCount = ${DemoStore.loginCount}\n" +
                "isVip = ${DemoStore.isVip}\n" +
                "score = ${DemoStore.score}\n" +
                "tags = ${DemoStore.tags}"
            )
        }

        // ---- 加密存储 ----
        binding.btnEncryptWrite.setOnClickListener {
            SecureStore.secretToken = "encrypted_token_${System.currentTimeMillis()}"
            showResult("加密写入成功:\nsecretToken = \"${SecureStore.secretToken}\"")
        }

        binding.btnEncryptRead.setOnClickListener {
            showResult("加密读取:\nsecretToken = \"${SecureStore.secretToken}\"")
        }

        // ---- 存储管理 ----
        binding.btnStorageInfo.setOnClickListener {
            val keys = DemoStore.allKeys()
            showResult(
                "存储信息:\n" +
                "键总数: ${keys.size}\n" +
                "所有键: ${keys.joinToString()}\n" +
                "存储大小: ${DemoStore.totalSize()} bytes\n" +
                "包含 username: ${DemoStore.contains("username")}"
            )
        }

        binding.btnClearStore.setOnClickListener {
            DemoStore.clear()
            showResult("已清空 DemoStore\n读取 username = \"${DemoStore.username}\"")
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }

    /** 演示用普通存储 */
    object DemoStore : MmkvDelegate(mmapId = "demo_store") {
        var username by string("username", "")
        var loginCount by int("login_count", 0)
        var isVip by boolean("is_vip", false)
        var score by float("score", 0f)
        var tags by stringSet("tags", setOf("Android", "Kotlin"))
    }

    /** 演示用加密存储 */
    object SecureStore : MmkvDelegate(mmapId = "secure_store", cryptKey = "brick_demo_key") {
        var secretToken by string("secret_token", "")
    }
}
