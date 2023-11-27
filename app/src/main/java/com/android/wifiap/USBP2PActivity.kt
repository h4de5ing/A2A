package com.android.wifiap

import android.app.Activity
import android.os.Bundle
import android.text.TextUtils
import com.android.wifiap.databinding.ActivityP2pBinding

class USBP2PActivity : Activity() {
    private lateinit var binding: ActivityP2pBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUSB(this, connected = { connected, device ->
            binding.title.text = if (connected) "${device?.productName}[已连接]" else "未连接"
            binding.send.isEnabled = connected
        }, data = {
            runOnUiThread { binding.result.append("${String(it)}\n") }
        })
        binding.send.setOnClickListener {
            val inputStr = binding.input.text.toString()
            if (!TextUtils.isEmpty(inputStr)) {
                send(inputStr.toByteArray())
            }
        }
    }
}