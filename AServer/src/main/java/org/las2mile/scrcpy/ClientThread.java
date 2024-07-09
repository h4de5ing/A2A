package org.las2mile.scrcpy;

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
            dataInputStream = new DataInputStream(socket.getInputStream());
            while (true) {
                int length = dataInputStream.available();
                if (length > 0) {
                    System.out.println("收到的视频数据长度: " + length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
