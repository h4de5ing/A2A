package com.android.h4de5ing.screen;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.DexClassLoader;

public class Tools {

    public static void copyAssetToTmpDir(Context context, String assetFileName) {
        AssetManager assetManager = context.getAssets();
        try (InputStream is = assetManager.open(assetFileName)) {
            String targetPath = "/data/local/tmp/" + assetFileName;
            File targetFile = new File(targetPath);
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void run() {
        try {
            System.out.println("run 0");
            Process process = Runtime.getRuntime().exec("sh");
            System.out.println("run 1");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            System.out.println("run 2");
            os.writeBytes(" export CLASSPATH=/sdcard/scrcpy-server.jar exec app_process /sdcard org.las2mile.scrcpy.Server /10.16.127.95;\n");
            System.out.println("run 3");
            os.writeBytes(" exit \n");
            System.out.println("run 4");
            os.flush();
            System.out.println("执行成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void run2(Context context) {
        try {
            String dexPath = "/sdcard/scrcpy-server.jar";
            String optimizedDirectory = "/data/local/tmp";
            DexClassLoader dexClassLoader = new DexClassLoader(dexPath, optimizedDirectory, null, context.getClassLoader());
            Class<?> aClass = dexClassLoader.loadClass("org.las2mile.scrcpy.Server");
            if (aClass != null) {
                System.out.println("load class success");
                aClass.getMethod("main", String[].class)
                        .invoke(null, new Object[]{new String[]{"/10.16.127.95"}});
            } else System.err.println("load class err........");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
