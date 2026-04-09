package com.ail.brick.arch.nav

import android.os.Bundle
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_CONTAINER_ID = 1001

class BrickNavTestActivity : FragmentActivity() {
    lateinit var nav: BrickNav

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = TEST_CONTAINER_ID })
        nav = BrickNav.init(this, TEST_CONTAINER_ID)
            .register<HomeFragment>("home")
            .register<ProfileFragment>("profile")
    }
}

class HomeFragment : Fragment()

class ProfileFragment : Fragment()

@RunWith(AndroidJUnit4::class)
class BrickNavInstrumentedTest {

    @Test
    fun from_returnsSameInstance_forActivity() {
        val scenario = ActivityScenario.launch(BrickNavTestActivity::class.java)

        scenario.onActivity { activity ->
            assertSame(activity.nav, BrickNav.from(activity))
        }

        scenario.close()
    }

    @Test
    fun navigate_andBack_updateCurrentRouteAndStack() {
        val scenario = ActivityScenario.launch(BrickNavTestActivity::class.java)

        scenario.onActivity { activity ->
            activity.nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
            assertEquals("home", activity.nav.currentRoute)
            assertEquals(1, activity.nav.stackDepth)
        }

        SystemClock.sleep(350)

        scenario.onActivity { activity ->
            activity.nav.navigate("profile") { anim = NavAnim.NONE; addToBackStack = true }
            assertEquals("profile", activity.nav.currentRoute)
            assertEquals(2, activity.nav.stackDepth)
            assertTrue(activity.nav.back())
            assertEquals("home", activity.nav.currentRoute)
            assertEquals(1, activity.nav.stackDepth)
        }

        scenario.close()
    }
}