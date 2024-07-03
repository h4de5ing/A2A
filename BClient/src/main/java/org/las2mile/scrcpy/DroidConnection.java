package org.las2mile.scrcpy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class DroidConnection extends Thread {
    private ServerSocket serverSocket = null;
    private boolean isRunning = false;

    private static Socket socket = null;
    private Device mDdevice;

    public DroidConnection(Device device) {
        this.mDdevice = device;
    }

    @Override
    public void run() {
        super.run();
        try {
            serverSocket = new ServerSocket(7007);
            isRunning = true;
            Ln.d(">>>>>>Screen Share ServerThread<<<<<<");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (isRunning) {
            try {
                Ln.d(">>>>>>Client connect success<<<<<<");
                socket = serverSocket.accept();
                new PullEventThread(mDdevice, socket).start();
                new PushVideoThread(mDdevice, socket).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

