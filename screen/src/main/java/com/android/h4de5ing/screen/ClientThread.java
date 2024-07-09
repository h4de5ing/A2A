package com.android.h4de5ing.screen;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientThread extends Thread {
    private DataInputStream dataInputStream;

    @Override
    public void run() {
        super.run();
        try {
            Socket socket = new Socket("10.16.126.38", 7007);
            while (true) {
                dataInputStream = new DataInputStream(socket.getInputStream());
                if (dataInputStream.available() > 0) {
                    byte[] packetSize = new byte[4];
                    dataInputStream.readFully(packetSize, 0, 4);
                    int size = ByteUtils.bytesToInt(packetSize);
                    System.out.println("收到的视频数据大小: " + size);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
