package com.android.android

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class ProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.proxy)
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), requestCodeCodeMedia
        )
    }

    private val requestCodeCodeMedia = 102
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeCodeMedia) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    "onActivityResult: RESULT_OK".logI()
                    Consts.screenCaptureIntent = data
                    startService(Intent(this, AppService::class.java))
                    startService(Intent(this, ForegroundService::class.java).setAction("recording"))
                    finish()
                }
            }
        }
    }
}