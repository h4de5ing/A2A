package com.android.usbp2p.host;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.usbp2p.R;

import java.util.HashMap;


public class ConnectActivity extends AppCompatActivity {
    private UsbManager mUsbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (searchForUsbAccessory(deviceList)) return;
        for (UsbDevice device : deviceList.values()) {
            initAccessory(device);
        }
        finish();
    }

    private boolean searchForUsbAccessory(final HashMap<String, UsbDevice> deviceList) {
        for (UsbDevice device : deviceList.values()) {
            if (isUsbAccessory(device)) {
                Intent intent = new Intent(this, HostActivity.class);
                intent.putExtra(Constants.DEVICE_EXTRA_KEY, device);
                startActivity(intent);
                finish();
                return true;
            }
        }

        return false;
    }

    private boolean isUsbAccessory(UsbDevice device) {
        return (device.getProductId() == 0x2d00) || (device.getProductId() == 0x2d01);
    }

    private boolean initAccessory(UsbDevice device) {
        UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if (connection == null) return false;
        initStringControlTransfer(connection, 0, "H4de5ing"); // MANUFACTURER
        initStringControlTransfer(connection, 1, "Android2AndroidAccessory"); // MODEL
        initStringControlTransfer(connection, 2, "showcasing android2android USB communication"); // DESCRIPTION
        initStringControlTransfer(connection, 3, "0.1"); // VERSION
        initStringControlTransfer(connection, 4, "https://github.com/h4de5ing"); // URI
        initStringControlTransfer(connection, 5, "42"); // SERIAL
        connection.controlTransfer(0x40, 53, 0, 0, new byte[]{}, 0, Constants.USB_TIMEOUT_IN_MS);
        connection.close();
        return true;
    }

    private void initStringControlTransfer(UsbDeviceConnection deviceConnection, int index, String string) {
        deviceConnection.controlTransfer(0x40, 52, 0, index, string.getBytes(), string.length(), Constants.USB_TIMEOUT_IN_MS);
    }
}
