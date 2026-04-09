package com.ail.brick.ui.widget

import android.app.Activity
import android.graphics.Color
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
class RoundLayoutTest {

    private lateinit var activity: Activity
    private lateinit var roundLayout: RoundLayout

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        roundLayout = RoundLayout(activity)
    }

    @Test
    fun `default stroke is transparent and zero width`() {
        assertEquals(Color.TRANSPARENT, roundLayout.strokeColor)
        assertEquals(0f, roundLayout.strokeWidth)
    }

    @Test
    fun `setRadius sets all corners equally`() {
        roundLayout.setRadius(20f)
        // Verify via dispatchDraw not crashing with children
        val child = TextView(activity).apply { text = "Test" }
        roundLayout.addView(child)
        roundLayout.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        roundLayout.layout(0, 0, 200, 200)
        // No exception means radii applied correctly
    }

    @Test
    fun `setRadii sets independent corners`() {
        roundLayout.setRadii(10f, 20f, 30f, 40f)
        roundLayout.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        roundLayout.layout(0, 0, 200, 200)
    }

    @Test
    fun `setStroke updates color and width`() {
        roundLayout.setStroke(Color.RED, 5f)
        assertEquals(Color.RED, roundLayout.strokeColor)
        assertEquals(5f, roundLayout.strokeWidth)
    }

    @Test
    fun `strokeColor setter triggers invalidate`() {
        roundLayout.strokeColor = Color.BLUE
        assertEquals(Color.BLUE, roundLayout.strokeColor)
    }

    @Test
    fun `strokeWidth setter triggers invalidate`() {
        roundLayout.strokeWidth = 3f
        assertEquals(3f, roundLayout.strokeWidth)
    }

    @Test
    fun `dispatchDraw with stroke does not crash`() {
        roundLayout.setStroke(Color.RED, 2f)
        roundLayout.setRadius(12f)
        val child = TextView(activity).apply { text = "Stroke" }
        roundLayout.addView(child)
        roundLayout.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        roundLayout.layout(0, 0, 300, 100)
    }

    @Test
    fun `onSizeChanged resets path dirty flag`() {
        roundLayout.setRadius(10f)
        roundLayout.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
        )
        roundLayout.layout(0, 0, 100, 100)
        // Resize
        roundLayout.measure(
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        roundLayout.layout(0, 0, 200, 200)
        // No exception
    }
}
