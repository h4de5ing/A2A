package com.android.h4de5ing.screen

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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
        val input = findViewById<EditText>(R.id.input)
        findViewById<Button>(R.id.button).setOnClickListener {
        }
        serverThread = ServerThread(8000)
        serverThread?.start()
        Thread {
            connectToServer("10.16.127.95", 8000)
        }.start()
    }

    private var socket: Socket? = null
    private var bufferedWriter: BufferedWriter? = null
    private fun connectToServer(ip: String, port: Int) {
        try {
            socket = Socket(ip, port)
            Log.i("Client", "Connected to server")
            if (socket != null) {
                bufferedWriter = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                Thread {
                    try {
                        var inputLine: String?
                        while (true) {
                            inputLine = reader.readLine()
                            if (inputLine != null) {
                                runOnUiThread { tv.append("收到结果:${inputLine}\n") }
                                Log.i("Client", "收到结果:$inputLine")
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Client", "Error connecting to server", e)
        }
    }

    private fun send(message: String) {
        try {
            scope.launch(Dispatchers.IO) {
                bufferedWriter?.write(message)
                bufferedWriter?.newLine()
                bufferedWriter?.flush()
                Log.i("Client", "Sent message: $message")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Client", "Error sending message", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
        serverThread?.stopServer()
    }
}