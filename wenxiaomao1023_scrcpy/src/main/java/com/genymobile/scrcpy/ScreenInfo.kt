package com.genymobile.scrcpy

import android.graphics.Rect

class ScreenInfo(
    val contentRect: Rect,// device size, possibly cropped
    val videoSize: Size,
    val rotated: Boolean
) {
    fun withRotation(rotation: Int): ScreenInfo {
        val newRotated = (rotation and 1) != 0
        if (rotated == newRotated) return this
        return ScreenInfo(Device.flipRect(contentRect), videoSize.rotate(), newRotated)
    }
}