package com.android.web

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.android.mylibrary.Consts
import com.android.mylibrary.SignalingWS
import com.android.mylibrary.WebRTCWeb
import com.android.web.databinding.ActivityMainBinding
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : Activity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var srcSn: String = "webrtc:100"
    private lateinit var webSocket: SignalingWS
    private var webrtc: WebRTCWeb? = null
    private var widthHeight = ""
    private var count = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        srcSn = "webrtc:${Random.nextInt(10, 100)}"
        webSocket = SignalingWS.getInstance(srcSn) {
            val data = JSONObject(it)
            if (data.has("type")) {
                val type = data.optString("type")
                when (type) {
                    "offer" -> {
                        val sdp = data.optString("sdp")
                        webrtc?.setRemoteSdp(sdp)
                        webrtc?.createAnswer()
                    }

                    "candidate" -> {
                        val sdpMid = data.optString("sdpMid")
                        val sdpMLineIndex = data.optInt("sdpMLineIndex")
                        val candidate = data.optString("candidate")
                        webrtc?.addIceCandidate(sdpMid, sdpMLineIndex, candidate)
                    }
                }
            }
        }
        webSocket.init { runOnUiThread { setTitle("Web [$srcSn] $it,${widthHeight}") } }
        binding.call.setOnClickListener {
            val target = binding.etTarget.text.toString()
            if (target.isEmpty()) {
                binding.etTarget.error = "请输入目标SN"
            } else {
                Consts.targetSn = target
                webSocket.sendStartStream(srcSn, Consts.targetSn)
            }
        }
        webrtc = WebRTCWeb(
            applicationContext,
            binding.surface,
            webSocket,
            srcSn,
            p2pState = { isConnected ->
                runOnUiThread {
                    binding.call.text = if (isConnected) "挂断" else "连接"
                    binding.surface.visibility = if (isConnected) View.VISIBLE else View.GONE
                }
            },
            dataChange2 = {
                runOnUiThread { binding.ping.text = "0" }
            }, videoSizeChange = {
                count++
                runOnUiThread { binding.frameCount.text = count.toString() }
            })
        binding.frameCount.text = count.toString()
    }
}