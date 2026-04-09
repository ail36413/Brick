package com.ail.brick.sample.net

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ail.brick.net.http.annotations.NetworkConfigProvider
import com.ail.brick.net.http.annotations.NetworkLogLevel
import com.ail.brick.net.http.model.NetworkResult
import com.ail.brick.net.http.model.fold
import com.ail.brick.net.http.model.getOrDefault
import com.ail.brick.net.http.model.getOrNull
import com.ail.brick.net.http.model.map
import com.ail.brick.net.http.model.onBusinessFailure
import com.ail.brick.net.http.model.onSuccess
import com.ail.brick.net.http.model.onTechnicalFailure
import com.ail.brick.net.http.util.NetworkExecutor
import com.ail.brick.net.http.util.NetworkMonitor
import com.ail.brick.net.http.util.NetworkType
import com.ail.brick.net.http.util.pollingFlow
import com.ail.brick.net.http.util.retryWithBackoff
import com.ail.brick.net.websocket.IWebSocketManager
import com.ail.brick.net.websocket.WebSocketLogLevel
import com.ail.brick.net.websocket.WebSocketManager
import com.ail.brick.sample.databinding.ActivityNetDemoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * brick-net 网络库功能演示页面
 *
 * 演示内容：
 * - GET / POST 基础请求
 * - @Timeout 注解动态超时
 * - HTTP 错误码处理（404 / 500）
 * - 文件下载（带进度）
 * - 文件上传（带进度）
 * - WebSocket 连接 / 收发消息
 * - 网络状态监听（NetworkMonitor）
 */
@AndroidEntryPoint
class NetDemoActivity : AppCompatActivity() {

    @Inject lateinit var networkExecutor: NetworkExecutor
    @Inject lateinit var sampleApi: SampleApi
    @Inject lateinit var webSocketManager: IWebSocketManager
    @Inject lateinit var configProvider: NetworkConfigProvider
    @Inject lateinit var networkMonitor: NetworkMonitor

    private lateinit var binding: ActivityNetDemoBinding
    private val logBuilder = StringBuilder()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupHttpButtons()
        setupErrorButtons()
        setupFileTransferButtons()
        setupResponseMappingButtons()
        setupAdvancedButtons()
        setupNetworkMonitorButtons()
        setupWebSocketButtons()

        binding.btnClearLog.setOnClickListener {
            logBuilder.clear()
            binding.tvLog.text = "等待操作…"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        webSocketManager.disconnectDefault(permanent = true)
    }

    // ==================== HTTP 请求 ====================

    private fun setupHttpButtons() {
        binding.btnGet.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 发起 GET 请求…")
                networkExecutor.executeRawRequest { sampleApi.testGet().string() }
                    .onSuccess { appendLog("✓ GET 成功: ${formatData(it)}") }
                    .onTechnicalFailure { appendLog("✗ GET 失败: ${it.message}") }
            }
        }

        binding.btnPost.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 发起 POST 请求…")
                networkExecutor.executeRawRequest { sampleApi.testPost().string() }
                    .onSuccess { appendLog("✓ POST 成功: ${formatData(it)}") }
                    .onTechnicalFailure { appendLog("✗ POST 失败: ${it.message}") }
            }
        }

        binding.btnDelay.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 发起延迟请求（@Timeout read=3s）…")
                networkExecutor.executeRawRequest { sampleApi.testDelay().string() }
                    .onSuccess { appendLog("✓ 延迟请求成功: ${formatData(it)}") }
                    .onTechnicalFailure { appendLog("✗ 延迟请求失败: ${it.message}") }
            }
        }
    }

    // ==================== 错误处理 ====================

    private fun setupErrorButtons() {
        binding.btn404.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 请求 404 接口…")
                networkExecutor.executeRawRequest { sampleApi.testNotFound() }
                    .onSuccess { appendLog("✓ 收到响应（不应出现此情况）") }
                    .onTechnicalFailure { appendLog("✗ 404 错误捕获: [${it.code}] ${it.message}") }
            }
        }

        binding.btn500.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 请求 500 接口…")
                networkExecutor.executeRawRequest { sampleApi.testServerError() }
                    .onSuccess { appendLog("✓ 收到响应（不应出现此情况）") }
                    .onTechnicalFailure { appendLog("✗ 500 错误捕获: [${it.code}] ${it.message}") }
            }
        }
    }

    // ==================== 文件传输 ====================

    private fun setupFileTransferButtons() {
        binding.btnDownload.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 开始下载文件…")
                binding.progressDownload.visibility = View.VISIBLE
                binding.progressDownload.progress = 0

                val progressFlow = NetworkExecutor.createDefaultProgressFlow()
                val targetFile = File(cacheDir, "download_test.json")

                // 收集进度
                val progressJob = launch {
                    progressFlow.collect { info ->
                        binding.progressDownload.progress = info.progress
                        if (info.isDone) {
                            appendLog("  下载完成: ${info.totalSize} bytes")
                        }
                    }
                }

                val result = networkExecutor.downloadFile(
                    targetFile = targetFile,
                    progressFlow = progressFlow
                ) { sampleApi.downloadFile("https://httpbin.org/json") }

                progressJob.cancel()

                when (result) {
                    is NetworkResult.Success -> {
                        binding.progressDownload.progress = 100
                        appendLog("✓ 文件已保存: ${result.data?.absolutePath}")
                        appendLog("  文件大小: ${result.data?.length() ?: 0} bytes")
                    }
                    is NetworkResult.TechnicalFailure -> {
                        appendLog("✗ 下载失败: ${result.exception.message}")
                    }
                    is NetworkResult.BusinessFailure -> {
                        appendLog("✗ 下载业务失败: [${result.code}] ${result.msg}")
                    }
                }
            }
        }

        binding.btnUpload.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 开始上传文件…")
                binding.progressUpload.visibility = View.VISIBLE
                binding.progressUpload.progress = 0

                // 创建临时测试文件
                val tempFile = File(cacheDir, "upload_test.txt").apply {
                    writeText("Brick-Net 上传测试文件内容\n".repeat(100))
                }

                val progressFlow = NetworkExecutor.createDefaultProgressFlow()

                val progressJob = launch {
                    progressFlow.collect { info ->
                        binding.progressUpload.progress = info.progress
                        if (info.isDone) {
                            appendLog("  上传完成: ${info.totalSize} bytes")
                        }
                    }
                }

                val part = networkExecutor.createProgressPart("file", tempFile, progressFlow)
                val result = networkExecutor.executeRawRequest {
                    sampleApi.uploadFile(part).string()
                }

                progressJob.cancel()

                result
                    .onSuccess {
                        binding.progressUpload.progress = 100
                        appendLog("✓ 上传成功: ${formatData(it)}")
                    }
                    .onTechnicalFailure { appendLog("✗ 上传失败: ${it.message}") }

                tempFile.delete()
            }
        }
    }

    // ==================== 响应映射 & 业务请求 ====================

    private fun setupResponseMappingButtons() {
        // 1. executeRequest — 标准业务请求（GlobalResponse + ResponseFieldMapping）
        binding.btnExecuteRequest.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ executeRequest 演示（GlobalResponse + ResponseFieldMapping）")
                appendLog("  当前 mapping: codeKey=url, msgKey=origin, dataKey=headers")
                appendLog("  httpbin 返回 {url, origin, headers, args}")
                appendLog("  → 自动映射为 GlobalResponse(code, msg, data)")

                val result = networkExecutor.executeRequest { sampleApi.testMappedGet() }

                result
                    .onSuccess { data ->
                        appendLog("✓ 业务成功 (code=successCode)")
                        appendLog("  msg (origin IP): ${result.fold({ "" }, { it.message }, { _, msg -> msg })}")
                        appendLog("  data (headers): ${formatData(data)}")
                    }
                    .onBusinessFailure { code, msg ->
                        appendLog("✗ 业务失败: [$code] $msg")
                    }
                    .onTechnicalFailure { appendLog("✗ 技术错误: ${it.message}") }
            }
        }

        // 2. ResponseFieldMapping — 演示非标响应字段映射的原理
        binding.btnResponseMapping.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ ResponseFieldMapping 演示")
                appendLog("────────────────────────────────────")
                appendLog("当前全局映射配置（在 SampleNetworkModule 中）：")
                val mapping = configProvider.current.responseFieldMapping
                appendLog("  codeKey = \"${mapping.codeKey}\"")
                appendLog("  msgKey  = \"${mapping.msgKey}\"")
                appendLog("  dataKey = \"${mapping.dataKey}\"")
                appendLog("  自定义 codeValueConverter: 非空→success, 空→failure")
                appendLog("")
                appendLog("若后端返回 {status, message, result} 样式，只需修改：")
                appendLog("  ResponseFieldMapping(")
                appendLog("    codeKey = \"status\",")
                appendLog("    msgKey  = \"message\",")
                appendLog("    dataKey = \"result\",")
                appendLog("    codeValueConverter = { raw, m ->")
                appendLog("      if (raw == true) m.successCode else m.failureCode")
                appendLog("    }")
                appendLog("  )")
                appendLog("")
                appendLog("▶ 使用当前 mapping 发起请求…")

                networkExecutor.executeRequest { sampleApi.testMappedGet() }
                    .onSuccess { headers ->
                        appendLog("✓ 映射解析成功！")
                        appendLog("  code → successCode (url 非空)")
                        appendLog("  msg  → \"${headers?.size ?: 0} 个 headers\"")
                        headers?.entries?.take(3)?.forEach { (k, v) ->
                            appendLog("  header: $k = ${v.take(40)}")
                        }
                    }
                    .onTechnicalFailure { appendLog("✗ 技术错误: ${it.message}") }
            }
        }

        // 3. BusinessFailure — 模拟业务失败
        binding.btnBusinessFailure.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ BusinessFailure 演示")
                appendLog("  使用 @SuccessCode(200) 但 mapping 的 codeValueConverter")
                appendLog("  将 httpbin 的 url 字段（字符串）解析为 0,")
                appendLog("  期望 successCode=200 → 会触发 BusinessFailure")

                networkExecutor.executeRequest(successCode = 200) { sampleApi.testMappedGet() }
                    .onSuccess { appendLog("✓ 业务成功（不应该到这里）") }
                    .onBusinessFailure { code, msg ->
                        appendLog("✓ 捕获 BusinessFailure:")
                        appendLog("  code = $code (期望 200 但实际为 0)")
                        appendLog("  msg  = $msg")
                    }
                    .onTechnicalFailure { appendLog("✗ 技术错误: ${it.message}") }
            }
        }

        // 4. NetworkResult extensions — fold/map/getOrDefault/getOrNull
        binding.btnResultExtensions.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ NetworkResult 扩展方法演示")

                val result = networkExecutor.executeRawRequest { sampleApi.testGet().string() }

                // fold — 统一处理三种结果
                val summary = result.fold(
                    onSuccess = { "成功: ${it?.length ?: 0} chars" },
                    onTechnicalFailure = { "技术错误: ${it.message}" },
                    onBusinessFailure = { code, msg -> "业务失败: [$code] $msg" }
                )
                appendLog("  fold → $summary")

                // map — 成功时转换数据类型
                val mapped = result.map { it?.length ?: 0 }
                appendLog("  map(String→Int) → length = ${mapped.getOrNull()}")

                // getOrDefault — 失败时提供默认值
                val length = result.map { it?.length ?: 0 }.getOrDefault(0)
                appendLog("  getOrDefault(0) → $length")

                // isSuccess
                appendLog("  isSuccess → ${result.fold({ true }, { false }, { _, _ -> false })}")
            }
        }
    }

    // ==================== 高级特性 ====================

    private fun setupAdvancedButtons() {
        // @BaseUrl — 动态域名切换
        binding.btnBaseUrl.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ @BaseUrl 演示 — 请求 dummyjson.com")
                appendLog("  全局 baseUrl = httpbin.org")
                appendLog("  通过 @BaseUrl(\"https://dummyjson.com/\") 覆盖")

                networkExecutor.executeRawRequest { sampleApi.testBaseUrl().string() }
                    .onSuccess { data ->
                        appendLog("✓ dummyjson 产品详情:")
                        // 手动提取部分字段展示
                        val json = data ?: ""
                        val title = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.getOrNull(1)
                        val price = Regex("\"price\"\\s*:\\s*([\\d.]+)").find(json)?.groupValues?.getOrNull(1)
                        appendLog("  title = $title")
                        appendLog("  price = $price")
                    }
                    .onTechnicalFailure { appendLog("✗ 失败: ${it.message}") }

                appendLog("▶ @BaseUrl — 获取产品列表（限 3 条）")
                networkExecutor.executeRawRequest { sampleApi.testBaseUrlList().string() }
                    .onSuccess { data ->
                        appendLog("✓ 产品列表: ${formatData(data)}")
                    }
                    .onTechnicalFailure { appendLog("✗ 失败: ${it.message}") }
            }
        }

        // extraHeaders — 公共请求头回显
        binding.btnExtraHeaders.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ extraHeaders 演示")
                appendLog("  配置的公共头: X-App-Name=BrickSample, X-App-Version=1.0.0")
                appendLog("  请求 httpbin.org/headers 回显全部请求头…")

                networkExecutor.executeRawRequest { sampleApi.echoHeaders().string() }
                    .onSuccess { data ->
                        appendLog("✓ 服务端收到的 Headers:")
                        val json = data ?: ""
                        // 提取我们关心的自定义头
                        val appName = Regex("\"X-App-Name\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                            .find(json)?.groupValues?.getOrNull(1)
                        val appVersion = Regex("\"X-App-Version\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                            .find(json)?.groupValues?.getOrNull(1)
                        appendLog("  X-App-Name = $appName ✓")
                        appendLog("  X-App-Version = $appVersion ✓")
                    }
                    .onTechnicalFailure { appendLog("✗ 失败: ${it.message}") }
            }
        }

        // 运行时配置切换 — NetworkConfigProvider
        binding.btnRuntimeConfig.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 运行时配置切换演示 (NetworkConfigProvider)")
                val original = configProvider.current
                appendLog("  当前日志级别: ${original.networkLogLevel}")
                appendLog("  当前 extraHeaders: ${original.extraHeaders}")

                // 动态更新配置
                configProvider.update { config ->
                    config.copy(
                        networkLogLevel = NetworkLogLevel.BASIC,
                        extraHeaders = config.extraHeaders + ("X-Request-Id" to "demo-${System.currentTimeMillis()}")
                    )
                }
                val updated = configProvider.current
                appendLog("  → 切换后日志级别: ${updated.networkLogLevel}")
                appendLog("  → 新增 Header: X-Request-Id")

                // 发送请求验证新头被携带
                networkExecutor.executeRawRequest { sampleApi.echoHeaders().string() }
                    .onSuccess { data ->
                        val json = data ?: ""
                        val requestId = Regex("\"X-Request-Id\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                            .find(json)?.groupValues?.getOrNull(1)
                        appendLog("✓ 服务端确认收到 X-Request-Id = $requestId")
                    }
                    .onTechnicalFailure { appendLog("✗ 请求失败: ${it.message}") }

                // 恢复原始配置
                configProvider.updateConfig(original)
                appendLog("  → 已恢复原始配置")
            }
        }

        // retryWithBackoff — 重试机制
        binding.btnRetry.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ retryWithBackoff 演示")
                appendLog("  maxAttempts=3, initialDelay=500ms, factor=2.0")

                var attempt = 0
                try {
                    val result = retryWithBackoff(
                        maxAttempts = 3,
                        initialDelayMillis = 500,
                        factor = 2.0
                    ) {
                        attempt++
                        appendLog("  第 $attempt 次尝试…")
                        if (attempt < 3) {
                            error("模拟失败 (attempt=$attempt)")
                        }
                        sampleApi.testGet().string()
                    }
                    appendLog("✓ 第 $attempt 次成功: ${formatData(result)}")
                } catch (e: Exception) {
                    appendLog("✗ 全部重试失败: ${e.message}")
                }
            }
        }

        // pollingFlow — 轮询
        binding.btnPolling.setOnClickListener {
            if (pollingJob?.isActive == true) {
                pollingJob?.cancel()
                appendLog("▶ 轮询已停止")
                binding.btnPolling.text = "pollingFlow 轮询"
                return@setOnClickListener
            }

            appendLog("▶ pollingFlow 演示 — 每 2s 获取 UUID，共 3 次")
            binding.btnPolling.text = "停止轮询"

            pollingJob = lifecycleScope.launch {
                var count = 0
                pollingFlow(
                    periodMillis = 2000,
                    maxAttempts = 3
                ) {
                    sampleApi.getUuid().string()
                }.collect { data ->
                    count++
                    val uuid = Regex("\"uuid\"\\s*:\\s*\"([^\"]+)\"").find(data)?.groupValues?.getOrNull(1)
                    appendLog("  #$count UUID = ${uuid?.take(20)}…")
                }
                appendLog("✓ 轮询完成（共 $count 次）")
                binding.btnPolling.text = "pollingFlow 轮询"
            }
        }
    }

    // ==================== 网络状态监听 ====================

    private fun setupNetworkMonitorButtons() {
        binding.btnNetworkStatus.setOnClickListener {
            appendLog("▶ NetworkMonitor 网络状态监听")
            appendLog("  当前是否在线: ${networkMonitor.isOnline()}")
            appendLog("  当前网络类型: ${networkMonitor.currentNetworkType()}")
            appendLog("  → 关闭/打开 Wi-Fi 或移动数据以观察变化")

            lifecycleScope.launch {
                networkMonitor.isConnected.collect { connected ->
                    appendLog("  网络状态变化: ${if (connected) "已连接 ✓" else "已断开 ✗"}")
                }
            }
        }

        binding.btnNetworkType.setOnClickListener {
            lifecycleScope.launch {
                appendLog("▶ 实时监听网络类型变化…")
                networkMonitor.networkType.collect { type ->
                    val typeName = when (type) {
                        NetworkType.WIFI -> "Wi-Fi"
                        NetworkType.CELLULAR -> "蜂窝移动"
                        NetworkType.ETHERNET -> "以太网"
                        NetworkType.NONE -> "无网络"
                        NetworkType.OTHER -> "其他"
                    }
                    appendLog("  网络类型: $typeName")
                }
            }
        }
    }

    // ==================== WebSocket ====================

    private fun setupWebSocketButtons() {
        binding.btnWsFullDemo.setOnClickListener {
            startActivity(android.content.Intent(this, WebSocketDemoActivity::class.java))
        }

        binding.btnWsConnect.setOnClickListener {
            appendLog("▶ 正在连接 WebSocket…")
            webSocketManager.connectDefault(
                url = "wss://echo.websocket.org/ws",
                config = WebSocketManager.Config(
                    enableHeartbeat = true,
                    heartbeatIntervalMs = 20_000,
                    wsLogLevel = WebSocketLogLevel.FULL,
                    callbackOnMainThread = true
                ),
                listener = object : WebSocketManager.WebSocketListener {
                    override fun onOpen(connectionId: String) {
                        appendLog("✓ WebSocket 已连接")
                        setWsButtonState(connected = true)
                    }

                    override fun onMessage(connectionId: String, text: String) {
                        appendLog("◀ 收到消息: $text")
                    }

                    override fun onClosing(connectionId: String, code: Int, reason: String) {
                        appendLog("  WebSocket 正在关闭: [$code] $reason")
                    }

                    override fun onClosed(connectionId: String, code: Int, reason: String) {
                        appendLog("✗ WebSocket 已断开: [$code] $reason")
                        setWsButtonState(connected = false)
                    }

                    override fun onFailure(connectionId: String, throwable: Throwable) {
                        appendLog("✗ WebSocket 连接失败: ${throwable.message}")
                        setWsButtonState(connected = false)
                    }

                    override fun onReconnecting(connectionId: String, attempt: Int) {
                        appendLog("  正在重连（第 $attempt 次）…")
                    }
                }
            )
        }

        binding.btnWsDisconnect.setOnClickListener {
            appendLog("▶ 断开 WebSocket…")
            webSocketManager.disconnectDefault(permanent = true)
        }

        binding.btnWsSend.setOnClickListener {
            val text = binding.etWsMessage.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isEmpty()) return@setOnClickListener
            val sent = webSocketManager.sendText(text)
            appendLog("▶ 发送消息: $text （${if (sent) "成功" else "失败"}）")
            binding.etWsMessage.text?.clear()
        }
    }

    private fun setWsButtonState(connected: Boolean) {
        binding.btnWsConnect.isEnabled = !connected
        binding.btnWsDisconnect.isEnabled = connected
        binding.btnWsSend.isEnabled = connected
    }

    // ==================== 日志工具 ====================

    private fun appendLog(message: String) {
        val time = timeFormat.format(Date())
        logBuilder.append("[$time] $message\n")
        binding.tvLog.text = logBuilder.toString()
        binding.scrollLog.post {
            binding.scrollLog.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun formatData(data: Any?): String {
        if (data == null) return "null"
        val str = data.toString()
        return if (str.length > 200) str.take(200) + "…" else str
    }
}
