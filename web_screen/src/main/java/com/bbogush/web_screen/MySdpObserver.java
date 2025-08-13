package com.bbogush.web_screen;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

class MySdpObserver implements SdpObserver {
    private final String tag = "WebRtcManager";

    MySdpObserver() {
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(tag, "onCreateSuccess() called with");
//        Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription.description + "]");
    }

    @Override
    public void onSetSuccess() {
        Log.d(tag, "onSetSuccess() called");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
    }
}
