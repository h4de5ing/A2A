package com.android.h4de5ing.cclient

import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button


class MainActivity : Activity() {
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        println("获取到屏幕信息:${getDisplaySize()}")
        mMediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            val captureIntent = mMediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, 90)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (data == null) return
    }
}