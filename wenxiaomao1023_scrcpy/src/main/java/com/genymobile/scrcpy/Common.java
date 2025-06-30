package com.genymobile.scrcpy;

import android.os.Handler;
import android.os.Message;

public class Common {

    public static void stopScrcpy(Handler handler, String obj) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = obj;
        try {
            handler.sendMessage(msg);
        } catch (IllegalStateException e) {
            e.fillInStackTrace();
        }
    }
}