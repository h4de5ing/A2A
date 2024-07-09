package org.las2mile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.las2mile.scrcpy.decoder.VideoDecoder;
import org.las2mile.scrcpy.model.ByteUtils;
import org.las2mile.scrcpy.model.MediaPacket;
import org.las2mile.scrcpy.model.VideoPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class SurfaceViewActivity extends Activity {
    private SurfaceView surfaceView;
    private final int screenWidth = 800;
    private final int screenHeight = 1280;
    private byte[] event = null;
    private VideoDecoder videoDecoder;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final AtomicBoolean LetServceRunning = new AtomicBoolean(true);
    private final int[] remote_dev_resolution = new int[2];
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private int attempts = 50;
    private VideoPacket.StreamSettings streamSettings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.surface);
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
        TextView ip = findViewById(R.id.ip);
        ip.setText("本机IP:" + Tools.getMyIp());
        surfaceView = findViewById(R.id.decoder_surface);
//        surfaceView.setOnTouchListener((v, event) -> touchEvent(event, surfaceView.getWidth(), surfaceView.getHeight()));
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(7000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (attempts != 0) {
                try {
                    videoDecoder = new VideoDecoder();
                    videoDecoder.start();
                    socket = serverSocket.accept();
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    byte[] packetSize;
                    attempts = 0;
                    byte[] buf = new byte[16];
                    dataInputStream.read(buf, 0, 16);
                    for (int i = 0; i < remote_dev_resolution.length; i++) {
                        remote_dev_resolution[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                                (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                                (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                                ((int) (buf[i * 4 + 3]) & 0xFF);
                    }
                    if (remote_dev_resolution[0] > remote_dev_resolution[1]) {
                        int i = remote_dev_resolution[0];
                        remote_dev_resolution[0] = remote_dev_resolution[1];
                        remote_dev_resolution[1] = i;
                    }
                    Log.e("SurfaceViewActivity", remote_dev_resolution[0] + " x " + remote_dev_resolution[1]);
                    while (LetServceRunning.get()) {
                        try {
                            if (event != null) {
                                dataOutputStream.write(event, 0, event.length);
                                event = null;
                            }

                            if (dataInputStream.available() > 0) {
                                packetSize = new byte[4];
                                dataInputStream.readFully(packetSize, 0, 4);
                                int size = ByteUtils.bytesToInt(packetSize);
                                Log.e("SurfaceViewActivity","视频数据大小:" + size);
                                byte[] packet = new byte[size];
                                dataInputStream.readFully(packet, 0, size);
                                VideoPacket videoPacket = VideoPacket.fromArray(packet);
                                if (videoPacket.type == MediaPacket.Type.VIDEO) {
                                    byte[] data = videoPacket.data;
                                    if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get()) {
                                        if (!updateAvailable.get()) {
                                            streamSettings = VideoPacket.getStreamSettings(data);
                                        }
                                        updateAvailable.set(false);
                                        videoDecoder.configure(surfaceView.getHolder().getSurface(), screenWidth, screenHeight, streamSettings.sps, streamSettings.pps);
                                    } else {
                                        videoDecoder.decodeSample(data, 0, data.length, videoPacket.flag.getFlag());
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("客户端断开了");
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public boolean touchEvent(MotionEvent touch_event, int displayW, int displayH) {
        int[] buf = new int[]{touch_event.getAction(), touch_event.getButtonState(), (int) touch_event.getX() * screenWidth / displayW, (int) touch_event.getY() * screenHeight / displayH};
        byte[] array = new byte[buf.length * 4];
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        event = array;
        return true;
    }
}