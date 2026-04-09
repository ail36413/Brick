package com.ail.brick.arch.nav

import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * BrickNav Robolectric 测试 — 验证 Fragment 导航核心逻辑。
 *
 * 覆盖场景：
 * - 初始化与路由注册
 * - navigate / back / backTo / clearStack
 * - singleTop 去重
 * - 拦截器
 * - 未注册路由异常
 * - 参数传递
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BrickNavTest {

    companion object {
        private const val CONTAINER_ID = 100
    }

    /** 用于测试的空 Fragment */
    class HomeFragment : Fragment()
    class ProfileFragment : Fragment()
    class SettingsFragment : Fragment()

    private lateinit var activity: FragmentActivity
    private lateinit var nav: BrickNav

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        // 添加 container
        val container = FrameLayout(activity).apply { id = CONTAINER_ID }
        activity.setContentView(container)

        nav = BrickNav.init(activity, CONTAINER_ID)
            .register<HomeFragment>("home")
            .register<ProfileFragment>("profile")
            .register<SettingsFragment>("settings")
    }

    // ── 初始化 ──

    @Test
    fun `init returns BrickNav instance`() {
        assertNotNull(nav)
        assertNull(nav.currentRoute)
        assertEquals(0, nav.stackDepth)
    }

    @Test
    fun `from activity returns same instance`() {
        val same = BrickNav.from(activity)
        assertSame(nav, same)
    }

    // ── navigate ──

    @Test
    fun `navigate sets currentRoute`() {
        nav.navigate("home") { anim = NavAnim.NONE }
        assertEquals("home", nav.currentRoute)
    }

    @Test
    fun `navigate with addToBackStack increases stackDepth`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
        assertEquals(1, nav.stackDepth)

        // 防连点：wait enough time
        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE; addToBackStack = true }
        assertEquals(2, nav.stackDepth)
    }

    @Test
    fun `navigate without addToBackStack does not increase stackDepth`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = false }
        assertEquals(0, nav.stackDepth)
        assertEquals("home", nav.currentRoute)
    }

    @Test
    fun `navigate passes arguments to Fragment`() {
        val args = Bundle().apply { putString("key", "value") }
        nav.navigate("home", args) { anim = NavAnim.NONE }

        val fragment = activity.supportFragmentManager.findFragmentByTag("home")
        assertNotNull(fragment)
        assertEquals("value", fragment!!.arguments?.getString("key"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `navigate unregistered route throws`() {
        nav.navigate("unknown") { anim = NavAnim.NONE }
    }

    // ── singleTop ──

    @Test
    fun `singleTop navigation does not create duplicate`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
        val firstStackDepth = nav.stackDepth

        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("home") { anim = NavAnim.NONE; singleTop = true; addToBackStack = true }
        assertEquals(firstStackDepth, nav.stackDepth)
    }

    // ── back ──

    @Test
    fun `back returns true when stack not empty`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE; addToBackStack = true }

        assertTrue(nav.back())
        assertEquals(1, nav.stackDepth)
    }

    @Test
    fun `back returns false when stack is empty`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = false }
        assertFalse(nav.back())
    }

    // ── backTo ──

    @Test
    fun `backTo pops to specified route`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE; addToBackStack = true }
        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("settings") { anim = NavAnim.NONE; addToBackStack = true }

        val result = nav.backTo("home", inclusive = false)
        assertTrue(result)
        assertEquals(1, nav.stackDepth)
    }

    // ── clearStack ──

    @Test
    fun `clearStack empties the back stack`() {
        nav.navigate("home") { anim = NavAnim.NONE; addToBackStack = true }
        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE; addToBackStack = true }

        nav.clearStack()
        assertEquals(0, nav.stackDepth)
    }

    // ── 拦截器 ──

    @Test
    fun `interceptor can block navigation`() {
        nav.addInterceptor { _, to, _ -> to != "profile" }

        nav.navigate("home") { anim = NavAnim.NONE }
        assertEquals("home", nav.currentRoute)

        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE }
        // Should still be "home" because interceptor blocked "profile"
        assertEquals("home", nav.currentRoute)
    }

    @Test
    fun `interceptor receives correct from and to`() {
        var capturedFrom: String? = "not-set"
        var capturedTo: String? = "not-set"

        nav.addInterceptor { from, to, _ ->
            capturedFrom = from
            capturedTo = to
            true
        }

        nav.navigate("home") { anim = NavAnim.NONE }
        assertNull(capturedFrom)
        assertEquals("home", capturedTo)

        ShadowSystemClock.advanceBy(Duration.ofMillis(350))
        nav.navigate("profile") { anim = NavAnim.NONE }
        assertEquals("home", capturedFrom)
        assertEquals("profile", capturedTo)
    }
}
