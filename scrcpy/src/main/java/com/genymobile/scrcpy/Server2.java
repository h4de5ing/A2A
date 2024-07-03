package com.genymobile.scrcpy;

import com.genymobile.scrcpy.patch.ServerThread;

import java.io.File;

public class Server2 {

    public static final String SERVER_PATH;

    static {
        String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        SERVER_PATH = classPaths[0];
    }

    public static void main(String[] args) throws Exception {
        Ln.d("Server2222............" + SERVER_PATH);
        System.out.println("Server3............" + SERVER_PATH);
        new ServerThread(8000).start();
    }
}
