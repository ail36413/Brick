package com.ail.brick.sample.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ail.brick.sample.databinding.ActivityUiDemoBinding
import com.ail.brick.ui.anim.BrickItemAnimator
import com.ail.brick.ui.anim.pulse
import com.ail.brick.ui.anim.shake
import com.ail.brick.ui.dialog.BrickDialog
import com.ail.brick.ui.recyclerview.DividerDecoration
import com.ail.brick.ui.recyclerview.SimpleAdapter
import com.ail.brick.ui.statelayout.StateLayout
import com.ail.brick.ui.widget.LoadingDialog

/**
 * brick-ui 演示页面
 */
class UiDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUiDemoBinding
    private var badgeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupStateLayout()
        setupDialogs()
        setupWidgets()
        setupNewFeatures()
        setupRecyclerViewDemo()
        setupAnimDemo()
    }

    private fun setupStateLayout() {
        binding.btnLoading.setOnClickListener {
            binding.stateLayout.showLoading()
            showResult("StateLayout → Loading")
        }
        binding.btnEmpty.setOnClickListener {
            binding.stateLayout.showEmpty()
            showResult("StateLayout → Empty")
        }
        binding.btnError.setOnClickListener {
            binding.stateLayout.showError {
                Toast.makeText(this, "重试被点击", Toast.LENGTH_SHORT).show()
                binding.stateLayout.showLoading()
            }
            showResult("StateLayout → Error")
        }
        binding.btnContent.setOnClickListener {
            binding.stateLayout.showContent()
            showResult("StateLayout → Content")
        }
    }

    private fun setupDialogs() {
        binding.btnConfirm.setOnClickListener {
            BrickDialog.confirm(this, "提示", "确定执行此操作吗？") {
                showResult("确认对话框：点击了确定")
            }
        }
        binding.btnInput.setOnClickListener {
            BrickDialog.input(this, "输入昵称", hint = "请输入昵称") { text ->
                showResult("输入对话框：$text")
            }
        }
        binding.btnList.setOnClickListener {
            BrickDialog.list(this, "选择颜色", listOf("红色", "绿色", "蓝色", "黄色")) { index ->
                val colors = listOf("红色", "绿色", "蓝色", "黄色")
                showResult("列表对话框：选择了 ${colors[index]}（index=$index）")
            }
        }
        binding.btnBottomList.setOnClickListener {
            BrickDialog.bottomList(this, "选择操作", listOf("拍照", "从相册选择", "取消")) { index ->
                val actions = listOf("拍照", "从相册选择", "取消")
                showResult("底部列表：选择了 ${actions[index]}")
            }
        }
        binding.btnLoadingDialog.setOnClickListener {
            LoadingDialog.show(this, "加载中...")
            Handler(Looper.getMainLooper()).postDelayed({
                LoadingDialog.dismiss()
                showResult("LoadingDialog 已关闭")
            }, 2000)
        }
    }

    private fun setupWidgets() {
        binding.badgeView.count = 0
        binding.btnBadge.setOnClickListener {
            badgeCount++
            binding.badgeView.count = badgeCount
            showResult("BadgeView count = $badgeCount")
        }
    }

    /**
     * 新增功能演示：FlowLayout、StateLayout 回调、自定义对话框、可取消 LoadingDialog
     */
    private fun setupNewFeatures() {
        // FlowLayout 标签演示
        val tags = listOf("Android", "Kotlin", "Jetpack", "Compose", "MVVM", "Hilt", "Room", "Retrofit")
        val density = resources.displayMetrics.density
        for (tag in tags) {
            val tv = TextView(this).apply {
                text = tag
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.WHITE)
                setBackgroundColor(0xFF6200EE.toInt())
                val h = (12 * density).toInt()
                val v = (6 * density).toInt()
                setPadding(h, v, h, v)
            }
            binding.flowLayout.addView(tv)
        }

        // StateLayout 状态回调演示
        binding.stateLayout.setOnStateChangeListener { oldState, newState ->
            showResult("状态回调: $oldState → $newState")
        }
        binding.btnStateCallback.setOnClickListener {
            // 循环切换状态演示回调
            when (binding.stateLayout.currentState) {
                StateLayout.State.CONTENT -> binding.stateLayout.showLoading()
                StateLayout.State.LOADING -> binding.stateLayout.showEmpty()
                StateLayout.State.EMPTY -> binding.stateLayout.showError { binding.stateLayout.showContent() }
                StateLayout.State.ERROR -> binding.stateLayout.showContent()
            }
        }

        // 自定义布局对话框
        binding.btnCustomDialog.setOnClickListener {
            val customView = TextView(this).apply {
                text = "这是自定义对话框的内容视图\n支持传入任意 View 或布局资源 ID"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                val dp24 = (24 * density).toInt()
                setPadding(dp24, dp24, dp24, dp24)
            }
            BrickDialog.custom(
                context = this,
                title = "自定义对话框",
                view = customView,
                onConfirm = { showResult("自定义对话框：确认") }
            )
        }

        // 可取消 LoadingDialog
        binding.btnCancelableLoading.setOnClickListener {
            LoadingDialog.show(this, "请稍候…", cancelable = true) {
                showResult("LoadingDialog 被用户取消")
            }
            Handler(Looper.getMainLooper()).postDelayed({
                LoadingDialog.dismiss()
            }, 5000)
        }
    }

    /**
     * RecyclerView 组件演示：SimpleAdapter + DividerDecoration + ItemAnimator
     */
    private fun setupRecyclerViewDemo() {
        // 简易列表 ViewBinding 模拟
        class TextVB(val tv: TextView) : androidx.viewbinding.ViewBinding {
            override fun getRoot(): View = tv
        }

        val diffCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(old: String, new: String) = old == new
            override fun areContentsTheSame(old: String, new: String) = old == new
        }

        val adapter = SimpleAdapter<TextVB, String>(
            inflate = { _, parent, _ ->
                TextVB(TextView(parent.context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    val dp = (12 * resources.displayMetrics.density).toInt()
                    setPadding(dp, dp / 2, dp, dp / 2)
                })
            },
            diffCallback = diffCallback
        ) { binding, item, _ ->
            binding.tv.text = item
        }

        binding.rvDemo.layoutManager = LinearLayoutManager(this)
        binding.rvDemo.addItemDecoration(DividerDecoration(height = 1, color = Color.LTGRAY, paddingStart = 16, paddingEnd = 16))
        binding.rvDemo.adapter = adapter
        adapter.submitList((1..8).map { "列表项 #$it（DividerDecoration + ItemAnimator）" })

        adapter.setOnItemClickListener { item, pos ->
            showResult("点击: $item (pos=$pos)")
        }

        // Item 入场动画
        binding.btnItemAnim.setOnClickListener {
            val rv = binding.rvDemo
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i)
                BrickItemAnimator.animateItem(child, i, BrickItemAnimator.AnimType.FADE_SLIDE_UP)
            }
            showResult("Item 入场动画已触发")
        }
    }

    /**
     * BrickAnim 动画演示
     */
    private fun setupAnimDemo() {
        binding.btnAnimPulse.setOnClickListener {
            it.pulse()
            showResult("Pulse 脉冲动画")
        }
        binding.btnAnimShake.setOnClickListener {
            it.shake()
            showResult("Shake 抖动动画")
        }
    }

    private fun showResult(text: String) {
        binding.tvResult.text = text
    }
}
