package com.android.h4de5ing.screen

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.Socket


class MainActivity : AppCompatActivity() {
    fun getMyIp(): String {
        var ip = "127.0.0.1"
        try {
            for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    networkInterface.interfaceAddresses.forEach {
                        when (it.address) {
                            is Inet4Address -> {
                                it.address.hostAddress?.apply { ip = this }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ip
    }

    private var scope = MainScope()
    private lateinit var tv: TextView
    private var serverThread: ServerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tv = findViewById(R.id.tv)
        findViewById<TextView>(R.id.ip).text = getMyIp()
        serverThread = ServerThread(8000)
        serverThread?.start()
//        Thread {
//            connectToServer("10.16.126.38", 7007)
//        }.start()
//        ClientThread().start()
    }

    private var socket: Socket? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null
    private fun connectToServer(ip: String, port: Int) {
        try {
            socket = Socket(ip, port)
            Log.i("Client", "Connected to server")
            if (socket != null) {
                socket?.apply {
                    dataOutputStream = DataOutputStream(getOutputStream())
                    dataInputStream = DataInputStream(getInputStream())
                    Thread {
                        try {
                            while (true) {
                                val length = dataInputStream?.available() ?: 0
                                if (length > 0) {
                                    val packetSize = ByteArray(4)
                                    dataInputStream?.readFully(packetSize, 0, 4)
                                    println("读取到视频数据长度:${BigInteger(packetSize).toInt()}")
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun send(message: String) {
        try {
            scope.launch(Dispatchers.IO) {
                dataOutputStream?.write(message.toByteArray())
                dataOutputStream?.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
        serverThread?.stopServer()
    }
}