package org.las2mile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.las2mile.scrcpy.model.ByteUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity2 extends Activity {
    private TextView tvData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        TextView ip = findViewById(R.id.ip);
        tvData = findViewById(R.id.data);
        ip.setText("本机IP:" + Tools.getMyIp());
//        new ServerSocketThread(new DataChangeListener() {
//            @Override
//            public void onDataChange(byte[] data) {
//                runOnUiThread(() -> tvData.setText("收到数据:" + System.currentTimeMillis() + " ," + data.length));
//            }
//
//            @Override
//            public void onDisconnect() {
//                tvData.setText("客户端断开连接");
//            }
//        }).start();
//        new ClientThread().start();
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(7000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (attempts != 0) {
                try {
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
                    Log.e("SurfaceViewActivity", "屏幕分辨率:" + remote_dev_resolution[0] + " x " + remote_dev_resolution[1]);
                    while (LetServceRunning.get()) {
                        if (dataInputStream.available() > 0) {
                            packetSize = new byte[4];
                            dataInputStream.readFully(packetSize, 0, 4);
                            int size = ByteUtils.bytesToInt(packetSize);
                            Log.e("SurfaceViewActivity", "视频数据大小:" + size);
                            byte[] packet = new byte[size];
                            dataInputStream.readFully(packet, 0, size);
                        }
                    }
                } catch (Exception e) {
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

    private ServerSocket serverSocket = null;
    private int attempts = 50;
    private Socket socket = null;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private final int[] remote_dev_resolution = new int[2];
    private final AtomicBoolean LetServceRunning = new AtomicBoolean(true);

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
        private DataInputStream dataInputStream;

        public PullDataThread(Socket socket, DataChangeListener dataChangeListener) throws IOException {
            this.socket = socket;
            this.dataChangeListener = dataChangeListener;
        }

        @Override
        public void run() {
            try {
                //TODO 将Socket添加到队列
                while (true) {
                    dataInputStream = new DataInputStream(socket.getInputStream());
//                    if(dataInputStream.available() > 0){
//                    }
                    byte[] packetSize = new byte[4];
                    dataInputStream.readFully(packetSize, 0, 4);
                    int size = ByteUtils.bytesToInt(packetSize);
//                    byte[] newData = Arrays.copyOfRange(buffer, 0, len);
//                    dataChangeListener.onDataChange(newData);
                    System.out.println("收到客户端数据:" + size);
                }
            } catch (Exception e) {
                dataChangeListener.onDisconnect();
                e.printStackTrace();
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