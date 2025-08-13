package com.android.mylibrary

import android.content.Context
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoTrack

class WebRTCWeb(
    context2: Context,
    val videoView: SurfaceViewRenderer,
    val client: SignalingWS,
    val srcSn: String,
    val p2pState: (Boolean) -> Unit = {},
    val dataState2: (Boolean) -> Unit = {},
    val dataChange2: (String) -> Unit = {},
    val videoSizeChange: (VideoFrame) -> Unit = {},
) : WebRTCInterface(
    context = context2,
    dataState = dataState2,
    dataChange = dataChange2
) {
    init {
        initLog(true, "WebRTCAndroid")
        initializePeerConnectionFactory()
        initializePeerConnection(object : MyPeerObserver() {
            override fun onIceCandidate(iceCandidate: IceCandidate) {
                super.onIceCandidate(iceCandidate)
                "onIceCandidate ${iceCandidate.sdp}".logI()
                client.sendIceCandidate(
                    srcSn,
                    Consts.targetSn,
                    iceCandidate.sdpMLineIndex,
                    iceCandidate.sdpMid,
                    iceCandidate.sdp
                )
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                super.onConnectionChange(newState)
                p2pState(newState == PeerConnection.PeerConnectionState.CONNECTED)
                "P2P状态发生变化:onConnectionChange: $newState".logI()
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                "收到视频流=${transceiver?.receiver?.id()}".logI()
                if (transceiver?.receiver != null) {
                    if (transceiver.receiver?.track() is VideoTrack) {
                        val videoTrack = transceiver.receiver?.track() as VideoTrack
                        videoTrack.addSink { videoSizeChange(it) }
                        videoTrack.addSink(videoView)
                    }
                }
            }
        })
        initView()
    }

    override fun initRTCConfiguration(): RTCConfiguration =
        RTCConfiguration(initPeerIceServer()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN //支持多轨流
        }

    fun initView() {
        videoView.init(eglBaseContext, null)
        videoView.setEnableHardwareScaler(true)
        videoView.setMirror(false)
    }

    fun createAnswer() {
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(MySdpObserver(), sessionDescription)
                client.sendDesc(
                    srcSn,
                    Consts.targetSn,
                    sessionDescription.type.canonicalForm(),
                    sessionDescription.description
                )
            }
        }, mediaConstraint)
    }

    fun setRemoteSdp(sdp: String) {
        peerConnection?.setRemoteDescription(
            MySdpObserver(), SessionDescription(SessionDescription.Type.OFFER, sdp)
        )
    }
}