package com.ail.brick.ui.statelayout

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ail.brick.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val STATE_LAYOUT_ID = 2001
private const val CONTENT_VIEW_ID = 2002

private fun bindContentView(stateLayout: StateLayout, contentView: View) {
    val field = StateLayout::class.java.getDeclaredField("contentView")
    field.isAccessible = true
    field.set(stateLayout, contentView)
}

class StateLayoutTestActivity : FragmentActivity() {
    lateinit var stateLayout: StateLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = StateLayout(this).apply {
            id = STATE_LAYOUT_ID
            enableAnimation = false
        }
        val contentView = TextView(this).apply {
            id = CONTENT_VIEW_ID
            text = "content"
        }
        root.addView(contentView)
        bindContentView(root, contentView)
        stateLayout = root
        setContentView(root)
    }
}

@RunWith(AndroidJUnit4::class)
class StateLayoutInstrumentedTest {

    @Test
    fun showLoading_hidesContent_andUpdatesState() {
        val scenario = ActivityScenario.launch(StateLayoutTestActivity::class.java)

        scenario.onActivity { activity ->
            val stateLayout = activity.stateLayout
            val contentView = activity.findViewById<TextView>(CONTENT_VIEW_ID)

            stateLayout.showLoading()

            assertEquals(StateLayout.State.LOADING, stateLayout.currentState)
            assertEquals(View.GONE, contentView.visibility)
        }

        scenario.close()
    }

    @Test
    fun showError_bindsRetryCallback() {
        val scenario = ActivityScenario.launch(StateLayoutTestActivity::class.java)
        var retried = false

        scenario.onActivity { activity ->
            val stateLayout = activity.stateLayout
            stateLayout.showError { retried = true }

            val retryButton = stateLayout.findViewById<View>(R.id.btnRetry)
            assertEquals(StateLayout.State.ERROR, stateLayout.currentState)
            assertTrue(retryButton.performClick())
            assertTrue(retried)
        }

        scenario.close()
    }
}