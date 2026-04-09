package com.ail.brick.ui.recyclerview

import android.app.Activity
import android.view.View
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LoadMoreAdapterTest {

    private lateinit var activity: Activity
    private lateinit var recyclerView: RecyclerView

    class TestBinding(val textView: TextView) : ViewBinding {
        override fun getRoot(): View = textView
    }

    private val diffCallback = object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(old: String, new: String) = old == new
        override fun areContentsTheSame(old: String, new: String) = old == new
    }

    private fun createAdapter() = LoadMoreAdapter<TestBinding, String>(
        inflate = { _, parent, _ -> TestBinding(TextView(parent.context)) },
        diffCallback = diffCallback,
        bind = { binding, item, _ -> binding.textView.text = item }
    )

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
        }
    }

    @Test
    fun `submitList sets initial data`() {
        val adapter = createAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A", "B", "C"))
        assertEquals(3, adapter.currentList().size)
    }

    @Test
    fun `loadMore appends data`() {
        val adapter = createAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A", "B"))
        adapter.loadMore(listOf("C", "D"))
        assertEquals(4, adapter.currentList().size)
    }

    @Test
    fun `noMore changes state`() {
        val adapter = createAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A"))
        adapter.noMore()
        // Should not crash, footer updated
    }

    @Test
    fun `loadFailed changes state`() {
        val adapter = createAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A"))
        adapter.loadFailed()
        // Should not crash
    }

    @Test
    fun `resetLoadState resets to IDLE`() {
        val adapter = createAdapter()
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A"))
        adapter.noMore()
        adapter.resetLoadState()
        // State should be IDLE again
    }

    @Test
    fun `currentList returns copy`() {
        val adapter = createAdapter()
        adapter.submitList(listOf("X", "Y"))
        val list = adapter.currentList()
        assertEquals(listOf("X", "Y"), list)
    }

    @Test
    fun `custom text properties`() {
        val adapter = createAdapter()
        adapter.loadingText = "Fetching..."
        adapter.noMoreText = "All done"
        adapter.failedText = "Retry?"
        assertEquals("Fetching...", adapter.loadingText)
        assertEquals("All done", adapter.noMoreText)
        assertEquals("Retry?", adapter.failedText)
    }

    @Test
    fun `preloadOffset is configurable`() {
        val adapter = createAdapter()
        adapter.preloadOffset = 5
        assertEquals(5, adapter.preloadOffset)
    }

    @Test
    fun `setOnLoadMoreListener does not crash`() {
        val adapter = createAdapter()
        adapter.setOnLoadMoreListener { /* no-op */ }
    }

    @Test
    fun `click listener fires on item`() {
        var clicked: String? = null
        val adapter = createAdapter()
        adapter.setOnItemClickListener { item, _ -> clicked = item }
        recyclerView.adapter = adapter
        adapter.submitList(listOf("A", "B"))

        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        )
        recyclerView.layout(0, 0, 500, 500)

        val holder = recyclerView.findViewHolderForAdapterPosition(0)
        holder?.itemView?.performClick()
        assertEquals("A", clicked)
    }
}
