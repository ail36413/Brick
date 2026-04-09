package com.ail.brick.sample

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ail.brick.sample.databinding.ActivityMainBinding
import com.ail.brick.sample.databinding.ItemDemoEntryBinding
import com.ail.brick.sample.net.NetDemoActivity
import com.ail.brick.sample.utils.UtilsDemoActivity
import com.ail.brick.sample.ui.UiDemoActivity
import com.ail.brick.sample.image.ImageDemoActivity
import com.ail.brick.sample.arch.ArchDemoActivity
import com.ail.brick.sample.store.StoreDemoActivity
import com.ail.brick.sample.log.LogDemoActivity
import com.ail.brick.sample.data.DataDemoActivity
import com.ail.brick.sample.permission.PermissionDemoActivity
import com.ail.brick.sample.startup.StartupDemoActivity
import com.ail.brick.sample.anim.AnimDemoActivity
import com.ail.brick.sample.nav.NavDemoActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主页面 — 展示各库演示入口
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val entries = listOf(
            DemoEntry("brick-net", "网络库演示", "HTTP 请求、文件上传下载、WebSocket"),
            DemoEntry("brick-utils", "工具库演示", "扩展函数、SP 委托、日志、网络状态"),
            DemoEntry("brick-ui", "UI 组件库演示", "StateLayout、TitleBar、SimpleAdapter、Dialog"),
            DemoEntry("brick-image", "图片加载库演示", "图片加载、圆形/圆角、变换、预加载"),
            DemoEntry("brick-arch", "架构库演示", "MVVM / MVI 模式、FlowEventBus"),
            DemoEntry("brick-store", "键值存储演示", "MMKV 属性委托、加密存储、SP 迁移"),
            DemoEntry("brick-log", "日志系统演示", "Timber 增强日志、文件日志、崩溃收集"),
            DemoEntry("brick-data", "数据库演示", "Room DSL 构建、BaseDao CRUD、DbResult"),
            DemoEntry("brick-permission", "权限管理演示", "协程权限请求、扩展函数、设置跳转"),
            DemoEntry("brick-startup", "启动优化演示", "四级初始化优先级、启动报告、延迟初始化"),
            DemoEntry("brick-anim", "动画演示", "淡入淡出、滑入滑出、缩放、抖动、组合动画"),
            DemoEntry("brick-nav", "导航演示", "纯代码路由、零 XML、转场动画、返回栈管理"),
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = DemoAdapter(entries) { entry ->
                when (entry.module) {
                    "brick-net" -> startActivity(Intent(this@MainActivity, NetDemoActivity::class.java))
                    "brick-utils" -> startActivity(Intent(this@MainActivity, UtilsDemoActivity::class.java))
                    "brick-ui" -> startActivity(Intent(this@MainActivity, UiDemoActivity::class.java))
                    "brick-image" -> startActivity(Intent(this@MainActivity, ImageDemoActivity::class.java))
                    "brick-arch" -> startActivity(Intent(this@MainActivity, ArchDemoActivity::class.java))
                    "brick-store" -> startActivity(Intent(this@MainActivity, StoreDemoActivity::class.java))
                    "brick-log" -> startActivity(Intent(this@MainActivity, LogDemoActivity::class.java))
                    "brick-data" -> startActivity(Intent(this@MainActivity, DataDemoActivity::class.java))
                    "brick-permission" -> startActivity(Intent(this@MainActivity, PermissionDemoActivity::class.java))
                    "brick-startup" -> startActivity(Intent(this@MainActivity, StartupDemoActivity::class.java))
                    "brick-anim" -> startActivity(Intent(this@MainActivity, AnimDemoActivity::class.java))
                    "brick-nav" -> startActivity(Intent(this@MainActivity, NavDemoActivity::class.java))
                    else -> Toast.makeText(this@MainActivity, "${entry.title} 开发中", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** 演示条目数据类 */
    data class DemoEntry(
        val module: String,
        val title: String,
        val description: String,
    )

    /** 演示列表适配器 */
    class DemoAdapter(
        private val entries: List<DemoEntry>,
        private val onClick: (DemoEntry) -> Unit,
    ) : RecyclerView.Adapter<DemoAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemDemoEntryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDemoEntryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = entries[position]
            holder.binding.tvTitle.text = entry.title
            holder.binding.tvDescription.text = entry.description
            holder.binding.root.setOnClickListener { onClick(entry) }
        }

        override fun getItemCount() = entries.size
    }
}
