package com.android.mylibrary

import android.content.Context
import android.util.DisplayMetrics
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory
import org.webrtc.PeerConnectionFactory.InitializationOptions
import org.webrtc.RTCStatsReport
import java.nio.ByteBuffer

abstract class WebRTCInterface(
    val context: Context,
    val dataState: (Boolean) -> Unit = {},
    val dataChange: (String) -> Unit = {}
) {
    val eglBaseContext: EglBase.Context = EglBase.create().eglBaseContext
    var peerConnectionFactory: PeerConnectionFactory? = null
    var peerConnection: PeerConnection? = null
    var screenMetrics = DisplayMetrics()
    val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }
    private var dataChannel: DataChannel? = null

    abstract fun initRTCConfiguration(): RTCConfiguration

    fun initializePeerConnectionFactory() {
        initLog(true, "gh1st")
        val initializationOptions =
            InitializationOptions.builder(context).setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/").createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(eglBaseContext)
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory).createPeerConnectionFactory()
    }

    fun initializePeerConnection(observer: PeerConnection.Observer) {
        peerConnection =
            peerConnectionFactory?.createPeerConnection(initRTCConfiguration(), observer)
        peerConnection?.getStats(object : org.webrtc.RTCStatsCollectorCallback {
            override fun onStatsDelivered(report: RTCStatsReport?) {
                "获取到统计信息:${report.toString()}".logI()
            }
        })
        initDataChannel()
    }

    fun initDataChannel() {
        DataChannel.Init().let {
            it.ordered = true //消息的传递是否有序 true代表有序
            it.negotiated = true //协商方式,2端都可以createDataChannel
            it.id = 0//通道id
            dataChannel = peerConnection?.createDataChannel("data", it)
            dataChannel?.registerObserver(object : MyDateObserver() {
                override fun onStateChange() {
                    super.onStateChange()
                    dataState(dataChannel?.state() == DataChannel.State.OPEN)
                    "数据通道状态变化时触发 ${dataChannel?.state()}".logI()
                }

                override fun onMessage(buffer: DataChannel.Buffer?) {
                    super.onMessage(buffer)
                    buffer?.data?.let { data ->
                        val bytes = ByteArray(data.remaining())
                        data.get(bytes)
                        val message = String(bytes)
                        dataChange(message)
                        "收到的消息:${message}".logI()
                    }
                }
            })
        }
    }

    fun sendData(data: String) {
        val binary = false//是否二进制数据，默认为false
        dataChannel?.send(DataChannel.Buffer(ByteBuffer.wrap(data.toByteArray()), binary))
    }

    fun initPeerIceServer(): List<PeerConnection.IceServer> {
        return buildList {
//            add(
//                PeerConnection.IceServer.builder("turn:43.139.96.173:3478").setUsername("admin123").setPassword("admin123").createIceServer()
//            )
//            add(
//                PeerConnection.IceServer.builder("turns:43.139.96.173:5349").setUsername("admin123").setPassword("admin123").createIceServer()
//            )
            add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        }
    }

    fun addIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        peerConnection?.addIceCandidate(
            IceCandidate(sdpMid, sdpMLineIndex, candidate)
        )
    }
}