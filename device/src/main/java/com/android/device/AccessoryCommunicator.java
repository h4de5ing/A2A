package com.android.device;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;


import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public abstract class AccessoryCommunicator {
    private UsbManager usbManager;
    private Context context;
    private Handler sendHandler;
    private ParcelFileDescriptor fileDescriptor;
    private FileInputStream inStream;
    private FileOutputStream outStream;
    private boolean running;

    public AccessoryCommunicator(final Context context) {
        this.context = context;
        usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        final UsbAccessory[] accessoryList = usbManager.getAccessoryList();
        if (accessoryList == null || accessoryList.length == 0) {
            onError("no accessory found");
        } else {
            openAccessory(accessoryList[0]);
        }
    }

    public void send(byte[] payload) {
        if (sendHandler != null) {
            Message msg = sendHandler.obtainMessage();
            msg.obj = payload;
            sendHandler.sendMessage(msg);
        }
    }

    private void receive(final byte[] payload, final int length) {
        onReceive(payload, length);
    }

    public abstract void onReceive(final byte[] payload, final int length);

    public abstract void onError(String msg);

    public abstract void onConnected();

    public abstract void onDisconnected();


    private class CommunicationThread extends Thread {
        @Override
        public void run() {
            running = true;
            while (running) {
                byte[] msg = new byte[Constants.BUFFER_SIZE_IN_BYTES];
                try {
                    //Handle incoming messages
                    int len = inStream.read(msg);
                    while (inStream != null && len > 0 && running) {
                        receive(msg, len);
                        Thread.sleep(10);
                        len = inStream.read(msg);
                    }
                } catch (final Exception e) {
                    onError("USB Receive Failed " + e + "\n");
                    closeAccessory();
                }
            }
        }
    }

    private void openAccessory(UsbAccessory accessory) {
        fileDescriptor = usbManager.openAccessory(accessory);
        if (fileDescriptor != null) {
            FileDescriptor fd = fileDescriptor.getFileDescriptor();
            inStream = new FileInputStream(fd);
            outStream = new FileOutputStream(fd);
            new CommunicationThread().start();
            sendHandler = new Handler(Looper.getMainLooper()) {
                public void handleMessage(Message msg) {
                    try {
                        outStream.write((byte[]) msg.obj);
                    } catch ( Exception e) {
                        onError("USB Send Failed " + e + "\n");
                    }
                }
            };

            onConnected();
        } else {
            onError("could not connect");
        }
    }

    public void closeAccessory() {
        running = false;

        try {
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
        } catch (Exception ignored) {
        } finally {
            fileDescriptor = null;
        }

        onDisconnected();
    }

}