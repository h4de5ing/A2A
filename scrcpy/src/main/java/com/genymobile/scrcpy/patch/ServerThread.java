package com.genymobile.scrcpy.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public ServerThread(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        isRunning = true;
        System.out.println(">>>>>>Shell ServerThread<<<<<<");
        while (isRunning) {
            try {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        isRunning = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        private void sendText(String text) {
            try {
                clientSocket.getOutputStream().write(text.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);
                    try {
                        ServiceShellUtils.ServiceShellCommandResult sr = ServiceShellUtils.execCommand(inputLine, false);
                        System.out.println(sr.result + " " + sr.successMsg);
                        if (sr.result == 0) {
                            sendText("###ShellOK#" + sr.successMsg);
                        } else {
                            sendText("###ShellError#" + sr.errorMsg);
                        }
                    } catch (Exception e) {
                        sendText("###CodeError#" + e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}