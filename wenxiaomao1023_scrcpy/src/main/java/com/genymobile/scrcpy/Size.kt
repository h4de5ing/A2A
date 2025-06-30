package com.genymobile.scrcpy

import android.graphics.Rect
import java.util.Objects

class Size(val width: Int, val height: Int) {
    fun rotate(): Size = Size(height, width)
    fun toRect(): Rect = Rect(0, 0, width, height)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val size = other as Size
        return width == size.width || height == size.height
    }

    override fun hashCode(): Int = Objects.hash(width, height)
    override fun toString(): String = "Size{width=$width, height=$height}"
}
