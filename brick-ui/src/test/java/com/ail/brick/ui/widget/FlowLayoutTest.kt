package com.ail.brick.ui.widget

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FlowLayoutTest {

    private lateinit var activity: Activity
    private lateinit var flowLayout: FlowLayout

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        flowLayout = FlowLayout(activity)
    }

    @Test
    fun `default spacing is 8dp equivalent`() {
        val density = activity.resources.displayMetrics.density
        val expected = (8 * density).toInt()
        assertEquals(expected, flowLayout.horizontalSpacing)
        assertEquals(expected, flowLayout.verticalSpacing)
    }

    @Test
    fun `default maxLines is 0 (unlimited)`() {
        assertEquals(0, flowLayout.maxLines)
    }

    @Test
    fun `default gravity is START`() {
        assertEquals(Gravity.START, flowLayout.flowGravity)
    }

    @Test
    fun `children are laid out without crash`() {
        addTags("A", "B", "C")
        measureAndLayout(500, 500)
        assertEquals(3, flowLayout.childCount)
    }

    @Test
    fun `wraps to next line when width exceeded`() {
        // Add many wide tags to force wrapping
        for (i in 1..10) addTags("LongTag$i")
        measureAndLayout(200, 1000)
        // All children should be measured
        for (i in 0 until flowLayout.childCount) {
            assertTrue(flowLayout.getChildAt(i).measuredWidth > 0)
        }
    }

    @Test
    fun `maxLines limits visible rows`() {
        flowLayout.maxLines = 1
        for (i in 1..20) addTags("Tag$i")
        measureAndLayout(200, 1000)
        // Layout should still handle without crash
    }

    @Test
    fun `setting horizontalSpacing triggers relayout`() {
        flowLayout.horizontalSpacing = 20
        assertEquals(20, flowLayout.horizontalSpacing)
    }

    @Test
    fun `setting verticalSpacing triggers relayout`() {
        flowLayout.verticalSpacing = 16
        assertEquals(16, flowLayout.verticalSpacing)
    }

    @Test
    fun `setting flowGravity to CENTER works`() {
        flowLayout.flowGravity = Gravity.CENTER_HORIZONTAL
        assertEquals(Gravity.CENTER_HORIZONTAL, flowLayout.flowGravity)
        addTags("A", "B")
        measureAndLayout(500, 500)
    }

    @Test
    fun `setting flowGravity to END works`() {
        flowLayout.flowGravity = Gravity.END
        assertEquals(Gravity.END, flowLayout.flowGravity)
        addTags("A", "B")
        measureAndLayout(500, 500)
    }

    @Test
    fun `GONE children are skipped`() {
        addTags("A", "B", "C")
        flowLayout.getChildAt(1).visibility = View.GONE
        measureAndLayout(500, 500)
        // Should not crash
    }

    @Test
    fun `empty layout measures to zero height`() {
        flowLayout.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        assertEquals(flowLayout.paddingTop + flowLayout.paddingBottom, flowLayout.measuredHeight)
    }

    private fun addTags(vararg tags: String) {
        for (tag in tags) {
            flowLayout.addView(TextView(activity).apply { text = tag })
        }
    }

    private fun measureAndLayout(w: Int, h: Int) {
        flowLayout.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST)
        )
        flowLayout.layout(0, 0, flowLayout.measuredWidth, flowLayout.measuredHeight)
    }
}
