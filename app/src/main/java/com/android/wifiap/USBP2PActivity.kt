package com.android.wifiap

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.android.wifiap.databinding.ActivityP2pBinding

class USBP2PActivity : Activity() {
    private lateinit var binding: ActivityP2pBinding
    private lateinit var manager: UsbManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        initUSB(this, connected = { connected, device ->
//            binding.title.text = if (connected) "${device?.productName}[已连接]" else "未连接"
//            binding.send.isEnabled = connected
//        }, data = {
//            runOnUiThread { binding.result.append("${String(it)}\n") }
//        })
//        binding.send.setOnClickListener {
//            val inputStr = binding.input.text.toString()
//            if (!TextUtils.isEmpty(inputStr)) {
//                send(inputStr.toByteArray())
//            }
//        }
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action != null) {
                    when (action) {
                        UsbManager.ACTION_USB_DEVICE_ATTACHED -> updateUSB()
                        UsbManager.ACTION_USB_DEVICE_DETACHED -> updateUSB()
                    }
                }
            }
        }, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        manager = getSystemService(Context.USB_SERVICE) as UsbManager
        updateUSB()
    }

    fun updateUSB() {
        manager.deviceList?.forEach {
            binding.result.append("Device:${it.value.productName}\n")
        }
        manager.accessoryList?.forEach {
            binding.result.append("UsbAccessory:${it.version}\n")
        }
    }
}