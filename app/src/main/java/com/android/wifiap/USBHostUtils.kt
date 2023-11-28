package com.android.wifiap

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import kotlin.concurrent.thread

private const val usbAction = "com.android.usb.host"
private var mContext: Context? = null
private var manager: UsbManager? = null
private var mDevice: UsbDevice? = null
private var mUsbEndpointIn: UsbEndpoint? = null
private var mUsbEndpointOut: UsbEndpoint? = null
private var mUsbDeviceConnection: UsbDeviceConnection? = null
fun initUSB(
    context: Context,
    connected: ((Boolean, UsbDevice?) -> Unit) = { _, _ -> },
    data: ((ByteArray) -> Unit)
) {
    mContext = context
    manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val filter = IntentFilter()
    filter.addAction(usbAction)
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
    ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.apply {
                when (this) {
                    usbAction -> connect()
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> attached(attached = connected)
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        connected(false, mDevice)
                        detached()
                    }
                }
            }
        }
    }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    attached(attached = connected)
    thread {
        while (true) {
            readData(data)
            SystemClock.sleep(10)
        }
    }
}

fun send(data: ByteArray) {
    try {
        mUsbDeviceConnection?.apply {
            println("USB 写入:${String(data)}")
            bulkTransfer(mUsbEndpointOut, data, data.size, 1000)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun readData(onChange: ((ByteArray) -> Unit)) {
    try {
        mUsbEndpointIn?.apply {
            val inMax = maxPacketSize
            val byteBuffer = ByteBuffer.allocate(inMax)
            val usbRequest = UsbRequest()
            usbRequest.initialize(mUsbDeviceConnection, this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) usbRequest.queue(byteBuffer)
            mUsbDeviceConnection?.apply {
                if (requestWait() === usbRequest) {
                    println("USB 数据:${byteBuffer.array().toHexString()}")
                    onChange(byteBuffer.array())
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun attached(attached: ((Boolean, UsbDevice?) -> Unit)) {
    manager?.deviceList?.values?.apply {
        for (device in this) {
            mDevice = device
            if (isUsbAccessory(device)) initAccessory(device)
        }
    }
    mDevice?.apply {
        if (manager != null) {
            if (manager!!.hasPermission(this)) {
                connect()
            } else {
                val pendingIntent = PendingIntent.getBroadcast(
                    mContext, 0, Intent(usbAction), PendingIntent.FLAG_IMMUTABLE
                )
                manager?.requestPermission(this, pendingIntent)
            }
        }
    }
    attached(mDevice != null, mDevice)
}

private fun isUsbAccessory(device: UsbDevice): Boolean =
    device.productId == 0x2d00 || device.productId == 0x2d01

private fun initAccessory(device: UsbDevice) {
    try {
        manager?.openDevice(device)?.apply {
            initStringControlTransfer(this, 0, "h4de5ing") // MANUFACTURER
            initStringControlTransfer(this, 1, "Android2AndroidAccessory") // MODEL
            initStringControlTransfer(this, 2, "android2android USB communication") // DESCRIPTION
            initStringControlTransfer(this, 3, "1.0") // VERSION
            initStringControlTransfer(this, 4, "https://github.com/h4de5ing") // URI
            initStringControlTransfer(this, 5, "42") // SERIAL
            controlTransfer(0x40, 53, 0, 0, byteArrayOf(), 0, 1000)
            close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun initStringControlTransfer(
    deviceConnection: UsbDeviceConnection,
    index: Int,
    string: String
) {
    try {
        deviceConnection.controlTransfer(
            0x40,
            52,
            0,
            index,
            string.toByteArray(),
            string.length,
            1000
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun detached() {
    mDevice = null
    mUsbEndpointIn = null
    mUsbEndpointOut = null
    mUsbDeviceConnection = null
}

private fun connect() {
    mDevice?.apply {
        val anInterface = getInterface(0)
        mUsbDeviceConnection = manager!!.openDevice(this)
        if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection?.claimInterface(anInterface, true)
            val endpointCount = anInterface.endpointCount
            for (i in 0 until endpointCount) {
                val endpoint = anInterface.getEndpoint(i)
                if (UsbConstants.USB_DIR_IN == endpoint.direction) {
                    mUsbEndpointIn = endpoint
                } else if (UsbConstants.USB_DIR_OUT == endpoint.direction) {
                    mUsbEndpointOut = endpoint
                }
            }
        }
    }
}


fun ByteArray.toHexString(): String = this.toHexString(this.size)
fun ByteArray.toHexString(length: Int): String {
    val sb = StringBuilder()
    val hex =
        charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    for (i in 0 until length) {
        val value: Int = this[i].toInt().and(0xff)
        sb.append(hex[value / 16]).append(hex[value % 16]).append(" ")
    }
    return sb.toString()
}
