package com.android.h4de5ing.screen;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        final StringBuilder command = new StringBuilder();
        command.append(" CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / org.las2mile.scrcpy.Server2 ");
    }
}
