package com.android.mylibrary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

class MyWSClient(
    val url: String,
    val onOpenCallback: () -> Unit = {},
    val onFailureCallback: (Throwable, Response?) -> Unit = { _, _ -> },
    val onClosedCallback: () -> Unit = {},
    val onMessageCallback: (String) -> Unit = {}
) {
    private var webSocket: WebSocket? = null
    private var open = false
    fun isOpen(): Boolean = open
    fun connectWebSocket() {
        try {
            val request = Request.Builder().url(url).build()
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    "onOpen:${url}".logI()
                    open = true
                    onOpenCallback()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    "onFailure:${t}".logE()
                    open = false
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(5000)
                        connectWebSocket()
                    }
                    onFailureCallback(t, response)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    "收到：${text}".logI()
                    onMessageCallback(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    super.onMessage(webSocket, bytes)
                    "收到字节流".logI()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    "onClosed:${code},${reason}".logE()
                    open = false
                    onClosedCallback()
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(5000)
                        connectWebSocket()
                    }
                }
            }
            webSocket = OkHttpClient().newWebSocket(request, listener)
        } catch (e: Exception) {
            open = false
            e.printStackTrace()
        }
    }

    fun send(data: String) {
        "发送:${data}".logI()
        webSocket?.send(data)
    }

    fun send(data: ByteArray) {
        "发送字节流 ${data.size}".logI()
        webSocket?.send(data.toByteString())
    }
}