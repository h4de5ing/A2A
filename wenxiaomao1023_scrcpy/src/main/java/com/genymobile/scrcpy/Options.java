package com.genymobile.scrcpy;

import android.graphics.Rect;

public class Options {
    private int maxSize = 0;//最大尺寸 默认0   =输入1024 &~7 :multiple of 8
    private int bitRate = 1000000;//码率 100000~8000000 默认1000000
    private int maxFps = 30;//帧率 1~60 默认30
    private Rect crop = null;
    //是否发送帧原始信息，默认true
    private boolean sendFrameMeta = true; // send PTS so that the client may record properly

    private int quality = 60;//质量 1~100 默认60
    private int scale = 480;//缩放 1080 720 480 360 默认480

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getMaxFps() {
        return maxFps;
    }

    public void setMaxFps(int maxFps) {
        this.maxFps = maxFps;
    }

    public Rect getCrop() {
        return crop;
    }

    public void setCrop(Rect crop) {
        this.crop = crop;
    }

    public boolean getSendFrameMeta() {
        return sendFrameMeta;
    }

    public void setSendFrameMeta(boolean sendFrameMeta) {
        this.sendFrameMeta = sendFrameMeta;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    @Override
    public String toString() {
        return "Options{" +
                "maxSize=" + maxSize +
                ", bitRate=" + bitRate +
                ", maxFps=" + maxFps +
                ", crop=" + crop +
                ", sendFrameMeta=" + sendFrameMeta +
                ", quality=" + quality +
                ", scale=" + scale +
                '}';
    }
}
