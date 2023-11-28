package com.android.wifiap

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.text.method.ScrollingMovementMethod
import androidx.core.content.ContextCompat
import com.android.wifiap.databinding.ActivityP2pBinding
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class AccessoryActivity : Activity() {
    private lateinit var binding: ActivityP2pBinding
    private val usbAction = "com.android.usb.devices"
    private var manager: UsbManager? = null
    private var mFileDescriptor: ParcelFileDescriptor? = null
    private var mInputStream: FileInputStream? = null
    private var mOutputStream: FileOutputStream? = null
    private var mPermissionIntent: PendingIntent? = null
    private var mPermissionRequestPending = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.result.movementMethod = ScrollingMovementMethod()
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(usbAction),
                PendingIntent.FLAG_IMMUTABLE
            )
        val filter = IntentFilter(usbAction)
//        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.action?.apply {
                    when (this) {
                        usbAction -> {
                            synchronized(this) {
                                val accessory =
                                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                                if (intent.getBooleanExtra(
                                        UsbManager.EXTRA_PERMISSION_GRANTED,
                                        false
                                    )
                                ) {
                                    updateUI("usbAction")
                                    attached(accessory)
                                }
                            }
                        }

                        UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                            updateUI("ACTION_USB_ACCESSORY_ATTACHED")
                        }

                        UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                            updateUI("ACTION_USB_ACCESSORY_DETACHED")
                            val accessory =
                                intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)
                            mFileDescriptor?.close()
                            binding.title.text = "未连接2"
                        }
                    }
                }
            }
        }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val list = manager?.accessoryList
        if (list != null) {
            attached(list[0])
        } else {
            binding.title.text = "未连接1"
        }
        thread {
            val buffer = ByteArray(1024)
            while (true) {
                try {
                    val ret = mInputStream?.read(buffer) ?: 0
                    if (ret > 0) updateUI("收到:" + String(buffer))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                SystemClock.sleep(10)
            }
        }
    }

    private fun attached(accessory: UsbAccessory) {
        if (manager?.hasPermission(accessory) == true) {
            openAccessory(accessory)
        } else {
            manager?.requestPermission(accessory, mPermissionIntent)
        }
    }

    private fun openAccessory(accessory: UsbAccessory) {
        mFileDescriptor = manager?.openAccessory(accessory)
        if (mFileDescriptor != null) {
            mFileDescriptor?.fileDescriptor?.apply {
                mInputStream = FileInputStream(this)
                mOutputStream = FileOutputStream(this)
            }
            binding.title.text = "${accessory?.model}[已连接]"
        }
    }

    private fun updateUI(message: String) {
        runOnUiThread {
            binding.result.append("${message}\n")
        }
    }
}