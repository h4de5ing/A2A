package com.android.mylibrary

import org.webrtc.DataChannel

open class MyDateObserver : DataChannel.Observer {
    override fun onBufferedAmountChange(previousAmount: Long) {
//        "onBufferedAmountChange called with: previousAmount = [$previousAmount]".logE()
    }

    override fun onStateChange() {
        "onStateChange".logE()
    }

    override fun onMessage(buffer: DataChannel.Buffer?) {
        //TODO 需要封装发送文件和文本信息
        "onMessage() called with: binary = [${buffer?.binary}]".logE()
    }
}