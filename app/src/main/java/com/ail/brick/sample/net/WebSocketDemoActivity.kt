package com.ail.brick.sample.net

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ail.brick.net.websocket.IWebSocketManager
import com.ail.brick.net.websocket.WebSocketLogLevel
import com.ail.brick.net.websocket.WebSocketManager
import com.ail.brick.sample.databinding.ActivityWebsocketDemoBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * WebSocket 专项演示页面
 *
 * 演示内容：
 * - 多连接管理（connect/disconnect/reconnect）
 * - 文本 & 二进制消息收发
 * - 消息队列与离线补发
 * - 心跳、重连策略
 * - WebSocketLogLevel 独立日志控制
 * - 默认单连接快捷 API
 */
@AndroidEntryPoint
class WebSocketDemoActivity : AppCompatActivity() {

    @Inject
    lateinit var wsManager: IWebSocketManager

    private lateinit var binding: ActivityWebsocketDemoBinding

    private val logBuilder = StringBuilder()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val connectionId = "demo_ws"
    private val wsUrl = "wss://echo.websocket.org/ws"

    private val config = WebSocketManager.Config(
        enableHeartbeat = true,
        heartbeatIntervalMs = 20_000,
        enableMessageReplay = true,
        messageQueueCapacity = 50,
        dropOldestWhenQueueFull = true,
        callbackOnMainThread = true,
        wsLogLevel = WebSocketLogLevel.FULL
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebsocketDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupMultiConnectionButtons()
        setupDefaultConnectionButtons()

        appendLog("WebSocket 演示就绪，点击按钮开始操作")
        appendLog("端点: $wsUrl")
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnect(connectionId, permanent = true)
        wsManager.disconnectDefault(permanent = true)
    }

    // ==================== 多连接 API ====================

    private fun setupMultiConnectionButtons() {
        binding.btnWsConnect.setOnClickListener {
            appendLog("▶ 连接 [$connectionId]…")
            wsManager.connect(
                connectionId = connectionId,
                url = wsUrl,
                config = config,
                listener = multiListener
            )
        }
        binding.btnWsReconnect.setOnClickListener {
            val ok = wsManager.reconnect(connectionId)
            appendLog(if (ok) "▶ 手动重连已触发" else "✗ 重连失败（可能已连接）")
        }
        binding.btnWsDisconnect.setOnClickListener {
            wsManager.disconnect(connectionId, permanent = true)
            appendLog("▶ 已断开 [$connectionId]")
            setMultiConnected(false)
        }
        binding.btnWsSendText.setOnClickListener {
            val text = binding.etWsMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ok = wsManager.sendMessage(connectionId, text)
            appendLog("▷ 发送文本${if (ok) "成功" else "失败"}: $text")
        }
        binding.btnWsSendBinary.setOnClickListener {
            val bytes = "binary-demo-${System.currentTimeMillis()}".toByteArray()
            val ok = wsManager.sendMessage(connectionId, bytes)
            appendLog("▷ 发送二进制${if (ok) "成功" else "失败"} (${bytes.size} bytes)")
        }
    }

    private val multiListener = object : WebSocketManager.WebSocketListener {
        override fun onOpen(connectionId: String) {
            appendLog("✓ [$connectionId] 已连接")
            setMultiConnected(true)
        }

        override fun onStateChanged(connectionId: String, oldState: WebSocketManager.State, newState: WebSocketManager.State) {
            appendLog("  [$connectionId] 状态: ${oldState.name} → ${newState.name}")
        }

        override fun onMessage(connectionId: String, text: String) {
            appendLog("◀ [$connectionId] 文本: $text")
        }

        override fun onMessage(connectionId: String, bytes: ByteArray) {
            appendLog("◀ [$connectionId] 二进制: ${bytes.size} bytes")
        }

        override fun onClosing(connectionId: String, code: Int, reason: String) {
            appendLog("  [$connectionId] 正在关闭: [$code] $reason")
        }

        override fun onClosed(connectionId: String, code: Int, reason: String) {
            appendLog("✗ [$connectionId] 已关闭: [$code] $reason")
            setMultiConnected(false)
        }

        override fun onFailure(connectionId: String, throwable: Throwable) {
            appendLog("✗ [$connectionId] 失败: ${throwable.message}")
            setMultiConnected(false)
        }

        override fun onReconnecting(connectionId: String, attempt: Int) {
            appendLog("  [$connectionId] 正在重连（第 $attempt 次）…")
        }

        override fun onHeartbeatTimeout(connectionId: String) {
            appendLog("⚠ [$connectionId] 心跳超时")
        }
    }

    // ==================== 默认单连接快捷 API ====================

    private fun setupDefaultConnectionButtons() {
        binding.btnWsConnectDefault.setOnClickListener {
            appendLog("▶ 连接默认连接…")
            wsManager.connectDefault(
                url = wsUrl,
                config = WebSocketManager.Config(
                    enableHeartbeat = true,
                    heartbeatIntervalMs = 20_000,
                    enableMessageReplay = true,
                    callbackOnMainThread = true,
                    wsLogLevel = WebSocketLogLevel.FULL
                ),
                listener = object : WebSocketManager.WebSocketListener {
                    override fun onOpen(connectionId: String) {
                        appendLog("✓ [default] 已连接")
                        setDefaultConnected(true)
                    }

                    override fun onStateChanged(connectionId: String, oldState: WebSocketManager.State, newState: WebSocketManager.State) {
                        appendLog("  [default] 状态: ${oldState.name} → ${newState.name}")
                    }

                    override fun onMessage(connectionId: String, text: String) {
                        appendLog("◀ [default] 文本: $text")
                    }

                    override fun onMessage(connectionId: String, bytes: ByteArray) {
                        appendLog("◀ [default] 二进制: ${bytes.size} bytes")
                    }

                    override fun onClosed(connectionId: String, code: Int, reason: String) {
                        appendLog("✗ [default] 已关闭: [$code] $reason")
                        setDefaultConnected(false)
                    }

                    override fun onFailure(connectionId: String, throwable: Throwable) {
                        appendLog("✗ [default] 失败: ${throwable.message}")
                        setDefaultConnected(false)
                    }

                    override fun onReconnecting(connectionId: String, attempt: Int) {
                        appendLog("  [default] 正在重连（第 $attempt 次）…")
                    }
                }
            )
        }

        binding.btnWsSendDefault.setOnClickListener {
            val text = binding.etWsMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                Toast.makeText(this, "请输入消息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ok = wsManager.sendText(text)
            appendLog("▷ [default] 发送${if (ok) "成功" else "失败"}: $text")
        }

        binding.btnWsDisconnectDefault.setOnClickListener {
            wsManager.disconnectDefault(permanent = true)
            appendLog("▶ 已断开默认连接")
            setDefaultConnected(false)
        }

        binding.btnWsClearLog.setOnClickListener {
            logBuilder.clear()
            binding.tvWsLog.text = "日志已清空"
        }
    }

    // ==================== UI 状态 ====================

    private fun setMultiConnected(connected: Boolean) {
        binding.btnWsConnect.isEnabled = !connected
        binding.btnWsDisconnect.isEnabled = connected
        binding.btnWsSendText.isEnabled = connected
        binding.btnWsSendBinary.isEnabled = connected
    }

    private fun setDefaultConnected(connected: Boolean) {
        binding.btnWsConnectDefault.isEnabled = !connected
        binding.btnWsDisconnectDefault.isEnabled = connected
        binding.btnWsSendDefault.isEnabled = connected
    }

    // ==================== 日志工具 ====================

    private fun appendLog(message: String) {
        val time = timeFormat.format(Date())
        logBuilder.append("[$time] $message\n")
        val text = logBuilder.toString()
        binding.tvWsLog.text = if (text.length > 8000) text.takeLast(8000) else text
        binding.scrollLog.post {
            binding.scrollLog.fullScroll(View.FOCUS_DOWN)
        }
    }
}
