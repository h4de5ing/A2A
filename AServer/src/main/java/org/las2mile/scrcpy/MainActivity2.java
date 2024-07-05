package org.las2mile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class MainActivity2 extends Activity {
    private TextView tvData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        TextView ip = findViewById(R.id.ip);
        tvData = findViewById(R.id.data);
        ip.setText("本机IP:" + Tools.getMyIp());
        new ServerSocketThread(new DataChangeListener() {
            @Override
            public void onDataChange(byte[] data) {
                runOnUiThread(() -> tvData.setText("收到数据:" + System.currentTimeMillis()));
            }

            @Override
            public void onDisconnect() {
                tvData.setText("客户端断开连接");
            }
        }).start();
    }

    class ServerSocketThread extends Thread {
        private ServerSocket serverSocket = null;
        private DataChangeListener dataChangeListener;
        private boolean isRunning = false;

        public ServerSocketThread(DataChangeListener dataChangeListener) {
            try {
                this.dataChangeListener = dataChangeListener;
                serverSocket = new ServerSocket(7000);
                isRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println(">>>>>>Client connect success<<<<<<" + socket.getInetAddress() + ":" + socket.getPort());
//                    socket.getOutputStream().write("Hello Client".getBytes());
                    new PullDataThread(socket, dataChangeListener).start();
                } catch (IOException e) {
                    dataChangeListener.onDisconnect();
                    e.printStackTrace();
                }
            }
        }
    }

    class PullDataThread extends Thread {
        private final Socket socket;
        private final DataChangeListener dataChangeListener;

        public PullDataThread(Socket socket, DataChangeListener dataChangeListener) {
            this.socket = socket;
            this.dataChangeListener = dataChangeListener;
        }

        @Override
        public void run() {
            try {
                //TODO 将Socket添加到队列
                byte[] buffer = new byte[1024];
                int len;
                while ((len = socket.getInputStream().read(buffer)) != -1) {
                    byte[] newData = Arrays.copyOfRange(buffer, 0, len);
                    dataChangeListener.onDataChange(newData);
                    System.out.println("收到客户端数据:" + new String(buffer, 0, len));
                }
            } catch (Exception e) {
                dataChangeListener.onDisconnect();
//                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}