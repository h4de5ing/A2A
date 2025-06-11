package org.las2mile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    public static String getMyIp() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isLoopback()) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            sb.append(inetAddress.getHostAddress()).append("\n");
                            break;
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return sb.toString();
    }

    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private TextView ip;
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ip = findViewById(R.id.ip);
        tv = findViewById(R.id.tv);
        new Thread(() -> {
            DroidConnection connection = new DroidConnection(new Device());
            connection.start();
            while (true) {
                try {
                    Thread.sleep(16);
                    runOnUiThread(() -> tv.setText(getCurrentTimeWithMillis() + "\n" + Conts.frameCount));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        runOnUiThread(() -> ip.setText("本机IP:" + getMyIp()));
    }

    private String getCurrentTimeWithMillis() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }


    private void testClient() {
        new Thread(() -> {
            try {
                socket = new Socket("10.16.127.111", 7000);
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
                while (true) {
                    try {
                        byte[] bytes = new byte[1024];
                        int len = inputStream.read(bytes);
                        if (len > 0) {
                            String str = new String(bytes, 0, len);
                            System.out.println("收到服务器数据:" + str);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    outputStream.write("hello server ".getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000);
    }
}