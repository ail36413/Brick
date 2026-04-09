package com.ail.brick.sample.net

import com.ail.brick.net.http.annotations.BaseUrl
import com.ail.brick.net.http.annotations.SuccessCode
import com.ail.brick.net.http.annotations.Timeout
import com.ail.brick.net.http.model.GlobalResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * 演示用 API 接口
 *
 * - httpbin.org：HTTP 基础测试（GET/POST/错误码/文件传输/Header 回显等）
 * - dummyjson.com：模拟真实 REST API（@BaseUrl / 业务响应 / 登录认证）
 */
interface SampleApi {

    // ==================== httpbin.org 基础 ====================

    /** GET 请求演示 */
    @GET("get")
    suspend fun testGet(): ResponseBody

    /** POST 请求演示 */
    @POST("post")
    suspend fun testPost(): ResponseBody

    /** 模拟延迟请求（用于超时演示） */
    @Timeout(read = 3)
    @GET("delay/2")
    suspend fun testDelay(): ResponseBody

    /** 模拟 404 错误 */
    @GET("status/404")
    suspend fun testNotFound(): ResponseBody

    /** 模拟 500 错误 */
    @GET("status/500")
    suspend fun testServerError(): ResponseBody

    /** 文件下载 */
    @Streaming
    @GET
    suspend fun downloadFile(@Url url: String): ResponseBody

    /** 文件上传 — httpbin.org 返回原始 JSON */
    @Multipart
    @POST("post")
    suspend fun uploadFile(@Part file: MultipartBody.Part): ResponseBody

    /** 公共 Header 回显 — 验证 extraHeaders */
    @GET("headers")
    suspend fun echoHeaders(): ResponseBody

    /** 获取随机 UUID — 用于 pollingFlow 演示 */
    @GET("uuid")
    suspend fun getUuid(): ResponseBody

    // ==================== executeRequest + ResponseFieldMapping ====================

    /**
     * httpbin /get 通过 GlobalResponse + ResponseFieldMapping 解析。
     *
     * httpbin 返回格式形如：
     * ```json
     * { "url": "...", "origin": "...", "headers": {...}, "args": {} }
     * ```
     * 全局 mapping 配置为 codeKey=url, msgKey=origin, dataKey=headers，
     * 所以 GlobalResponse.data 将会是 headers 对象。
     */
    @GET("get")
    suspend fun testMappedGet(): GlobalResponse<Map<String, String>>

    // ==================== @BaseUrl 演示 ====================

    /** @BaseUrl 动态切换不同域名 — 请求 dummyjson 产品详情 */
    @BaseUrl("https://dummyjson.com/")
    @GET("products/1")
    suspend fun testBaseUrl(): ResponseBody

    /** @BaseUrl + @SuccessCode — 请求 dummyjson 所有产品（限制 3 条） */
    @BaseUrl("https://dummyjson.com/")
    @GET("products?limit=3&select=title,price")
    suspend fun testBaseUrlList(): ResponseBody

    // ==================== @SuccessCode 演示 ====================

    /**
     * @SuccessCode(200) — 以 HTTP 200 为业务成功码。
     * httpbin /get 正常返回 200，配合 ResponseFieldMapping 的 codeValueConverter
     * 将 URL 字符串识别为"存在即成功"。
     */
    @SuccessCode(200)
    @GET("get")
    suspend fun testSuccessCode(): GlobalResponse<Map<String, String>>
}
