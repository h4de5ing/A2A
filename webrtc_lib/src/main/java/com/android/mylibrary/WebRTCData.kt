package com.android.mylibrary

import android.content.Context
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class WebRTCData(
    context2: Context,
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
            }
        })
        createOffer()
    }

    override fun initRTCConfiguration(): PeerConnection.RTCConfiguration =
        PeerConnection.RTCConfiguration(initPeerIceServer())

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
}