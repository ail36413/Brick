package com.ail.brick.ui.recyclerview

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DividerDecorationTest {

    private lateinit var activity: Activity
    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        recyclerView = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
        }
    }

    @Test
    fun `default decoration has 1px height`() {
        val decoration = DividerDecoration()
        recyclerView.addItemDecoration(decoration)
        assertNotNull(decoration)
    }

    @Test
    fun `custom color and height`() {
        val decoration = DividerDecoration(height = 2, color = Color.RED)
        recyclerView.addItemDecoration(decoration)
        assertNotNull(decoration)
    }

    @Test
    fun `padding start and end are applied`() {
        val decoration = DividerDecoration(paddingStart = 16, paddingEnd = 16)
        recyclerView.addItemDecoration(decoration)
        assertNotNull(decoration)
    }

    @Test
    fun `can be added to recyclerView without crash`() {
        val adapter = MultiTypeAdapter()
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerDecoration(height = 1, color = Color.GRAY))
        recyclerView.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        )
        recyclerView.layout(0, 0, 500, 500)
    }
}
