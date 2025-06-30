package com.android.scrcpy.v2

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.genymobile.scrcpy.Server
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val ip = findViewById<TextView>(R.id.sample_text)
        ip.text = Tools.getMyIp()
        findViewById<Button>(R.id.start_button).setOnClickListener {
            startServer()
        }
        start()
        startServer()
    }

    private fun start() {
        MyWSClient("ws://10.18.16.212:8080/emm_mdm/websocket/123456") {
            it.logI()
        }.connectWebSocket()
    }

    private fun startServer() {
        Thread {
            val localServerSocket = ServerSocketChannel.open()
            localServerSocket.bind(InetSocketAddress(1717))

            val controlServerSocket = ServerSocketChannel.open()
            controlServerSocket.bind(InetSocketAddress(6612))
            while (true) {
                var videoSocketChannel: SocketChannel?
                var controlSocketChannel: SocketChannel?
                try {
                    videoSocketChannel = localServerSocket.accept()
                    println("videoSocketChannel 有设备连接,${videoSocketChannel.socket().inetAddress}")
                    controlSocketChannel = controlServerSocket.accept()
                    if (videoSocketChannel != null && controlSocketChannel != null) {
                        val options = Server.customOptions()
                        println(options.toString())
                        Server.scrcpy(options, videoSocketChannel, controlSocketChannel)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}