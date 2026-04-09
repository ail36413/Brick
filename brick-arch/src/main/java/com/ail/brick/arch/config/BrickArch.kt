package com.ail.brick.arch.config

/**
 * brick-arch 全局配置入口。
 *
 * 在 Application.onCreate() 中初始化：
 * ```kotlin
 * BrickArch.init {
 *     logger = TimberBrickLogger()  // 替换为 Timber 等自定义实现
 * }
 * ```
 */
object BrickArch {

    /** 全局日志实现，默认使用 Android [android.util.Log] */
    var logger: BrickLogger = DefaultBrickLogger()

    /**
     * DSL 方式初始化配置。
     */
    fun init(block: BrickArch.() -> Unit) {
        block()
    }
}

/**
 * brick-arch 日志接口，可自定义实现（如 Timber）。
 *
 * ```kotlin
 * class TimberBrickLogger : BrickLogger {
 *     override fun d(tag: String, message: String) = Timber.tag(tag).d(message)
 *     override fun w(tag: String, message: String, throwable: Throwable?) = Timber.tag(tag).w(throwable, message)
 *     override fun e(tag: String, message: String, throwable: Throwable?) = Timber.tag(tag).e(throwable, message)
 * }
 * ```
 */
interface BrickLogger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * 默认日志实现，使用 Android [android.util.Log]。
 */
internal class DefaultBrickLogger : BrickLogger {
    override fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}
