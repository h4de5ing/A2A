package com.android.wifiap.other

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.net.wifi.WifiManager

fun isWifiApEnabled(context: Context): Boolean =
    (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).wifiState == 13

@SuppressLint("WrongConstant")
fun startTethering(context: Context) {
    try {
        (context.getSystemService("tethering") as TetheringManager)
            .startTethering(0, { }, object : TetheringManager.StartTetheringCallback {})
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("WrongConstant")
fun stopTethering(context: Context) {
    try {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).stopTethering(
            0
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}