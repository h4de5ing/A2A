package org.las2mile.scrcpy;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import org.las2mile.scrcpy.decoder.VideoDecoder;
import org.las2mile.scrcpy.model.ByteUtils;
import org.las2mile.scrcpy.model.MediaPacket;
import org.las2mile.scrcpy.model.VideoPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class Scrcpy extends Service {
    private String serverAdr;
    private SurfaceView surfaceView;
    private int screenWidth;
    private int screenHeight;
    private byte[] event = null;
    private VideoDecoder videoDecoder;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final IBinder mBinder = new MyServiceBinder();
    private boolean first_time = true;
    private final AtomicBoolean LetServceRunning = new AtomicBoolean(true);
    private ServiceCallbacks serviceCallbacks;
    private final int[] remote_dev_resolution = new int[2];
    private boolean socket_status = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setServiceCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public void setParms(SurfaceView NewSurface, int NewWidth, int NewHeight) {
        this.screenWidth = NewWidth;
        this.screenHeight = NewHeight;
        this.surfaceView = NewSurface;
        videoDecoder.start();
        updateAvailable.set(true);
    }

    public void start(SurfaceView surface, String serverAdr, int screenHeight, int screenWidth) {
        this.videoDecoder = new VideoDecoder();
        videoDecoder.start();
        this.serverAdr = serverAdr;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.surfaceView = surface;
        Thread thread = new Thread(this::startConnection);
        thread.start();
    }

    public void pause() {
        videoDecoder.stop();
    }

    public void resume() {
        videoDecoder.start();
        updateAvailable.set(true);
    }

    public void StopService() {
        LetServceRunning.set(false);
        stopSelf();
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

    public boolean check_socket_connection() {
        return socket_status;
    }

    public void sendKeyEvent(int keycode) {
        int[] buf = new int[]{keycode};
        byte[] array = new byte[buf.length * 4];
        for (int j = 0; j < buf.length; j++) {
            int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        event = array;
    }

    private void startConnection() {
        videoDecoder = new VideoDecoder();
        videoDecoder.start();
        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;
        Socket socket = null;
        VideoPacket.StreamSettings streamSettings = null;
        int attempts = 50;
        while (attempts != 0) {
            try {
                Log.e("Scrcpy", "Connecting to " + serverAdr);
                socket = new Socket(serverAdr, 7007);
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
                    first_time = false;
                    int i = remote_dev_resolution[0];
                    remote_dev_resolution[0] = remote_dev_resolution[1];
                    remote_dev_resolution[1] = i;
                }
                socket_status = true;
//                Log.e("Remote device res", remote_dev_resolution[0] + " x " + remote_dev_resolution[1]);
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
                            byte[] packet = new byte[size];
                            dataInputStream.readFully(packet, 0, size);
                            VideoPacket videoPacket = VideoPacket.fromArray(packet);
                            if (videoPacket.type == MediaPacket.Type.VIDEO) {
                                byte[] data = videoPacket.data;
                                if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get()) {
                                    if (!updateAvailable.get()) {
                                        streamSettings = VideoPacket.getStreamSettings(data);
                                        if (!first_time) {
                                            if (serviceCallbacks != null) {
                                                serviceCallbacks.loadNewRotation();
                                            }
                                            while (!updateAvailable.get()) {
                                                // Waiting for new surface
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                    updateAvailable.set(false);
                                    Log.e("Scrcpy", "Connecting to " + serverAdr);
                                    videoDecoder.configure(surfaceView.getHolder().getSurface(), screenWidth, screenHeight, streamSettings.sps, streamSettings.pps);
                                } else if (videoPacket.flag == VideoPacket.Flag.END) {
                                    // need close stream
                                } else {
                                    videoDecoder.decodeSample(data, 0, data.length, videoPacket.flag.getFlag());
                                }
                                first_time = false;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                attempts = attempts - 1;
                if (attempts == 0) {
                    socket_status = false;
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
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
    }

    public interface ServiceCallbacks {
        void loadNewRotation();
    }

    public class MyServiceBinder extends Binder {
        public Scrcpy getService() {
            return Scrcpy.this;
        }
    }
}
