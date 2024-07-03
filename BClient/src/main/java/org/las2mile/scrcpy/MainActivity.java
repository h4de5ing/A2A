package org.las2mile.scrcpy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 服务端逻辑
 * 1.服务端，发送被控的设备信息给客户端，设备名称，SN号，屏幕宽高,帧率等信息
 * 2.开启ServerSocket并等待客户端连接
 * 3.客户端连接成功获取客户端的IP地址，并向客户端推送数据
 * 4.客户端连接失败，关闭ServerSocket
 * <p>
 * <p>
 * 客户端逻辑
 * 1.先给服务端发送消息我要准备连接你了，等待客户端回应
 * 2.收到客户端给的设备名称，SN号，屏幕宽高等信息
 * 3.开启Socket连接服务端，并等待服务端推送数据
 * 4.渲染UI
 */
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
}