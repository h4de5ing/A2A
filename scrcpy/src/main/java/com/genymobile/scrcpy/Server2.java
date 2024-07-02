package com.genymobile.scrcpy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server2 {

    public static final String SERVER_PATH;

    static {
        String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        SERVER_PATH = classPaths[0];
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Server3............" + SERVER_PATH);
        new Thread(() -> {
            Socket socket;
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(7000);
                socket = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    System.out.println("got it : " + temp);
                }
            } catch (IOException e) {
                System.out.println("Event controller stopped");
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.out.println("Event finally stopped");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
