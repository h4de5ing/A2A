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
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    public static String getMyIp() {
        String ip = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback()) {
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        if (inetAddress instanceof Inet4Address) {
                            ip = inetAddress.getHostAddress();
                            break;
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return ip;
    }

    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.ip);
        tv.setText("本机IP:" + getMyIp());
        new Thread(() -> {
            DroidConnection connection = new DroidConnection(new Device());
            connection.start();
        }).start();
    }

    private void testClient() {
        new Thread(() -> {
            try {
                socket = new Socket("10.16.127.95", 7000);
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