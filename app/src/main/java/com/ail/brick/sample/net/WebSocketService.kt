package com.ail.brick.sample.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ail.brick.net.websocket.IWebSocketManager
import com.ail.brick.net.websocket.WebSocketLogLevel
import com.ail.brick.net.websocket.WebSocketManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {

    @Inject
    lateinit var wsManager: IWebSocketManager

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务（避免被系统杀死）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ws_channel"
            val channel = NotificationChannel(
                channelId,
                "WebSocket 连接",
                NotificationManager.IMPORTANCE_LOW // 后台服务用 LOW 即可
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("正在保持连接")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()

            startForeground(1, notification)
        }

        connectWebSocket()
        return START_STICKY
    }

    private fun connectWebSocket() {
        wsManager.connectDefault(
            url = "wss://echo.websocket.org/ws",
            config = WebSocketManager.Config(
                enableHeartbeat = true,
                heartbeatIntervalMs = 20_000,
                callbackOnMainThread = true,
                wsLogLevel = WebSocketLogLevel.BASIC
            ),
            listener = object : WebSocketManager.WebSocketListener {
                override fun onOpen(connectionId: String) {
                }

                override fun onMessage(connectionId: String, text: String) {
                    Log.d("WS", "connectionId == " + connectionId + " onMessage == " + text)
                    // 可通过 EventBus / Broadcast 通知 UI
//                    val intent = Intent("WS_MESSAGE").putExtra("text", text)
//                    LocalBroadcastManager.getInstance(this@WebSocketService).sendBroadcast(intent)
                }

                override fun onClosed(connectionId: String, code: Int, reason: String) {
                }

                override fun onFailure(connectionId: String, throwable: Throwable) {
                }

            }
        )
    }

    fun sendMessage(text: String) {
        wsManager.sendText(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnectDefault(permanent = true)
    }
}