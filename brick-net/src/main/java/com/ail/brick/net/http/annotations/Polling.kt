package com.ail.brick.net.http.annotations

/**
 * 用于 Retrofit 接口方法的注解，指定该接口的轮询策略。
 *
 * - maxAttempts：轮询次数（含首次），0 或 1 表示只请求一次
 * - intervalMs：每次请求间隔毫秒数，默认 1000ms
 *
 * 示例：
 * ```kotlin
 * @Polling(maxAttempts = 5, intervalMs = 2000)
 * suspend fun pollStatus(): GlobalResponse<Status>
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Polling(
    val maxAttempts: Int = 1,
    val intervalMs: Long = 1000L
)
