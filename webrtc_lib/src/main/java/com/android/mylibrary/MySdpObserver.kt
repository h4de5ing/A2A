package com.android.mylibrary

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

internal open class MySdpObserver : SdpObserver {

    override fun onCreateSuccess(sessionDescription: SessionDescription) {
        "onCreateSuccess() called with".logE()
//        "onCreateSuccess() called with: sessionDescription = [" + sessionDescription.description + "]".logE()
    }

    override fun onSetSuccess() {
        "onSetSuccess() called".logE()
    }

    override fun onCreateFailure(s: String) {
        "onCreateFailure() called with: s = [$s]".logE()
    }

    override fun onSetFailure(s: String) {
        "onSetFailure() called with: s = [$s]".logE()
    }
}
