package com.genymobile.scrcpy;

import java.nio.ByteBuffer;

public class JpegEncoder {

    static {
        try {
            System.loadLibrary("compress");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static native byte[] compress(ByteBuffer buffer, int width, int pitch, int height, int quality);

    public static native void test();
}
