package com.android.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import com.android.android.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.tv.movementMethod = ScrollingMovementMethod()
        startService(Intent(this, ForegroundService::class.java))
        MainScope().launch(Dispatchers.IO) {
            while (true) {
                delay(1000)
                runOnUiThread {
                    binding.tv.text =
                        "${Consts.srcSn}, ${Consts.wsStatus},${com.android.mylibrary.Consts.frameCountFlow.value}"
                }
            }
        }
    }
}