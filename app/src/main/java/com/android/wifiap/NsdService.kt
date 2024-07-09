package com.android.wifiap

import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder

/**
 * Nsd主要用于局域网设备发现
 */
class NsdService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        println("NsdService onCreate")
        val nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        nsdManager.discoverServices(
            "_http._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onDiscoveryStarted(serviceType: String?) {
                    TODO("Not yet implemented")
                }

                override fun onDiscoveryStopped(serviceType: String?) {
                    TODO("Not yet implemented")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    TODO("Not yet implemented")
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                    TODO("Not yet implemented")
                }
            })

        var serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = "h4de5ing's android"
        serviceInfo.serviceType = "_http._tcp."
        serviceInfo.port = 1234
        serviceInfo.setAttribute("hostname", "192.168.0.1")
        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    TODO("Not yet implemented")
                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                    TODO("Not yet implemented")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    TODO("Not yet implemented")
                }
            })
    }
}