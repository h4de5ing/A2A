package com.android.wifiap

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread


private const val ACTION_USB_PERMISSION = "com.examples.accessory.controller.action.USB_PERMISSION"
private var mContext: Context? = null
private var manager: UsbManager? = null
private var mAccessory: UsbAccessory? = null
private var mFileDescriptor: ParcelFileDescriptor? = null
private var mInputStream: FileInputStream? = null
private var mOutputStream: FileOutputStream? = null
private var mPermissionRequestPending = false
fun initAccessory(
    context: Context,
    connected: ((Boolean, UsbAccessory?) -> Unit) = { _, _ -> },
    data: ((ByteArray) -> Unit)
) {
    mContext = context
    manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val mPermissionIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
    val filter = IntentFilter(ACTION_USB_PERMISSION)
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
    ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.apply {
                when (this) {
                    ACTION_USB_PERMISSION -> {
                        synchronized(this) {
                            val accessory =
                                intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                            if (intent.getBooleanExtra(
                                    UsbManager.EXTRA_PERMISSION_GRANTED,
                                    false
                                )
                            ) {
                                openAccessory(accessory)
                            }
                            mPermissionRequestPending = false
                        }
                        connected(true, mAccessory)
                    }

                    UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {

                    }

                    UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                        val accessory =
                            intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                        if (accessory != null && accessory == mAccessory) {
                            detached()
                            connected(false, mAccessory)
                        }
                    }
                }
            }
        }
    }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    thread {
        val buffer = ByteArray(1024)
        while (true) {
            try {
                val ret = mInputStream?.read(buffer) ?: 0
                if (ret > 0) data(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            SystemClock.sleep(10)
        }
    }
    val accessories: Array<UsbAccessory>? = manager?.accessoryList
    val accessory = if (accessories == null) null else accessories[0]
    if (accessory != null) {
        if (manager?.hasPermission(accessory) == true) {
            openAccessory(accessory)
            connected(mAccessory != null, mAccessory)
        } else {
            if (!mPermissionRequestPending) {
                manager?.requestPermission(accessory, mPermissionIntent)
                mPermissionRequestPending = true
            }
        }
    }
}
private fun attached(attached: ((Boolean, UsbAccessory?) -> Unit)){

}
private fun openAccessory(accessory: UsbAccessory) {
    mFileDescriptor = manager?.openAccessory(accessory)
    if (mFileDescriptor != null) {
        mAccessory = accessory
        mFileDescriptor?.fileDescriptor?.apply {
            mInputStream = FileInputStream(this)
            mOutputStream = FileOutputStream(this)
        }
    }
}

private fun detached() {
    try {
        mFileDescriptor?.close()
    } catch (ignored: Exception) {
    } finally {
        mFileDescriptor = null
        mAccessory = null
    }
}

fun sendAccessory(data: ByteArray) {
    try {
        mOutputStream?.write(data)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}