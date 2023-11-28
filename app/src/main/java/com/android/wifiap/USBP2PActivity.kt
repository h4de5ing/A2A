package com.android.wifiap

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import com.android.wifiap.databinding.ActivityP2pBinding

class USBP2PActivity : Activity() {
    private lateinit var binding: ActivityP2pBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.result.movementMethod = ScrollingMovementMethod()
        initUSB(this, connected = { connected, device ->
            binding.title.text = if (connected) "${device?.productName}[已连接]" else "未连接"
            binding.send.isEnabled = connected
        }, data = {
            if (it.isNotEmpty())
                runOnUiThread { binding.result.append("收到:${String(it)}\n") }
        })
        initAccessory(this,
            connected = { connected, accessory ->
                binding.title.text = if (connected) "${accessory?.model}[已连接]" else "未连接"
                binding.result.append(
                    "UsbAccessory[Manufacturer=${accessory?.manufacturer}," +
                            "Model=${accessory?.model}," +
                            "Description=${accessory?.description}," +
                            "Version=${accessory?.version}," +
                            "Uri=${accessory?.uri}," +
                            "SerialNumber=${accessory?.serial}]"
                )
                binding.send.isEnabled = connected
            }, data = {
                if (it.isNotEmpty())
                    runOnUiThread { binding.result.append("收到:${String(it)}\n") }
            })
        binding.send.setOnClickListener {
            val inputStr = binding.input.text.toString()
            if (!TextUtils.isEmpty(inputStr)) {
                send(inputStr.toByteArray())
                sendAccessory(inputStr.toByteArray())
                binding.input.setText("")
            }
        }
    }
}