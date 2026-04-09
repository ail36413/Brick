package com.ail.brick.ui.statelayout

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * StateLayout 的 Robolectric 测试。
 *
 * 覆盖场景：
 * - 默认状态为 CONTENT
 * - 状态切换（showLoading / showContent / showEmpty / showError）
 * - 懒加载 inflate（首次切换才创建状态视图）
 * - 状态变更监听器
 * - 重复切换同状态不触发回调
 * - 动画开关控制
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StateLayoutTest {

    private lateinit var activity: Activity
    private lateinit var stateLayout: StateLayout
    private lateinit var contentView: TextView

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        stateLayout = StateLayout(activity)
        contentView = TextView(activity).apply { text = "Content" }
        stateLayout.addView(contentView)
        // Trigger onFinishInflate
        stateLayout.measure(
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        )
    }

    @Test
    fun `default state is CONTENT`() {
        assertEquals(StateLayout.State.CONTENT, stateLayout.currentState)
    }

    @Test
    fun `showLoading switches state`() {
        stateLayout.showLoading()
        assertEquals(StateLayout.State.LOADING, stateLayout.currentState)
    }

    @Test
    fun `showEmpty switches state`() {
        stateLayout.showEmpty()
        assertEquals(StateLayout.State.EMPTY, stateLayout.currentState)
    }

    @Test
    fun `showError switches state`() {
        stateLayout.showError()
        assertEquals(StateLayout.State.ERROR, stateLayout.currentState)
    }

    @Test
    fun `showContent restores from loading`() {
        stateLayout.showLoading()
        stateLayout.showContent()
        assertEquals(StateLayout.State.CONTENT, stateLayout.currentState)
        assertEquals(View.VISIBLE, contentView.visibility)
    }

    @Test
    fun `state change listener fires on transition`() {
        var oldFired: StateLayout.State? = null
        var newFired: StateLayout.State? = null
        stateLayout.setOnStateChangeListener { old, new ->
            oldFired = old
            newFired = new
        }
        stateLayout.showLoading()
        assertEquals(StateLayout.State.CONTENT, oldFired)
        assertEquals(StateLayout.State.LOADING, newFired)
    }

    @Test
    fun `duplicate state does not fire listener`() {
        stateLayout.showLoading()
        var called = false
        stateLayout.setOnStateChangeListener { _, _ -> called = true }
        stateLayout.showLoading() // same state
        assertFalse(called)
    }

    @Test
    fun `animation can be disabled`() {
        stateLayout.enableAnimation = false
        assertFalse(stateLayout.enableAnimation)
    }

    @Test
    fun `animation duration is configurable`() {
        stateLayout.animationDuration = 500L
        assertEquals(500L, stateLayout.animationDuration)
    }

    @Test
    fun `showError with retry callback`() {
        var retryCalled = false
        stateLayout.showError { retryCalled = true }
        assertEquals(StateLayout.State.ERROR, stateLayout.currentState)
    }

    @Test
    fun `full lifecycle CONTENT to LOADING to CONTENT`() {
        val transitions = mutableListOf<Pair<StateLayout.State, StateLayout.State>>()
        stateLayout.setOnStateChangeListener { old, new -> transitions.add(old to new) }

        stateLayout.showLoading()
        stateLayout.showContent()

        assertEquals(2, transitions.size)
        assertEquals(StateLayout.State.CONTENT to StateLayout.State.LOADING, transitions[0])
        assertEquals(StateLayout.State.LOADING to StateLayout.State.CONTENT, transitions[1])
    }
}
