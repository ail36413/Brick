package com.ail.brick.net.http.interceptor

import com.ail.brick.net.http.annotations.Polling
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

/**
 * 动态轮询拦截器，支持通过 @Polling 注解实现 per-API 轮询。
 *
 * - 每次请求无论成功/失败都继续，直到 maxAttempts 次数用尽
 * - intervalMs 控制每次请求间隔
 * - 若无注解则直接放行
 */
class DynamicPollingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val polling = request.tag(Invocation::class.java)?.method()?.getAnnotation(Polling::class.java)
        if (polling == null || polling.maxAttempts <= 1) {
            return chain.proceed(request)
        }
        var lastResponse: Response? = null
        repeat(polling.maxAttempts) { attempt ->
            if (attempt > 0) Thread.sleep(polling.intervalMs)
            lastResponse?.close()
            lastResponse = chain.proceed(request)
        }
        return lastResponse!!
    }
}
