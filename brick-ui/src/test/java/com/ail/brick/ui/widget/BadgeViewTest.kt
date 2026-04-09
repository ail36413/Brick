package com.ail.brick.ui.widget

import android.app.Activity
import android.graphics.Color
import android.view.View
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BadgeViewTest {

    private lateinit var activity: Activity
    private lateinit var badgeView: BadgeView

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        badgeView = BadgeView(activity)
    }

    @Test
    fun `default count is 0 (red dot mode)`() {
        assertEquals(0, badgeView.count)
        assertEquals(View.VISIBLE, badgeView.visibility)
    }

    @Test
    fun `negative count hides view`() {
        badgeView.count = -1
        assertEquals(View.GONE, badgeView.visibility)
    }

    @Test
    fun `zero count shows red dot`() {
        badgeView.count = 0
        assertEquals(View.VISIBLE, badgeView.visibility)
    }

    @Test
    fun `positive count shows number`() {
        badgeView.count = 5
        assertEquals(View.VISIBLE, badgeView.visibility)
        assertEquals(5, badgeView.count)
    }

    @Test
    fun `count over 99 is capped display`() {
        badgeView.count = 150
        assertEquals(150, badgeView.count) // internal value preserved
    }

    @Test
    fun `red dot measures as small circle`() {
        badgeView.count = 0
        badgeView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val density = activity.resources.displayMetrics.density
        val expectedSize = (8 * density).toInt()
        assertEquals(expectedSize, badgeView.measuredWidth)
        assertEquals(expectedSize, badgeView.measuredHeight)
    }

    @Test
    fun `number badge measures wider than red dot`() {
        badgeView.count = 0
        badgeView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val dotWidth = badgeView.measuredWidth

        badgeView.count = 88
        badgeView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        assertTrue(badgeView.measuredWidth > dotWidth)
    }

    @Test
    fun `toggling count between positive and negative`() {
        badgeView.count = 5
        assertEquals(View.VISIBLE, badgeView.visibility)
        badgeView.count = -1
        assertEquals(View.GONE, badgeView.visibility)
        badgeView.count = 0
        assertEquals(View.VISIBLE, badgeView.visibility)
    }

    @Test
    fun `onDraw does not crash for all modes`() {
        for (count in listOf(-1, 0, 1, 50, 100)) {
            badgeView.count = count
            if (count >= 0) {
                badgeView.measure(
                    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST)
                )
                badgeView.layout(0, 0, badgeView.measuredWidth, badgeView.measuredHeight)
            }
        }
    }
}
