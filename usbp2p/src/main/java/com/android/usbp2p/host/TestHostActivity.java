package com.android.usbp2p.host;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import java.util.HashMap;

public class TestHostActivity extends BaseChatActivity {
    @Override
    protected void sendString(String string) {
    }
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbEndpoint endpointIn = null;
    private UsbEndpoint endpointOut = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (String s : deviceList.keySet()) {
            UsbDevice device = deviceList.get(s);
            printLineToUI(device.getProductName() + " " + device.getProductId() + " " + device.getVendorId());
            if (device.getProductId() == 0x5740 && device.getVendorId() == 0x483) {
                usbDevice = device;
            }
        }
        if (usbDevice != null) {
            int interfaceCount = usbDevice.getInterfaceCount();
            printLineToUI("interfaceCount=" + interfaceCount);
            for (int i = 0; i < interfaceCount; i++) {
                printLineToUI("index=" + i + " " + usbDevice.getInterface(i).toString());
            }
//            UsbInterface usbInterface = usbDevice.getInterface(0);
//            for (int i = 0; i < usbDevice.getInterface(0).getEndpointCount(); i++) {
//                UsbEndpoint endpoint = usbDevice.getInterface(0).getEndpoint(i);
//                if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
//                    endpointIn = endpoint;
//                }
//                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
//                    endpointOut = endpoint;
//                }
//            }
//            UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
//            boolean claimResult = connection.claimInterface(usbInterface, true);
//            printLineToUI("claimResult  " + claimResult);
//            if (!claimResult) {
//                printLineToUI("Could not claim device");
//            } else {
//                new Thread(() -> {
//                    byte[] buff = new byte[Constants.BUFFER_SIZE_IN_BYTES];
//                    printLineToUI("Claimed interface - ready to communicate 有从设备连接到本机");
//                    while (true) {
//                        int bytesTransferred = connection.bulkTransfer(endpointIn, buff, buff.length, Constants.USB_TIMEOUT_IN_MS);
//                        if (bytesTransferred > 0) {
//                            printLineToUI("接收:" + new String(buff, 0, bytesTransferred));
//                        }
//                    }
//                }).start();
//            }
        }
    }
}
