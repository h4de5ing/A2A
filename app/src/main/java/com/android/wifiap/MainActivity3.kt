package com.android.wifiap

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import com.android.wifiap.databinding.ActivityP2pBinding

class MainActivity3 : AppCompatActivity() {
    private lateinit var binding: ActivityP2pBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityP2pBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.result.movementMethod = ScrollingMovementMethod()

    }
}