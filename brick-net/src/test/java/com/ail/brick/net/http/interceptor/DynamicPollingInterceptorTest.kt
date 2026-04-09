package com.ail.brick.net.http.interceptor

import com.ail.brick.net.http.annotations.Polling
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Invocation
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicInteger

/**
 * DynamicPollingInterceptor 使用 MockWebServer 的集成测试。
 * 验证 @Polling 注解的 per-API 轮询配置。
 */
class DynamicPollingInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DynamicPollingInterceptor())
            .build()
    }

    /**
     * 构造带 @Polling 注解的 Request（通过模拟 Retrofit Invocation tag）
     */
    private fun requestWithPollingAnnotation(
        url: okhttp3.HttpUrl,
        pollingAnnotation: Polling
    ): Request {
        val annotatedMethod = findAnnotatedMethod(pollingAnnotation)
        val invocation = Invocation.of(annotatedMethod, emptyList<Any>())
        return Request.Builder().url(url).tag(Invocation::class.java, invocation).build()
    }

    /** 用于测试的标注接口 */
    @Suppress("unused")
    private interface AnnotatedApis {
        @Polling(maxAttempts = 1)
        fun once()

        @Polling(maxAttempts = 3, intervalMs = 10)
        fun poll3Times()
    }

    private fun findAnnotatedMethod(polling: Polling): Method {
        return when (polling.maxAttempts) {
            1 -> AnnotatedApis::class.java.getMethod("once")
            3 -> AnnotatedApis::class.java.getMethod("poll3Times")
            else -> AnnotatedApis::class.java.getMethod("once")
        }
    }

    @Test
    fun `no annotation - only once`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val client = buildClient()
        val request = Request.Builder().url(server.url("/"))
            .build()
        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `@Polling maxAttempts=3 - always 3 times`() {
        repeat(2) { server.enqueue(MockResponse().setResponseCode(500)) }
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val client = buildClient()
        val request = requestWithPollingAnnotation(
            url = server.url("/"),
            pollingAnnotation = AnnotatedApis::class.java.getMethod("poll3Times").getAnnotation(Polling::class.java)!!
        )
        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `@Polling maxAttempts=3 - all fail still 3 times`() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }
        val client = buildClient()
        val request = requestWithPollingAnnotation(
            url = server.url("/"),
            pollingAnnotation = AnnotatedApis::class.java.getMethod("poll3Times").getAnnotation(Polling::class.java)!!
        )
        val response = client.newCall(request).execute()
        assertEquals(500, response.code)
        assertEquals(3, server.requestCount)
    }
}
