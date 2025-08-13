package com.android.mylibrary

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.view.Display
import android.view.WindowManager
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class WebRTCAndroid(
    context2: Context,
    val intent: Intent,
    val client: SignalingWS,
    val srcSn: String,
    val targetSn: String,
    val p2pState: (Boolean) -> Unit = {},
    val dataState2: (Boolean) -> Unit = {},
    val dataChange2: (String) -> Unit = {},
) : WebRTCInterface(
    context = context2,
    dataState = dataState2,
    dataChange = dataChange2
) {
    private var localStream: MediaStream? = null
    private var localVideoTrack: VideoTrack? = null
    private var display: Display? = null
    private var videoCapture: ScreenCaptureAndroid2? = null
    private var videoSource: VideoSource? = null

    init {
        initLog(true, "WebRTCAndroid")
        initializePeerConnectionFactory()
        initializePeerConnection(object : MyPeerObserver() {
            override fun onIceCandidate(iceCandidate: IceCandidate) {
                super.onIceCandidate(iceCandidate)
                "onIceCandidate ${iceCandidate.sdp}".logI()
                client.sendIceCandidate(
                    srcSn,
                    targetSn,
                    iceCandidate.sdpMLineIndex,
                    iceCandidate.sdpMid,
                    iceCandidate.sdp
                )
            }


            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                super.onConnectionChange(newState)
                p2pState(newState == PeerConnection.PeerConnectionState.CONNECTED)
                "P2P状态发生变化:onConnectionChange: $newState".logI()
                when (newState) {
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.CLOSED -> {
                        onDestroy()
                    }

                    else -> {}
                }
            }
        })
        videoSource =
            peerConnectionFactory?.createVideoSource(videoCapture?.isScreencast == true, true)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
        localStream?.addTrack(localVideoTrack)
        startScreenCapture()
        createOffer()
    }

    override fun initRTCConfiguration(): PeerConnection.RTCConfiguration =
        PeerConnection.RTCConfiguration(initPeerIceServer()).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED //禁用TCP候选者，减少穿透问题
//            disableIpv6 = true //禁用IPv6，减少穿透问题
//            disableIPv6OnWifi = true //禁用IPv6，减少穿透问题
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE //强制素偶又流共用一个端口，减少端口占用
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE //强制RTCP和RTP复用

            continualGatheringPolicy =
                PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY //动态使用网络变化
            keyType = PeerConnection.KeyType.ECDSA
        }

    private var count = 0L
    fun startScreenCapture() {
        videoCapture = ScreenCaptureAndroid2(intent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                "屏幕共享停止".logI()
            }
        })
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
        videoCapture?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        display = wm.defaultDisplay
        display?.getRealMetrics(screenMetrics)
        videoCapture?.startCapture(screenMetrics.widthPixels, screenMetrics.heightPixels, 0)
        localStream = peerConnectionFactory?.createLocalMediaStream("102")
        localStream?.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
        videoCapture?.setFrameChangeListener {
            count++
            Consts.frameCountFlow.value = count
        }
    }

    fun createOffer() {
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                super.onCreateSuccess(sessionDescription)
                peerConnection?.setLocalDescription(MySdpObserver(), sessionDescription)
                client.sendDesc(
                    srcSn,
                    targetSn,
                    sessionDescription.type.canonicalForm(),
                    sessionDescription.description
                )
            }
        }, mediaConstraint)
    }

    fun setRemoteSdp(sdp: String) {
        peerConnection?.setRemoteDescription(
            MySdpObserver(), SessionDescription(
                SessionDescription.Type.ANSWER, sdp
            )
        )
    }

    fun onDestroy() {
        try {
            videoCapture?.stopCapture()
            videoCapture?.dispose()
            localStream?.removeTrack(localVideoTrack)
            localVideoTrack?.dispose()
            localStream?.dispose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}