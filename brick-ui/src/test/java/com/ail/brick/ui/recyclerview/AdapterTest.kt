package com.ail.brick.ui.recyclerview

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SimpleAdapter / MultiTypeAdapter 的 Robolectric 测试。
 *
 * 覆盖场景：
 * - 基本数据提交与 item count
 * - 空视图切换
 * - DiffUtil 差量更新
 * - 点击监听器
 * - 多类型注册与绑定
 * - DSL 构建器
 * - 未注册类型抛异常
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdapterTest {

    private lateinit var activity: Activity
    private lateinit var recyclerView: RecyclerView

    /** 简单的 ViewBinding 模拟 */
    class TestBinding(val textView: TextView) : ViewBinding {
        override fun getRoot(): View = textView
    }

    private val diffCallback = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(old: String, new: String) = old == new
        override fun areContentsTheSame(old: String, new: String) = old == new
    }

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
        }
    }

    // ==================== SimpleAdapter ====================

    @Test
    fun `SimpleAdapter submits list and reports itemCount`() {
        val adapter = SimpleAdapter<TestBinding, String>(
            inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
            diffCallback = diffCallback,
            bind = { binding, item, _ -> binding.textView.text = item }
        )
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A", "B", "C"))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `SimpleAdapter empty list results in zero items`() {
        val adapter = SimpleAdapter<TestBinding, String>(
            inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
            diffCallback = diffCallback,
            bind = { _, _, _ -> }
        )
        recyclerView.adapter = adapter
        adapter.submitList(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `SimpleAdapter emptyView shown when no data`() {
        val emptyView = TextView(activity).apply { text = "Empty"; visibility = View.GONE }
        val adapter = SimpleAdapter<TestBinding, String>(
            inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
            diffCallback = diffCallback,
            bind = { _, _, _ -> }
        )
        recyclerView.adapter = adapter
        adapter.setEmptyView(emptyView)

        // 初始无数据 → emptyView 可见（setEmptyView 内部调用 toggleEmptyView）
        assertEquals(View.VISIBLE, emptyView.visibility)
    }

    @Test
    fun `SimpleAdapter click listener fires`() {
        var clicked: String? = null
        var clickedPos = -1
        val adapter = SimpleAdapter<TestBinding, String>(
            inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
            diffCallback = diffCallback,
            bind = { binding, item, _ -> binding.textView.text = item }
        )
        adapter.setOnItemClickListener { item, pos ->
            clicked = item
            clickedPos = pos
        }
        recyclerView.adapter = adapter
        adapter.submitList(listOf("X", "Y"))

        // Force layout so ViewHolders are created
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        )
        recyclerView.layout(0, 0, 500, 500)

        val holder = recyclerView.findViewHolderForAdapterPosition(0)
        holder?.itemView?.performClick()

        assertEquals("X", clicked)
        assertEquals(0, clickedPos)
    }

    // ==================== MultiTypeAdapter ====================

    data class TextItem(val id: Int, val text: String)
    data class NumberItem(val id: Int, val value: Int)

    @Test
    fun `MultiTypeAdapter registers types and reports itemCount`() {
        val adapter = MultiTypeAdapter()
        adapter.register(TextItem::class,
            create = { parent -> TestBinding(TextView(parent.context)) },
            bind = { binding, item, _ ->
                (binding as TestBinding).textView.text = (item as TextItem).text
            }
        )
        adapter.submitList(listOf(TextItem(1, "Hello"), TextItem(2, "World")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `MultiTypeAdapter with DiffUtil updates correctly`() {
        val adapter = MultiTypeAdapter(
            itemDiff = { old, new ->
                (old as? TextItem)?.id == (new as? TextItem)?.id
            },
            contentDiff = { old, new -> old == new }
        )
        adapter.register(TextItem::class,
            create = { parent -> TestBinding(TextView(parent.context)) },
            bind = { _, _, _ -> }
        )
        recyclerView.adapter = adapter

        adapter.submitList(listOf(TextItem(1, "A"), TextItem(2, "B")))
        assertEquals(2, adapter.itemCount)

        adapter.submitList(listOf(TextItem(1, "A"), TextItem(2, "B"), TextItem(3, "C")))
        assertEquals(3, adapter.itemCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `MultiTypeAdapter throws for unregistered type`() {
        val adapter = MultiTypeAdapter()
        adapter.register(TextItem::class,
            create = { parent -> TestBinding(TextView(parent.context)) },
            bind = { _, _, _ -> }
        )
        recyclerView.adapter = adapter
        adapter.submitList(listOf(NumberItem(1, 42)))
        // Force getItemViewType
        adapter.getItemViewType(0)
    }

    @Test
    fun `MultiTypeAdapter getItem returns correct item`() {
        val adapter = MultiTypeAdapter()
        adapter.register(TextItem::class,
            create = { parent -> TestBinding(TextView(parent.context)) },
            bind = { _, _, _ -> }
        )
        adapter.submitList(listOf(TextItem(1, "Hello")))
        assertEquals(TextItem(1, "Hello"), adapter.getItem(0))
    }

    @Test
    fun `multiTypeAdapter DSL builder works`() {
        val adapter = multiTypeAdapter {
            itemDiff { old, new -> old == new }
            contentDiff { old, new -> old == new }
            register<TextItem, TestBinding>(
                inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
                bind = { binding, item, _ -> binding.textView.text = item.text }
            )
        }
        adapter.submitList(listOf(TextItem(1, "DSL")))
        assertEquals(1, adapter.itemCount)
    }
}
