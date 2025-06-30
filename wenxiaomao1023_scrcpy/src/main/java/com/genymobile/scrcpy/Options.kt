package com.genymobile.scrcpy

import android.graphics.Rect

data class Options(
    var maxSize: Int = 0, //最大尺寸 默认0   =输入1024 &~7 :multiple of 8
    var bitRate: Int = 1000000, //码率 100000~8000000 默认1000000
    var maxFps: Int = 30,//帧率 1~60 默认30
    var crop: Rect? = null,

    //是否发送帧原始信息，默认true
    var sendFrameMeta: Boolean = true, // send PTS so that the client may record properly

    var quality: Int = 60, //质量 1~100 默认60
    var scale: Int = 480 //缩放 1080 720 480 360 默认480
) {
    override fun toString(): String {
        return "Options{" +
                "maxSize=" + maxSize +
                ", bitRate=" + bitRate +
                ", maxFps=" + maxFps +
                ", crop=" + crop +
                ", sendFrameMeta=" + sendFrameMeta +
                ", quality=" + quality +
                ", scale=" + scale +
                '}'
    }
}
