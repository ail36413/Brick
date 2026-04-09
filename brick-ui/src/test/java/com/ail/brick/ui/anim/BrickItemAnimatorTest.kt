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
class BrickItemAnimatorTest {

    private lateinit var activity: Activity
    private lateinit var itemView: View

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        itemView = View(activity).apply {
            val parent = FrameLayout(activity)
            parent.addView(this, 100, 50)
            parent.measure(
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
            )
            parent.layout(0, 0, 200, 200)
        }
    }

    @Test
    fun `animateItem FADE_SLIDE_UP does not crash`() {
        BrickItemAnimator.animateItem(itemView, 0, BrickItemAnimator.AnimType.FADE_SLIDE_UP)
    }

    @Test
    fun `animateItem FADE_SLIDE_LEFT does not crash`() {
        BrickItemAnimator.animateItem(itemView, 1, BrickItemAnimator.AnimType.FADE_SLIDE_LEFT)
    }

    @Test
    fun `animateItem FADE_SLIDE_RIGHT does not crash`() {
        BrickItemAnimator.animateItem(itemView, 2, BrickItemAnimator.AnimType.FADE_SLIDE_RIGHT)
    }

    @Test
    fun `animateItem FADE_IN does not crash`() {
        BrickItemAnimator.animateItem(itemView, 0, BrickItemAnimator.AnimType.FADE_IN)
    }

    @Test
    fun `animateItem SCALE_IN does not crash`() {
        BrickItemAnimator.animateItem(itemView, 0, BrickItemAnimator.AnimType.SCALE_IN)
    }

    @Test
    fun `custom duration and delay`() {
        BrickItemAnimator.animateItem(itemView, 3, duration = 500L, delayPerItem = 100L)
    }

    @Test
    fun `resetItem restores default state`() {
        BrickItemAnimator.animateItem(itemView, 0, BrickItemAnimator.AnimType.SCALE_IN)
        BrickItemAnimator.resetItem(itemView)
        assertEquals(1f, itemView.alpha)
        assertEquals(0f, itemView.translationX)
        assertEquals(0f, itemView.translationY)
        assertEquals(1f, itemView.scaleX)
        assertEquals(1f, itemView.scaleY)
    }

    @Test
    fun `all AnimType values are handled`() {
        for (type in BrickItemAnimator.AnimType.values()) {
            BrickItemAnimator.animateItem(itemView, 0, type)
            BrickItemAnimator.resetItem(itemView)
        }
    }
}
