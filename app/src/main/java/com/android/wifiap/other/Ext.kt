package com.android.wifiap.other

import android.annotation.SuppressLint
import android.content.Context
import android.net.TetheringManager
import android.net.wifi.WifiManager

@SuppressLint("WrongConstant")
fun setAP(context: Context) {
    try {
        val wifiManger = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val tether = context.getSystemService("tether") as TetheringManager
//        tether.startTethering()

    } catch (e: Exception) {
        e.printStackTrace()
    }
}