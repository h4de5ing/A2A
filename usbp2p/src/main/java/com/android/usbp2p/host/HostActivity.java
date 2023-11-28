package com.android.usbp2p.host;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class HostActivity extends BaseChatActivity {

    private final AtomicBoolean keepThreadAlive = new AtomicBoolean(true);
    private final List<String> sendBuffer = new ArrayList<>();

    @Override
    protected void sendString(String string) {
        sendBuffer.add(string);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(new CommunicationRunnable()).start();
    }

    private class CommunicationRunnable implements Runnable {

        @Override
        public void run() {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbDevice device = getIntent().getParcelableExtra(ConnectActivity.DEVICE_EXTRA_KEY);
            UsbEndpoint endpointIn = null;
            UsbEndpoint endpointOut = null;
            UsbInterface usbInterface = device.getInterface(0);
            for (int i = 0; i < device.getInterface(0).getEndpointCount(); i++) {
                UsbEndpoint endpoint = device.getInterface(0).getEndpoint(i);
                if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    endpointIn = endpoint;
                }
                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    endpointOut = endpoint;
                }
            }

            if (endpointIn == null) {
                printLineToUI("Input Endpoint not found");
                return;
            }

            if (endpointOut == null) {
                printLineToUI("Output Endpoint not found");
                return;
            }

            UsbDeviceConnection connection = usbManager.openDevice(device);

            if (connection == null) {
                printLineToUI("Could not open device");
                return;
            }

            boolean claimResult = connection.claimInterface(usbInterface, true);

            if (!claimResult) {
                printLineToUI("Could not claim device");
            } else {
                byte[] buff = new byte[Constants.BUFFER_SIZE_IN_BYTES];
                printLineToUI("Claimed interface - ready to communicate 有从设备连接到本机");

                while (keepThreadAlive.get()) {
                    int bytesTransferred = connection.bulkTransfer(endpointIn, buff, buff.length, Constants.USB_TIMEOUT_IN_MS);
                    if (bytesTransferred > 0) {
                        printLineToUI("device> " + new String(buff, 0, bytesTransferred));
                    }

                    synchronized (sendBuffer) {
                        if (sendBuffer.size() > 0) {
                            byte[] sendBuff = sendBuffer.get(0).toString().getBytes();
                            connection.bulkTransfer(endpointOut, sendBuff, sendBuff.length, Constants.USB_TIMEOUT_IN_MS);
                            sendBuffer.remove(0);
                        }
                    }
                }
            }
            connection.releaseInterface(usbInterface);
            connection.close();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        keepThreadAlive.set(false);
    }
}
