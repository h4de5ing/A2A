package com.android.android

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.android.android.databinding.ActivityMainBinding
import com.android.mylibrary.SignalingWS
import com.android.mylibrary.logI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.las2mile.scrcpy.DataChangeListener
import org.las2mile.scrcpy.Device
import org.las2mile.scrcpy.Options
import org.las2mile.scrcpy.ScreenEncoder2
import kotlin.random.Random


class RecordActivity : Activity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnStart.visibility = View.VISIBLE
        binding.btnStart.setOnClickListener {
            //开始录屏并将视频回调通过websocket发送到服务器端
            Thread {
                ScreenEncoder2(Options.bitRate).streamScreen(Device(), object : DataChangeListener {
                    override fun onDataChanged(data: ByteArray) {
                        count++
                        client.send(data)
//                        client.sendByteArray(srcSn, target, data)
                    }
                })
            }.start()
        }
        MainScope().launch(Dispatchers.IO) {
            while (true) {
                delay(1000)
                runOnUiThread {
                    binding.tv.text = "${Consts.srcSn}, ${Consts.wsStatus},\n视频帧数:${count}"
                }
            }
        }
        initData()
    }

    private var count = 0L
    private var srcSn: String = ""
    private var target: String = "123"
    private lateinit var client: SignalingWS
    private fun initData() {
        Consts.srcSn = "webrtc:${Random.nextInt(10, 100)}"
        srcSn = Consts.srcSn
        client = SignalingWS.getInstance(srcSn, onMessageCallback = {
            println("收到消息：$it")
        })
        client.init {
            Consts.wsStatus = it
            "$srcSn $it".logI()
        }
    }
}