package com.bbogush.web_screen;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebRtcManager {
    private static final String TAG = "WebRtcManager";
    private static final int FRAMES_PER_SECOND = 30;
    private static final String SDP_PARAM = "sdp";
    private static final String ICE_PARAM = "ice";

    private VideoCapturer videoCapturer;
    private PeerConnectionFactory peerConnectionFactory;
    private VideoTrack localVideoTrack;
    private PeerConnection localPeer = null;
    private final HttpServer server;
    private final List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
    private final Display display;
    private DisplayMetrics screenMetrics = new DisplayMetrics();
    private Thread rotationDetectorThread = null;

    public WebRtcManager(Intent intent, Context context, HttpServer server) {
        this.server = server;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        createMediaProjection(intent);
        initWebRTC(context);
    }


    private void createMediaProjection(Intent intent) {
        videoCapturer = new ScreenCaptureAndroid2(intent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.e(TAG, "User has revoked media projection permissions");
            }
        });
    }

    private void initWebRTC(Context context) {
        EglBase rootEglBase = EglBase.create();
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options).setVideoEncoderFactory(defaultVideoEncoderFactory).setVideoDecoderFactory(defaultVideoDecoderFactory).createPeerConnectionFactory();

        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());

        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        display.getRealMetrics(screenMetrics);
        if (videoCapturer != null) {
            videoCapturer.startCapture(screenMetrics.widthPixels, screenMetrics.heightPixels, FRAMES_PER_SECOND);
            //startRotationDetector();
        }
    }

    public void start(HttpServer server) {
        Log.d(TAG, "WebRTC start");
        createPeerConnection();
        doCall(server);
    }


    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(peerIceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new MyPeerObserver() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                Log.d(TAG, "onAddStream Unexpected remote stream received.");
            }
        });

        addStreamToLocalPeer();
    }

    public void onIceCandidateReceived(IceCandidate iceCandidate) {
        JSONObject messageJson = new JSONObject();
        JSONObject iceJson = new JSONObject();
        try {
            iceJson.put("type", "candidate");
            iceJson.put("label", iceCandidate.sdpMLineIndex);
            iceJson.put("id", iceCandidate.sdpMid);
            iceJson.put("candidate", iceCandidate.sdp);

            messageJson.put("type", "ice");
            messageJson.put("ice", iceJson);

            String messageJsonStr = messageJson.toString();
            server.send(messageJson.toString());
            Log.d(TAG, "Send ICE candidates: " + messageJsonStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addStreamToLocalPeer() {
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);
    }

    private void doCall(HttpServer server) {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        localPeer.createOffer(new MySdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new MySdpObserver(), sessionDescription);

                JSONObject messageJson = new JSONObject();
                JSONObject sdpJson = new JSONObject();
                try {
                    sdpJson.put("type", sessionDescription.type.canonicalForm());
                    sdpJson.put("sdp", sessionDescription.description);

                    messageJson.put("type", "sdp");
                    messageJson.put("sdp", sdpJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                String messageJsonStr = messageJson.toString();
                try {
                    server.send(messageJsonStr);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                Log.d(TAG, "Send SDP: " + messageJsonStr);
            }
        }, sdpConstraints);
    }

    public void onAnswerReceived(JSONObject data) {
        JSONObject json;
        try {
            json = data.getJSONObject(SDP_PARAM);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "Remote SDP received: " + json);
        try {
            localPeer.setRemoteDescription(new MySdpObserver(), new SessionDescription(SessionDescription.Type.fromCanonicalForm(json.getString("type").toLowerCase()), json.getString("sdp")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onIceCandidateReceived(JSONObject data) {
        JSONObject json;
        try {
            json = data.getJSONObject(ICE_PARAM);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        Log.d(TAG, "ICE candidate received: " + json);
        try {
            localPeer.addIceCandidate(new IceCandidate(json.getString("id"), json.getInt("label"), json.getString("candidate")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startRotationDetector() {
        Runnable runnable = () -> {
            Log.d(TAG, "Rotation detector start");
            display.getRealMetrics(screenMetrics);
            while (true) {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getRealMetrics(metrics);
                if (metrics.widthPixels != screenMetrics.widthPixels || metrics.heightPixels != screenMetrics.heightPixels) {
                    Log.d(TAG, "Rotation detected\n" + "w=" + metrics.widthPixels + " h=" + metrics.heightPixels + " d=" + metrics.densityDpi);
                    screenMetrics = metrics;
                    if (videoCapturer != null) {
                        try {
                            videoCapturer.stopCapture();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        videoCapturer.startCapture(screenMetrics.widthPixels, screenMetrics.heightPixels, FRAMES_PER_SECOND);
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Rotation detector exit");
                    Thread.interrupted();
                    break;
                }
            }
        };
        rotationDetectorThread = new Thread(runnable);
        rotationDetectorThread.start();
    }

    private void stopRotationDetector() {
        rotationDetectorThread.interrupt();
    }

    public void close() {
        stop();
        stopRotationDetector();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        try {
            videoCapturer.stopCapture();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        videoCapturer = null;
    }

    public void stop() {
        Log.d(TAG, "WebRTC stop");
        if (localPeer == null) return;
        localPeer.close();
        localPeer = null;
    }
}
