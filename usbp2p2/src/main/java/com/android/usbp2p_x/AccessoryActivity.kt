package com.android.usbp2p_x

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.usbp2p_x.databinding.ActivitySendBinding
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class AccessoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySendBinding
    private val ACTION_USB_PERMISSION = "com.examples.accessory.controller.action.USB_PERMISSION"
    private var mUsbManager: UsbManager? = null
    private var mPermissionIntent: PendingIntent? = null
    private var mPermissionRequestPending = false
    var mAccessory: UsbAccessory? = null
    var mFileDescriptor: ParcelFileDescriptor? = null
    var mInputStream: FileInputStream? = null
    var mOutputStream: FileOutputStream? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.result.movementMethod = ScrollingMovementMethod()
        mUsbManager = getSystemService(USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        ContextCompat.registerReceiver(
            this,
            mUsbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        mUsbManager?.apply {
            val accessories = accessoryList
            val accessory = accessories?.get(0)
            if (accessory != null) {
                if (hasPermission(accessory)) {
                    openAccessory(accessory)
                } else {
                    synchronized(mUsbReceiver) {
                        if (!mPermissionRequestPending) {
                            requestPermission(accessory, mPermissionIntent)
                            mPermissionRequestPending = true
                        }
                    }
                }
            } else {
                updateUI("没找到UsbAccessory设备")
            }
        }
        thread {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val ret = mInputStream!!.read(buffer)
                    if (ret > 0) updateUI("接收: " + String(buffer))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openAccessory(accessory: UsbAccessory) {
        mFileDescriptor = mUsbManager!!.openAccessory(accessory)
        if (mFileDescriptor != null) {
            updateUI(accessory.toString() + " " + accessory.serial)
            mAccessory = accessory
            val fd = mFileDescriptor!!.fileDescriptor
            mInputStream = FileInputStream(fd)
            mOutputStream = FileOutputStream(fd)
        }
    }

    private fun closeAccessory() {
        try {
            mFileDescriptor?.close()
        } catch (ignored: Exception) {
        } finally {
            mFileDescriptor = null
            mAccessory = null
        }
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val accessory =
                        intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory)
                    } else {
                        updateUI("permission denied for accessory $accessory")
                    }
                    mPermissionRequestPending = false
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED == action) {
                val accessory = intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                if (accessory != null && accessory == mAccessory) {
                    closeAccessory()
                }
            }
        }
    }

    fun updateUI(message: String) {
        runOnUiThread {
            binding.result.append("${message}\n")
        }
    }
}