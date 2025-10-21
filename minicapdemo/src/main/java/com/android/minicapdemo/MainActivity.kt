package com.android.minicapdemo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.devicefarmer.minicap.SimpleServer
import io.devicefarmer.minicap.provider.SurfaceProvider
import io.devicefarmer.minicap.utils.Ln
import java.net.ServerSocket

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
        run()
    }

    private var isRunning = false
    fun run() {
        Thread {
            val serverSocket = ServerSocket(1717)
            isRunning = true
            while (isRunning) {
                try {
                    val socket = serverSocket.accept()
                    Ln.d(">>>>>>Client connect success<<<<<< ${socket.localAddress}:${socket.localPort}")
                    //1080x672 DisplayWidth=1200, DisplayHeight=1920
                    val provider = SurfaceProvider(0)
                    provider.quality = 100
                    provider.frameRate = Float.MAX_VALUE
                    SimpleServer(socket, provider).start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }
}