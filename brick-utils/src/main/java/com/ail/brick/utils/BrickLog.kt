package com.ail.brick.utils

import android.util.Log

/**
 * 轻量日志工具，支持全局开关、日志级别过滤和自定义 Tag 前缀
 *
 * 推荐在 `Application.onCreate` 中初始化：
 * ```kotlin
 * BrickLog.init(isDebug = BuildConfig.DEBUG, prefix = "MyApp")
 * ```
 *
 * 生产环境仅输出警告及以上：
 * ```kotlin
 * BrickLog.init(isDebug = true, prefix = "MyApp", minLevel = BrickLog.Level.WARN)
 * ```
 *
 * 使用示例：
 * ```kotlin
 * BrickLog.d("Network", "请求成功")       // Tag: MyApp-Network
 * BrickLog.e("DB", "写入失败", exception)  // Tag: MyApp-DB
 * ```
 *
 * 使用 Lambda 延迟拼接（避免关闭日志时的字符串拼接开销）：
 * ```kotlin
 * BrickLog.d("Network") { "响应数据: ${response.body}" }
 * ```
 *
 * > **迁移提示**：如果需要文件日志、崩溃收集、JSON 格式化等高级功能，
 * > 请迁移到 `brick-log` 模块的 [com.ail.brick.log.BrickLogger]。
 * > `BrickLog` 继续作为零依赖的轻量日志工具维护，适用于简单场景。
 */
object BrickLog {

    /**
     * 日志级别，数值越高优先级越高
     */
    enum class Level(val priority: Int) {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6),
        NONE(Int.MAX_VALUE)
    }

    @Volatile
    private var enabled = true
    @Volatile
    private var prefix = "Brick"
    @Volatile
    private var minLevel = Level.VERBOSE

    /**
     * 初始化日志配置
     *
     * @param isDebug 是否启用日志输出，建议传入 `BuildConfig.DEBUG`
     * @param prefix Tag 前缀，最终 Tag 格式为 `{prefix}-{tag}`
     * @param minLevel 最低输出级别，低于此级别的日志将被忽略，默认 [Level.VERBOSE]（全部输出）
     */
    fun init(isDebug: Boolean = true, prefix: String = "Brick", minLevel: Level = Level.VERBOSE) {
        this.enabled = isDebug
        this.prefix = prefix
        this.minLevel = minLevel
    }

    @PublishedApi
    internal fun tag(tag: String) = "$prefix-$tag"

    @PublishedApi
    internal fun isLoggable(level: Level): Boolean = enabled && level.priority >= minLevel.priority

    /** Verbose 级别日志 */
    fun v(tag: String, msg: String) {
        if (isLoggable(Level.VERBOSE)) Log.v(tag(tag), msg)
    }

    /** Verbose 级别日志（Lambda 延迟拼接，避免关闭时的字符串开销） */
    inline fun v(tag: String, msg: () -> String) {
        if (isLoggable(Level.VERBOSE)) Log.v(tag(tag), msg())
    }

    /** Debug 级别日志 */
    fun d(tag: String, msg: String) {
        if (isLoggable(Level.DEBUG)) Log.d(tag(tag), msg)
    }

    /** Debug 级别日志（Lambda 延迟拼接） */
    inline fun d(tag: String, msg: () -> String) {
        if (isLoggable(Level.DEBUG)) Log.d(tag(tag), msg())
    }

    /** Info 级别日志 */
    fun i(tag: String, msg: String) {
        if (isLoggable(Level.INFO)) Log.i(tag(tag), msg)
    }

    /** Info 级别日志（Lambda 延迟拼接） */
    inline fun i(tag: String, msg: () -> String) {
        if (isLoggable(Level.INFO)) Log.i(tag(tag), msg())
    }

    /**
     * Warning 级别日志
     *
     * @param tag 标签
     * @param msg 日志内容
     * @param tr 可选的异常堆栈
     */
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (isLoggable(Level.WARN)) {
            if (tr != null) Log.w(tag(tag), msg, tr) else Log.w(tag(tag), msg)
        }
    }

    /** Warning 级别日志（Lambda 延迟拼接） */
    inline fun w(tag: String, tr: Throwable? = null, msg: () -> String) {
        if (isLoggable(Level.WARN)) {
            val m = msg()
            if (tr != null) Log.w(tag(tag), m, tr) else Log.w(tag(tag), m)
        }
    }

    /**
     * Error 级别日志
     *
     * @param tag 标签
     * @param msg 日志内容
     * @param tr 可选的异常堆栈
     */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (isLoggable(Level.ERROR)) {
            if (tr != null) Log.e(tag(tag), msg, tr) else Log.e(tag(tag), msg)
        }
    }

    /** Error 级别日志（Lambda 延迟拼接） */
    inline fun e(tag: String, tr: Throwable? = null, msg: () -> String) {
        if (isLoggable(Level.ERROR)) {
            val m = msg()
            if (tr != null) Log.e(tag(tag), m, tr) else Log.e(tag(tag), m)
        }
    }
}
