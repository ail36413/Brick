package com.ail.brick.sample.net

import com.ail.brick.net.http.annotations.INetLogger
import com.ail.brick.net.http.annotations.NetworkConfig
import com.ail.brick.net.http.annotations.NetworkLogLevel
import com.ail.brick.net.http.model.ResponseFieldMapping
import com.ail.brick.net.websocket.IWebSocketLogger
import com.ail.brick.net.websocket.annotation.WebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 演示 App 的网络配置模块
 *
 * 展示 brick-net 的可配置项：
 * - baseUrl / 超时 / 日志级别
 * - extraHeaders 公共请求头
 * - ResponseFieldMapping 自定义响应字段映射（兼容非 code/msg/data 样式）
 * - enableRetryInterceptor 自动重试
 * - maxIdleConnections / keepAliveDurationSeconds 连接池优化
 * - INetLogger / IWebSocketLogger 自定义日志输出
 */
@Module
@InstallIn(SingletonComponent::class)
object SampleNetworkModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig {
        return NetworkConfig(
            baseUrl = "https://httpbin.org/",
            connectTimeout = 15L,
            readTimeout = 15L,
            writeTimeout = 15L,
            defaultSuccessCode = 0,
            networkLogLevel = NetworkLogLevel.BODY,
            enableRetryInterceptor = true,
            retryMaxAttempts = 1,
            extraHeaders = mapOf(
                "X-App-Name" to "BrickSample",
                "X-App-Version" to "1.0.0"
            ),
            // ────────── 连接池配置（性能优化） ──────────
            maxIdleConnections = 5,         // 最大空闲连接数
            keepAliveDurationSeconds = 300,  // 空闲连接存活时间（秒）
            // ────────── 响应字段映射（核心特性演示） ──────────
            // httpbin.org 返回 { url, origin, headers, args } 格式
            // 通过 ResponseFieldMapping 映射为 GlobalResponse<T>：
            //   url    → code（codeValueConverter 将非空值转为 successCode）
            //   origin → msg
            //   headers → data
            responseFieldMapping = ResponseFieldMapping(
                codeKey = "url",
                msgKey = "origin",
                dataKey = "headers",
                codeValueConverter = { rawCode, mapping ->
                    if (rawCode != null) mapping.successCode else mapping.failureCode
                }
            )
        )
    }

    @Provides
    @Singleton
    fun provideSampleApi(retrofit: Retrofit): SampleApi {
        return retrofit.create(SampleApi::class.java)
    }

    /**
     * 选配：提供 HTTP 日志实现（如接入 BrickLog、Timber 等）
     */
    @Provides
    @Singleton
    fun provideNetLogger(): INetLogger = object : INetLogger {
        override fun d(tag: String, msg: String) {
            android.util.Log.d(tag, msg)
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            android.util.Log.e(tag, msg, throwable)
        }
    }

    /**
     * 选配：提供 WebSocket OkHttpClient；不提供时库内会使用默认配置。
     */
    @Provides
    @Singleton
    @WebSocketClient
    fun provideWebSocketOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 选配：提供 WebSocket 日志实现；与 HTTP 日志完全独立。
     */
    @Provides
    @Singleton
    fun provideWebSocketLogger(): IWebSocketLogger = object : IWebSocketLogger {
        override fun d(tag: String, msg: String) {
            android.util.Log.d(tag, msg)
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            android.util.Log.e(tag, msg, throwable)
        }
    }
}
