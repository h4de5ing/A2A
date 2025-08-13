/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.android.mylibrary;

import android.content.Context;

import androidx.annotation.Nullable;

import org.las2mile.scrcpy.Device;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * An implementation of VideoCapturer to capture the screen content as a video stream.
 * Capturing is done by {@code MediaProjection} on a {@code SurfaceTexture}. We interact with this
 * {@code SurfaceTexture} using a {@code SurfaceTextureHelper}.
 * The {@code SurfaceTextureHelper} is created by the native code and passed to this capturer in
 * {@code VideoCapturer.initialize()}. On receiving a new frame, this capturer passes it
 * as a texture to the native code via {@code CapturerObserver.onFrameCaptured()}. This takes
 * place on the HandlerThread of the given {@code SurfaceTextureHelper}. When done with each frame,
 * the native code returns the buffer to the  {@code SurfaceTextureHelper} to be used for new
 * frames. At any time, at most one frame is being processed.
 */
public class ScreenCaptureAndroid3 implements VideoCapturer, VideoSink {
    private int width;
    private int height;
    @Nullable
    private SurfaceTextureHelper surfaceTextureHelper;
    @Nullable
    private CapturerObserver capturerObserver;
    private long numCapturedFrames;
    private boolean isDisposed;
    private Device device;


    public ScreenCaptureAndroid3() {
        device = new Device();
    }

    private void checkNotDisposed() {
        if (isDisposed) {
            throw new RuntimeException("capturer is disposed.");
        }
    }

    @Override
    @SuppressWarnings("NoSynchronizedMethodCheck")
    public synchronized void initialize(final SurfaceTextureHelper surfaceTextureHelper, final Context applicationContext, final CapturerObserver capturerObserver) {
        checkNotDisposed();

        if (capturerObserver == null) {
            throw new RuntimeException("capturerObserver not set.");
        }
        this.capturerObserver = capturerObserver;

        if (surfaceTextureHelper == null) {
            throw new RuntimeException("surfaceTextureHelper not set.");
        }
        this.surfaceTextureHelper = surfaceTextureHelper;
    }

    @Override
    @SuppressWarnings("NoSynchronizedMethodCheck")
    public synchronized void startCapture(final int width, final int height, final int ignoredFramerate) {
        checkNotDisposed();
        this.width = width;
        this.height = height;
        surfaceTextureHelper.setTextureSize(this.width, this.height);
        capturerObserver.onCapturerStarted(true);
        surfaceTextureHelper.startListening(ScreenCaptureAndroid3.this);
    }

    @Override
    @SuppressWarnings("NoSynchronizedMethodCheck")
    public synchronized void stopCapture() {
        checkNotDisposed();
        ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), () -> {
            surfaceTextureHelper.stopListening();
            capturerObserver.onCapturerStopped();
        });
    }

    @Override
    @SuppressWarnings("NoSynchronizedMethodCheck")
    public synchronized void dispose() {
        isDisposed = true;
    }

    /**
     * Changes output video format. This method can be used to scale the output
     * video, or to change orientation when the captured screen is rotated for example.
     *
     * @param width            new output video width
     * @param height           new output video height
     * @param ignoredFramerate ignored
     */
    @Override
    @SuppressWarnings("NoSynchronizedMethodCheck")
    public synchronized void changeCaptureFormat(final int width, final int height, final int ignoredFramerate) {
    }

    @Override
    public void onFrame(VideoFrame frame) {
        numCapturedFrames++;
        capturerObserver.onFrameCaptured(frame);
    }

    @Override
    public boolean isScreencast() {
        return true;
    }

    public long getNumCapturedFrames() {
        return numCapturedFrames;
    }
}