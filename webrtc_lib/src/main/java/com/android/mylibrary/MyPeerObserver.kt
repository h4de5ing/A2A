package com.android.mylibrary

import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.PeerConnectionState
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.RtpReceiver

open class MyPeerObserver : PeerConnection.Observer {

    override fun onSignalingChange(signalingState: SignalingState) {
        "onSignalingChange() called with: signalingState = [$signalingState]".logE()
    }

    override fun onConnectionChange(newState: PeerConnectionState) {
        super.onConnectionChange(newState)
        "onConnectionChange() called with: newState = [$newState]".logE()
    }

    override fun onIceConnectionChange(iceConnectionState: IceConnectionState?) {
        "onIceConnectionChange() called with: iceConnectionState = [$iceConnectionState]".logE()
    }

    override fun onIceConnectionReceivingChange(b: Boolean) {
        "onIceConnectionReceivingChange() called with: b = [$b]".logE()
    }

    override fun onIceGatheringChange(iceGatheringState: IceGatheringState?) {
        "onIceGatheringChange() called with: iceGatheringState = [$iceGatheringState]".logE()
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        "onIceCandidate() called with: iceCandidate = [$iceCandidate]".logE()
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        "onIceCandidatesRemoved() called with: iceCandidates = [${iceCandidates.contentToString()}]".logE()
    }

    override fun onAddStream(mediaStream: MediaStream) {
        "onAddStream() called with: mediaStream = [$mediaStream]".logE()
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        "onRemoveStream() called with: mediaStream = [$mediaStream]".logE()
    }

    override fun onDataChannel(dataChannel: DataChannel) {
        "onDataChannel() called with: dataChannel = [$dataChannel]".logE()
    }

    override fun onRenegotiationNeeded() {
        "onRenegotiationNeeded()".logE()
    }

    override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
        "onAddTrack() called with: rtpReceiver = [${rtpReceiver.id()}], mediaStreams = [${mediaStreams.contentToString()}]".logE()
    }
}
