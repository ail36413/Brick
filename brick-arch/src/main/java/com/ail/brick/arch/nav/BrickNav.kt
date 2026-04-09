package com.ail.brick.arch.nav

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ail.brick.arch.R
import kotlin.reflect.KClass

/**
 * 轻量级 Fragment 导航管理器 — 纯代码路由，零 XML。
 *
 * ### 特性
 * - 纯代码路由注册，无需 navigation XML
 * - 4 种内置转场动画 + 自定义动画
 * - 自动处理系统返回键（OnBackPressedCallback）
 * - 防连点保护（300ms 内忽略重复导航）
 * - State-loss 安全（不会在 onSaveInstanceState 后崩溃）
 * - 导航拦截器（登录检查 / 埋点等）
 *
 * ### 线程要求
 * 所有导航操作（[navigate]、[back]、[backTo]、[clearStack]）必须在**主线程**调用，
 * 已标注 `@MainThread`。在子线程调用会导致 FragmentManager 状态异常。
 *
 * ### 生命周期边界
 * - 应在 `Activity.onCreate()` 中调用 [init]，Activity 销毁时自动清理实例。
 * - `onSaveInstanceState` 之后的导航操作会被静默忽略（state-loss 保护）。
 * - 配置变更后可通过 [from] 重新获取实例，[currentRoute] 会自动从 FragmentManager 恢复。
 *
 * ```kotlin
 * val nav = BrickNav.init(this, R.id.container)
 *     .register<HomeFragment>("home")
 *     .register<ProfileFragment>("profile")
 *
 * nav.navigate("profile", bundleOf("id" to 123))
 * nav.navigate("profile") { anim = NavAnim.FADE; singleTop = true }
 * nav.back()
 *
 * // 从 Fragment 中
 * BrickNav.from(this).navigate("settings")
 * ```
 */
class BrickNav private constructor(
    private val activity: FragmentActivity,
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int,
) {

    private val routes = mutableMapOf<String, KClass<out Fragment>>()
    private val interceptors = mutableListOf<NavInterceptor>()

    /** 内部维护的当前路由，避免依赖 FragmentManager 异步状态 */
    private var _currentRoute: String? = null

    /** 上次导航时间戳，防连点 */
    private var lastNavigateTime = 0L

    /** 系统返回键回调 */
    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (!back()) {
                isEnabled = false
                activity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /** 当前显示的路由 tag */
    val currentRoute: String? get() = _currentRoute

    /** 返回栈深度 */
    val stackDepth: Int get() = fragmentManager.backStackEntryCount

    // ── 路由注册 ──────────────────────────────────────

    fun register(route: String, cls: KClass<out Fragment>): BrickNav {
        routes[route] = cls
        return this
    }

    inline fun <reified F : Fragment> register(route: String): BrickNav =
        register(route, F::class)

    // ── 拦截器 ──────────────────────────────────────

    fun addInterceptor(interceptor: NavInterceptor): BrickNav {
        interceptors += interceptor
        return this
    }

    // ── 导航操作 ──────────────────────────────────────

    /**
     * 导航到指定路由。
     *
     * @param route   目标路由名称（需先 [register]）
     * @param args    传递给目标 Fragment 的 [Bundle]
     * @param builder 可选 [NavOptions] DSL
     */
    @MainThread
    fun navigate(
        route: String,
        args: Bundle? = null,
        builder: (NavOptions.() -> Unit)? = null,
    ) {
        val cls = routes[route]
            ?: throw IllegalArgumentException("Route not registered: $route")

        // state-loss 保护：onSaveInstanceState 后跳过
        if (fragmentManager.isStateSaved) return

        // 防连点：300ms 内忽略（使用单调时钟，不受系统时间调整影响）
        val now = android.os.SystemClock.uptimeMillis()
        if (lastNavigateTime > 0 && now - lastNavigateTime < 300) return
        lastNavigateTime = now

        val options = NavOptions().apply { builder?.invoke(this) }

        for (interceptor in interceptors) {
            if (!interceptor.onNavigate(_currentRoute, route, args)) return
        }

        if (options.singleTop && _currentRoute == route) return

        val fragment = fragmentManager.fragmentFactory.instantiate(
            cls.java.classLoader ?: ClassLoader.getSystemClassLoader(),
            cls.java.name,
        ).apply {
            arguments = args
        }

        // commit() 支持 addToBackStack，commitNow() 不支持
        // 用 executePendingTransactions() 保证同步执行
        fragmentManager.commit {
            options.resolveAnimSet()?.let { a ->
                setCustomAnimations(a.enter, a.exit, a.popEnter, a.popExit)
            }
            replace(containerId, fragment, route)
            if (options.addToBackStack) {
                addToBackStack(route)
            }
        }
        fragmentManager.executePendingTransactions()

        _currentRoute = route
        updateBackCallbackState()
    }

    /**
     * 返回上一页。如果返回栈为空，返回 false。
     * 使用同步弹出（popBackStackImmediate）确保状态立即一致。
     */
    @MainThread
    fun back(): Boolean {
        if (fragmentManager.isStateSaved) return false
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStackImmediate()
            syncCurrentRoute()
            return true
        }
        return false
    }

    /**
     * 弹出到指定路由。
     *
     * @param route     目标路由名称（该路由对应的 backstack entry）
     * @param inclusive 是否连同目标路由一起弹出
     */
    @MainThread
    fun backTo(route: String, inclusive: Boolean = false): Boolean {
        if (fragmentManager.isStateSaved) return false
        val flag = if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
        val result = fragmentManager.popBackStackImmediate(route, flag)
        syncCurrentRoute()
        return result
    }

    /** 清空整个返回栈，恢复到第一个 Fragment。 */
    @MainThread
    fun clearStack() {
        if (fragmentManager.isStateSaved) return
        if (fragmentManager.backStackEntryCount > 0) {
            val first = fragmentManager.getBackStackEntryAt(0)
            fragmentManager.popBackStackImmediate(
                first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE,
            )
            syncCurrentRoute()
        }
    }

    // ── 内部方法 ──────────────────────────────────────

    /** 从 FragmentManager 返回栈同步 currentRoute */
    private fun syncCurrentRoute() {
        val count = fragmentManager.backStackEntryCount
        _currentRoute = if (count > 0) {
            fragmentManager.getBackStackEntryAt(count - 1).name
        } else {
            // 返回栈为空时，取容器中可见的 Fragment tag
            fragmentManager.findFragmentById(containerId)?.tag
        }
        updateBackCallbackState()
    }

    /** 更新返回键回调是否启用 */
    private fun updateBackCallbackState() {
        backPressedCallback.isEnabled = fragmentManager.backStackEntryCount > 0
    }

    // ── 静态工厂 ──────────────────────────────────────

    companion object {
        private val instances = mutableMapOf<Int, BrickNav>()

        /**
         * 在 Activity 中初始化。应在 `onCreate` 中调用。
         * Activity 销毁时自动清理。
         * 自动注册系统返回键处理。
         */
        fun init(activity: FragmentActivity, @IdRes containerId: Int): BrickNav {
            val key = System.identityHashCode(activity)
            val nav = BrickNav(activity, activity.supportFragmentManager, containerId)
            instances[key] = nav

            // 注册系统返回键回调
            activity.onBackPressedDispatcher.addCallback(activity, nav.backPressedCallback)

            // 同步 currentRoute（处理配置变更 / 进程恢复）
            nav.syncCurrentRoute()

            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    instances.remove(key)
                }
            })
            return nav
        }

        /** 从 Fragment 获取所属 Activity 的 BrickNav 实例 */
        fun from(fragment: Fragment): BrickNav {
            val key = System.identityHashCode(fragment.requireActivity())
            return instances[key]
                ?: error("BrickNav not initialized. Call BrickNav.init() in your Activity first.")
        }

        /** 从 Activity 获取已初始化的 BrickNav 实例 */
        fun from(activity: FragmentActivity): BrickNav {
            val key = System.identityHashCode(activity)
            return instances[key]
                ?: error("BrickNav not initialized. Call BrickNav.init() in your Activity first.")
        }
    }
}

// ── NavOptions ──────────────────────────────────────────

/** 导航选项 */
class NavOptions {
    /** 是否加入返回栈（默认 true） */
    var addToBackStack: Boolean = true

    /** 栈顶去重：当前路由与目标一致时跳过（默认 false） */
    var singleTop: Boolean = false

    /** 预设转场动画（默认水平滑动） */
    var anim: NavAnim = NavAnim.SLIDE_HORIZONTAL

    @AnimRes private var customEnter: Int = 0
    @AnimRes private var customExit: Int = 0
    @AnimRes private var customPopEnter: Int = 0
    @AnimRes private var customPopExit: Int = 0

    /** 使用自定义动画资源（会覆盖 [anim] 预设） */
    fun setCustomAnim(
        @AnimRes enter: Int,
        @AnimRes exit: Int,
        @AnimRes popEnter: Int = 0,
        @AnimRes popExit: Int = 0,
    ) {
        anim = NavAnim.NONE
        customEnter = enter
        customExit = exit
        customPopEnter = popEnter
        customPopExit = popExit
    }

    internal fun resolveAnimSet(): AnimSet? {
        if (anim != NavAnim.NONE) return anim.resolve()
        if (customEnter != 0 || customExit != 0) {
            return AnimSet(customEnter, customExit, customPopEnter, customPopExit)
        }
        return null
    }
}

// ── NavAnim ──────────────────────────────────────────

/** 内置转场动画预设 */
enum class NavAnim {
    /** 无动画 */
    NONE,
    /** 淡入淡出 */
    FADE,
    /** 水平滑动（左右推入推出） */
    SLIDE_HORIZONTAL,
    /** 垂直滑动（底部弹出 / 下拉关闭） */
    SLIDE_VERTICAL;

    internal fun resolve(): AnimSet? = when (this) {
        NONE -> null
        FADE -> AnimSet(
            R.anim.brick_nav_fade_in,
            R.anim.brick_nav_fade_out,
            R.anim.brick_nav_fade_in,
            R.anim.brick_nav_fade_out,
        )
        SLIDE_HORIZONTAL -> AnimSet(
            R.anim.brick_nav_slide_in_right,
            R.anim.brick_nav_slide_out_left,
            R.anim.brick_nav_slide_in_left,
            R.anim.brick_nav_slide_out_right,
        )
        SLIDE_VERTICAL -> AnimSet(
            R.anim.brick_nav_slide_in_up,
            R.anim.brick_nav_fade_out,
            R.anim.brick_nav_fade_in,
            R.anim.brick_nav_slide_out_down,
        )
    }
}

internal data class AnimSet(
    @AnimRes val enter: Int,
    @AnimRes val exit: Int,
    @AnimRes val popEnter: Int,
    @AnimRes val popExit: Int,
)

// ── NavInterceptor ──────────────────────────────────────

/** 导航拦截器 */
fun interface NavInterceptor {
    /**
     * @param from 当前路由（可能为 null）
     * @param to   目标路由
     * @param args 导航参数
     * @return true 允许导航，false 拦截
     */
    fun onNavigate(from: String?, to: String, args: Bundle?): Boolean
}
