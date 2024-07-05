package org.las2mile.scrcpy;

import java.io.IOException;
import java.net.Socket;

public final class DroidConnection extends Thread {
    private static Socket socket = null;
    private Device mDdevice;

    public DroidConnection(Device device) {
        this.mDdevice = device;
    }

    @Override
    public void run() {
        super.run();
        try {
            socket = new Socket("10.16.127.95", 7000);
            Ln.d(">>>>>>Client connect success<<<<<<" + socket.getRemoteSocketAddress());
            new PullEventThread(mDdevice, socket).start();
            new PushVideoThread(mDdevice, socket).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PullEventThread extends Thread {
        private final Socket clientSocket;
        private final Device mDevice;

        public PullEventThread(Device device, Socket socket) {
            this.mDevice = device;
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            super.run();
            try {
                Ln.d("开始推送数据");
                new EventController(mDevice, clientSocket.getInputStream()).control();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class PushVideoThread extends Thread {
        private final Socket clientSocket;
        private final Device mDevice;

        public PushVideoThread(Device device, Socket socket) {
            this.mDevice = device;
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            super.run();
            try {
                ScreenEncoder screenEncoder = new ScreenEncoder(Options.bitRate);
                screenEncoder.streamScreen(mDevice, clientSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

