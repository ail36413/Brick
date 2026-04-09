package com.ail.brick.ui.anim

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BrickAnimTest {

    private lateinit var activity: Activity
    private lateinit var testView: View

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        testView = View(activity).apply {
            val parent = FrameLayout(activity)
            parent.addView(this, 100, 100)
            parent.measure(
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
            )
            parent.layout(0, 0, 200, 200)
        }
    }

    @Test
    fun `fadeIn returns animator and sets visible`() {
        testView.visibility = View.GONE
        val anim = testView.fadeIn()
        assertNotNull(anim)
        assertEquals(View.VISIBLE, testView.visibility)
    }

    @Test
    fun `fadeOut returns animator`() {
        testView.visibility = View.VISIBLE
        testView.alpha = 1f
        val anim = testView.fadeOut()
        assertNotNull(anim)
    }

    @Test
    fun `slideInFromBottom returns animator`() {
        val anim = testView.slideInFromBottom()
        assertNotNull(anim)
        assertEquals(View.VISIBLE, testView.visibility)
    }

    @Test
    fun `slideInFromTop returns animator`() {
        val anim = testView.slideInFromTop()
        assertNotNull(anim)
        assertEquals(View.VISIBLE, testView.visibility)
    }

    @Test
    fun `slideInFromLeft returns animator`() {
        val anim = testView.slideInFromLeft()
        assertNotNull(anim)
    }

    @Test
    fun `slideInFromRight returns animator`() {
        val anim = testView.slideInFromRight()
        assertNotNull(anim)
    }

    @Test
    fun `slideOutToTop returns animator`() {
        val anim = testView.slideOutToTop()
        assertNotNull(anim)
    }

    @Test
    fun `slideOutToBottom returns animator`() {
        val anim = testView.slideOutToBottom()
        assertNotNull(anim)
    }

    @Test
    fun `scaleIn returns animator`() {
        val anim = testView.scaleIn()
        assertNotNull(anim)
        assertEquals(View.VISIBLE, testView.visibility)
    }

    @Test
    fun `scaleOut returns animator`() {
        val anim = testView.scaleOut()
        assertNotNull(anim)
    }

    @Test
    fun `pulse returns animator`() {
        val anim = testView.pulse()
        assertNotNull(anim)
    }

    @Test
    fun `shake returns animator`() {
        val anim = testView.shake()
        assertNotNull(anim)
    }

    @Test
    fun `bounce returns animator`() {
        val anim = testView.bounce()
        assertNotNull(anim)
    }

    @Test
    fun `fadeSlideIn returns animator`() {
        val anim = testView.fadeSlideIn()
        assertNotNull(anim)
        assertEquals(View.VISIBLE, testView.visibility)
    }

    @Test
    fun `fadeSlideOut returns animator`() {
        val anim = testView.fadeSlideOut()
        assertNotNull(anim)
    }

    @Test
    fun `rotate returns animator`() {
        val anim = testView.rotate()
        assertNotNull(anim)
    }

    @Test
    fun `fadeIn with custom duration`() {
        val anim = testView.fadeIn(duration = 500L)
        assertNotNull(anim)
    }

    @Test
    fun `fadeOut with goneOnEnd false`() {
        val anim = testView.fadeOut(goneOnEnd = false)
        assertNotNull(anim)
    }

    @Test
    fun `onEnd callback is accepted`() {
        var ended = false
        val anim = testView.fadeIn(onEnd = { ended = true })
        assertNotNull(anim)
        // Callback wired up (not verifying execution in Robolectric)
    }
}
