package com.genymobile.scrcpy;

import java.io.File;

public class Server2 {

    public static final String SERVER_PATH;

    static {
        String[] classPaths = System.getProperty("java.class.path").split(File.pathSeparator);
        // By convention, scrcpy is always executed with the absolute path of scrcpy-server.jar as the first item in the classpath
        SERVER_PATH = classPaths[0];
    }

    public static void main(String[] args) throws Exception {
        Ln.d("Server2............");
    }
}
