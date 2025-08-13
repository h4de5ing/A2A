package com.android.h4de5ing.cclient

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.os.ServiceManager
import android.view.IWindowManager
import android.view.Surface


private var sDisplaySize = Point()
fun getDisplaySize(): Point = Point(sDisplaySize.x, sDisplaySize.y)

fun init() {
    val iwm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"))
    iwm.getInitialDisplaySize(0, sDisplaySize)
}

fun screenShot(context: Context, widht: Int = 1080, height: Int = 2400, surface: Surface) {
//    val display: IBinder = SurfaceControl.createDisplay("scrcpy", false)
    val mp = context.getSystemService("media_projection") as MediaProjectionManager
    val mediaProjection = mp.getMediaProjection(0, Intent())
    val virtualDisplay = mediaProjection?.createVirtualDisplay(
        "scrcpy",
        widht,
        height,
        1,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
        surface,
        null,
        null
    )
}