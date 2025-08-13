package com.android.mylibrary

import org.json.JSONException
import org.json.JSONObject

class SignalingWS private constructor(
    val username: String,
    val onMessageCallback: (String) -> Unit = {},
) {
    companion object {
        var client: MyWSClient? = null
        private var instance: SignalingWS? = null
        fun getInstance(username: String, onMessageCallback: (String) -> Unit = {}): SignalingWS {
            return instance ?: SignalingWS(username, onMessageCallback = onMessageCallback).apply {
                instance = this
            }
        }
    }

    fun init(status: (String) -> Unit = {}) {
        val url0 = "ws://10.18.16.212:8080/emm_mdm"
        val url1 = "ws://10.18.16.203"
        val url2 = "wss://emcloudy.com"
        client = MyWSClient(
            url = "${url1}/websocket/${username}",
            onOpenCallback = { status("open") },
            onFailureCallback = { _, _ -> status("failed") },
            onClosedCallback = { status("closed") },
            onMessageCallback = onMessageCallback
        )
        client?.connectWebSocket()
    }

    fun sendIceCandidate(
        srcSn: String, targetSn: String, sdpMLineIndex: Int, sdpMid: String, sdp: String
    ) {
        val jo = JSONObject()
        try {
            jo.put("srcSn", srcSn)
            jo.put("targetSn", targetSn)
            jo.put("type", "candidate")
            jo.put("sdpMLineIndex", sdpMLineIndex)
            jo.put("sdpMid", sdpMid)
            jo.put("candidate", sdp)
            client?.send(jo.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendDesc(srcSn: String, targetSn: String, type: String, sdp: String) {
        val jo = JSONObject()
        try {
            jo.put("srcSn", srcSn)
            jo.put("targetSn", targetSn)
            jo.put("type", type)
            jo.put("sdp", sdp)
            client?.send(jo.toString())
            "发送${type}:$sdp".logI()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendStartStream(srcSn: String, targetSn: String) {
        val jo = JSONObject()
        try {
            jo.put("srcSn", srcSn)
            jo.put("targetSn", targetSn)
            jo.put("type", "ready")
            client?.send(jo.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendByteArray(srcSn: String, targetSn: String, byteArray: ByteArray) {
        val jo = JSONObject()
        try {
            jo.put("srcSn", srcSn)
            jo.put("targetSn", targetSn)
            jo.put("type", "data")
            jo.put("data", String(byteArray))
            client?.send(jo.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun send(message: String) {
        client?.send(message)
    }

    fun send(byteArray: ByteArray) {
        client?.send(byteArray)
    }
}