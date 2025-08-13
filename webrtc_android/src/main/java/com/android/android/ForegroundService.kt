package com.android.android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.MotionEvent
import com.android.mylibrary.SignalingWS
import com.android.mylibrary.WebRTCAndroid
import com.android.mylibrary.logD
import com.android.mylibrary.logI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random


class ForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    private var srcSn: String = ""
    private var targetSn: String = ""
    private var webrtc: WebRTCAndroid? = null
    private lateinit var client: SignalingWS
    override fun onCreate() {
        super.onCreate()
        "ForegroundService 服务启动成功".logI()
        injectInit()
        Consts.srcSn = "webrtc:${Random.nextInt(10, 100)}"
        srcSn = Consts.srcSn
        client = SignalingWS.getInstance(srcSn, onMessageCallback = { webrtcSignaling(it) })
        client.init {
            Consts.wsStatus = it
            "$srcSn $it".logI()
        }
    }

    fun ready() {
        "准备好了->${targetSn}".logD()
        startActivity(
            Intent(
                this, ProxyActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    //统一配置json
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
        encodeDefaults = true
    }

    fun webrtcSignaling(message: String) {
        try {
            if (message.startsWith("{") && message.endsWith("}")) {
                val jsonObjet: JsonObject = json.decodeFromString(message)
                if (jsonObjet.containsKey("type")) {
                    val type = jsonObjet["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "mousedown" -> {
                            val x = jsonObjet["x"]?.jsonPrimitive?.float ?: 0f
                            val y = jsonObjet["y"]?.jsonPrimitive?.float ?: 0f
                            val action = MotionEvent.ACTION_DOWN
                            injectMotionEvent(action, x, y)
                        }

                        "mouseup" -> {
                            val x = jsonObjet["x"]?.jsonPrimitive?.float ?: 0f
                            val y = jsonObjet["y"]?.jsonPrimitive?.float ?: 0f
                            val action = MotionEvent.ACTION_UP
                            injectMotionEvent(action, x, y)
                        }

                        "mousemove" -> {
                            val x = jsonObjet["x"]?.jsonPrimitive?.float ?: 0f
                            val y = jsonObjet["y"]?.jsonPrimitive?.float ?: 0f
                            val action = MotionEvent.ACTION_MOVE
                            injectMotionEvent(action, x, y)
                        }

                        "wheel" -> {
                            val x = jsonObjet["x"]?.jsonPrimitive?.float ?: 0f
                            val y = jsonObjet["y"]?.jsonPrimitive?.float ?: 0f
                            val deltaY = jsonObjet["deltaY"]?.jsonPrimitive?.float ?: 0f
                            injectScrollEvent(x, y, deltaY)
                        }

                        "keydown" -> {
                            val key = jsonObjet["keyCode"]?.jsonPrimitive?.content ?: ""
                            val code = jsonObjet["code"]?.jsonPrimitive?.int ?: 0
                            val action = MotionEvent.ACTION_DOWN
                            injectKeyEvent(action, key, code)
                        }

                        "keyup" -> {
                            val key = jsonObjet["keyCode"]?.jsonPrimitive?.content ?: ""
                            val code = jsonObjet["code"]?.jsonPrimitive?.int ?: 0
                            val action = MotionEvent.ACTION_UP
                            injectKeyEvent(action, key, code)
                        }

                        "ready" -> {//TODO 30S判断一次，如果连接异常的话就主动结束录屏服务
                            targetSn = jsonObjet["srcSn"]?.jsonPrimitive?.content ?: ""
                            ready()
                        }

                        "answer" -> {
                            jsonObjet["sdp"]?.jsonPrimitive?.content?.let {
                                "收到的answer $it".logI()
                                webrtc?.setRemoteSdp(it)
                            }
                        }

                        "candidate" -> {
                            val sdpMid = jsonObjet["sdpMid"]?.jsonPrimitive?.content ?: ""
                            val sdpMLineIndex = jsonObjet["sdpMLineIndex"]?.jsonPrimitive?.int ?: -1
                            val candidate = jsonObjet["candidate"]?.jsonPrimitive?.content ?: ""
                            webrtc?.addIceCandidate(sdpMid, sdpMLineIndex, candidate)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        "onStartCommand ${intent?.action}".logI()
        if (intent?.action == "recording") {
            if (Consts.screenCaptureIntent != null) {
                webrtc = WebRTCAndroid(
                    applicationContext, Consts.screenCaptureIntent!!, client, srcSn, targetSn,
                    p2pState = {},
                    dataState2 = {},
                    dataChange2 = { webrtcSignaling(it) }
                )
            }
        }
        return START_STICKY
    }
}