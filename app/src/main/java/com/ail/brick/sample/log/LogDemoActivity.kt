package com.ail.brick.sample.log

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.log.BrickLogger
import com.ail.brick.log.LogFileManager
import com.ail.brick.sample.databinding.ActivityLogDemoBinding

/**
 * brick-log 演示页面
 *
 * 展示功能：
 * - 各级别日志输出（V/D/I/W/E）
 * - Lambda 延迟拼接
 * - JSON 格式化输出
 * - 文件日志写入与管理
 */
class LogDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogDemoBinding
    private var logFileDir: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        logFileDir = "${cacheDir.absolutePath}/logs"
        setupButtons()
    }

    private fun setupButtons() {
        // ---- 日志输出 ----
        binding.btnLogLevels.setOnClickListener {
            BrickLogger.v("Verbose 级别日志")
            BrickLogger.d("Debug 级别日志")
            BrickLogger.i("Info 级别日志")
            BrickLogger.w("Warning 级别日志")
            BrickLogger.e("Error 级别日志")
            showResult(
                "已输出各级别日志，请查看 Logcat:\n" +
                "V → Verbose\n" +
                "D → Debug\n" +
                "I → Info\n" +
                "W → Warning\n" +
                "E → Error"
            )
        }

        binding.btnLogLambda.setOnClickListener {
            val data = mapOf("user" to "Brick", "action" to "demo", "ts" to System.currentTimeMillis())
            BrickLogger.d { "Lambda 日志: $data" }
            BrickLogger.i { "当前时间: ${System.currentTimeMillis()}" }
            showResult(
                "Lambda 延迟拼接日志:\n" +
                "→ 日志关闭时不会执行 Lambda，零开销\n" +
                "→ 已输出到 Logcat，请查看"
            )
        }

        binding.btnLogJson.setOnClickListener {
            val json = """
                {
                    "name": "Brick",
                    "version": "1.0.2",
                    "modules": ["net", "utils", "ui", "image", "arch", "store", "log", "data", "permission"],
                    "config": {
                        "minSdk": 24,
                        "targetSdk": 35,
                        "kotlin": "2.0.21"
                    }
                }
            """.trimIndent()
            BrickLogger.json(json, "DemoJSON")
            showResult("JSON 格式化输出:\n已输出到 Logcat，将显示为带边框的缩进格式")
        }

        // ---- 文件日志 ----
        binding.btnFileLogWrite.setOnClickListener {
            repeat(5) { i ->
                BrickLogger.i("文件日志测试 #${i + 1} — ${System.currentTimeMillis()}")
            }
            BrickLogger.w("这是一条 Warning 级别的文件日志")
            BrickLogger.e("这是一条 Error 级别的文件日志")
            showResult("已写入 7 条日志\n日志将异步写入文件: $logFileDir")
        }

        binding.btnFileLogInfo.setOnClickListener {
            val files = LogFileManager.getLogFiles(logFileDir)
            val totalSize = LogFileManager.getTotalSize(logFileDir)
            val sb = StringBuilder()
            sb.appendLine("日志文件目录: $logFileDir")
            sb.appendLine("文件数量: ${files.size}")
            sb.appendLine("总大小: ${formatSize(totalSize)}")
            if (files.isNotEmpty()) {
                sb.appendLine("\n文件列表:")
                files.forEach { file ->
                    sb.appendLine("  ${file.name} (${formatSize(file.length())})")
                }
            }
            showResult(sb.toString())
        }

        binding.btnFileLogClear.setOnClickListener {
            LogFileManager.clearAll(logFileDir)
            showResult("已清除所有日志文件")
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> "${"%.2f".format(bytes / 1024.0 / 1024.0)}MB"
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
