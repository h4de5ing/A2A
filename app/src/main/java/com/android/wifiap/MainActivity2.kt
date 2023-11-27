package com.android.wifiap

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import com.android.wifiap.databinding.ActivityMainBinding
import com.android.wifiap.other.SoftApCallback.registerSoftApCallback
import com.android.wifiap.other.startTethering
import com.android.wifiap.other.stopTethering

class MainActivity2 : Activity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        binding.open.setOnClickListener {
            startTethering(this)
        }
        binding.close.setOnClickListener {
            stopTethering(this)
        }
        registerSoftApCallback(this)
    }
}