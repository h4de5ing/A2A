package com.android.wifiap.other

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.TetheringManager

@SuppressLint("WrongConstant")
fun startTethering(context: Context) {
    try {
        val tether = context.getSystemService("tethering") as TetheringManager
        tether.startTethering(0, { }, object : TetheringManager.StartTetheringCallback {
            override fun onTetheringStarted() {
                super.onTetheringStarted()
                println("ap打开成功")
            }

            override fun onTetheringFailed(error: Int) {
                super.onTetheringFailed(error)
                println("ap打开失败:${error}")
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("WrongConstant")
fun stopTethering(context: Context) {
    try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.javaClass.getDeclaredMethod("stopTethering", Int::class.javaPrimitiveType).invoke(cm, 0)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}