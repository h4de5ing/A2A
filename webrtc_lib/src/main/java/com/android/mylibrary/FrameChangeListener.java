package com.android.mylibrary;

import org.webrtc.VideoFrame;

public interface FrameChangeListener {
    void onFrameChanged(VideoFrame frame);
}
