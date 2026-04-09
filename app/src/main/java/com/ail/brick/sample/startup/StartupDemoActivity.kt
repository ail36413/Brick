package com.ail.brick.sample.startup

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.sample.databinding.ActivityStartupDemoBinding
import com.ail.brick.startup.AppInitializer
import com.ail.brick.startup.BrickStartup
import com.ail.brick.startup.InitPriority

/**
 * brick-startup 演示页面
 *
 * 展示功能：
 * - 查看初始化报告（各组件耗时）
 * - 同步初始化总耗时统计
 * - 四级优先级说明
 * - 模拟延迟初始化效果
 */
class StartupDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartupDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartupDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        binding.btnReport.setOnClickListener {
            val report = BrickStartup.getReport()
            if (report.isEmpty()) {
                showResult("暂无初始化报告\n（BrickStartup 未初始化或未注册任何初始化器）")
                return@setOnClickListener
            }
            val sb = StringBuilder()
            sb.appendLine("┌── 初始化报告 ──")
            report.forEach { r ->
                val status = if (r.success) "✓" else "✗ ${r.error?.message}"
                sb.appendLine("│ ${r.name}")
                sb.appendLine("│   优先级: ${r.priority}")
                sb.appendLine("│   耗时: ${r.costMillis}ms  $status")
            }
            sb.appendLine("├────────────────")
            sb.appendLine("│ 同步总耗时: ${BrickStartup.getSyncCostMillis()}ms")
            sb.append("└────────────────")
            showResult(sb.toString())
        }

        binding.btnSyncCost.setOnClickListener {
            val cost = BrickStartup.getSyncCostMillis()
            val total = BrickStartup.getReport().sumOf { it.costMillis }
            showResult(
                "同步初始化（IMMEDIATELY + NORMAL）:\n" +
                "  总耗时: ${cost}ms\n\n" +
                "所有初始化器耗时合计: ${total}ms\n\n" +
                "→ DEFERRED 和 BACKGROUND 不影响启动速度"
            )
        }

        binding.btnPriorityInfo.setOnClickListener {
            showResult(
                "四级初始化优先级:\n\n" +
                "1. IMMEDIATELY（立即）\n" +
                "   主线程同步，最先执行\n" +
                "   适合：崩溃收集、日志\n\n" +
                "2. NORMAL（正常）\n" +
                "   主线程同步，常规顺序\n" +
                "   适合：网络、图片、存储\n\n" +
                "3. DEFERRED（延迟）\n" +
                "   主线程空闲时执行\n" +
                "   适合：统计上报、推送、预加载\n\n" +
                "4. BACKGROUND（后台）\n" +
                "   子线程异步执行\n" +
                "   适合：缓存清理、数据预热"
            )
        }

        binding.btnDeferredDemo.setOnClickListener {
            showResult("模拟延迟初始化...\n等待主线程空闲...")
            Looper.myQueue().addIdleHandler {
                Handler(Looper.getMainLooper()).post {
                    showResult(
                        "延迟初始化已触发！\n\n" +
                        "→ 通过 Looper.myQueue().addIdleHandler\n" +
                        "→ 在主线程空闲时执行\n" +
                        "→ 不阻塞首屏渲染\n" +
                        "→ 适合非关键组件初始化"
                    )
                }
                false
            }
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
