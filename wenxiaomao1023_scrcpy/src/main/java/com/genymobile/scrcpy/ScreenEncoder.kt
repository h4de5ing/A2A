package com.genymobile.scrcpy

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Process
import android.view.Surface
import androidx.core.graphics.createBitmap
import com.genymobile.scrcpy.Device.RotationListener
import com.genymobile.scrcpy.wrappers.SurfaceControl
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class ScreenEncoder(options: Options, rotation: Int, val change: (ByteArray) -> Unit = {}) :
    RotationListener {
    private val rotationChanged = AtomicBoolean()
    private val mRotation = AtomicInteger(0)
    private val maxFps: Int = options.maxFps
    private val quality: Int = options.quality
    private val scale: Int = options.scale
    private val handler: Handler
    private val mHandlerThread: HandlerThread?
    private var imageAvailableListenerImpl: ImageReader.OnImageAvailableListener? = null

    private val rotationLock = Any()
    private val imageReaderLock = Any()
    private var bImageReaderDisable = true //Segmentation fault

    @get:Synchronized
    @set:Synchronized
    private var alive = true

    init {
        mRotation.set(rotation)
        mHandlerThread = HandlerThread("ScrcpyImageReaderHandlerThread")
        mHandlerThread.start()
        handler = object : Handler(mHandlerThread.looper) {
            override fun handleMessage(msg: Message) {
                Ln.i("handler message: $msg")
                if (msg.what == 1) { //exit
                    alive = false
                    synchronized(rotationLock) {
                        (rotationLock as Object).notify()
                    }
                }
            }
        }
    }

    override fun onRotationChanged(rotation: Int) {
        Ln.i("rotation: $rotation")
        mRotation.set(rotation)
        rotationChanged.set(true)
        synchronized(rotationLock) {
            (rotationLock as Object).notify()
        }
    }

    private inner class ImageAvailableListenerImpl(
        var fd: SocketChannel?,
        frameRate: Int,
        var quality: Int
    ) : ImageReader.OnImageAvailableListener {
        var type: Int = 0 // 0:libjpeg-turbo 1:bitmap
        var framePeriodMs: Int = 1000 / frameRate

        var count: Int = 0
        var lastTime: Long = System.currentTimeMillis()
        var timeA: Long = lastTime

        override fun onImageAvailable(imageReader: ImageReader) {
            var jpegData: ByteArray? = null
            var jpegSize: ByteArray? = null
            var image: Image? = null

            synchronized(imageReaderLock) {
                try {
                    if (bImageReaderDisable) {
                        Ln.i("bImageReaderDisable !!!!!!!!!")
                        return
                    }
                    image = imageReader.acquireLatestImage()
                    if (image == null) return
                    val currentTime = System.currentTimeMillis()
                    if (framePeriodMs > currentTime - lastTime) return
                    lastTime = currentTime
                    val width = image.width
                    val height = image.height
                    val planes = image.planes
                    val buffer = planes[0]!!.buffer
                    val pixelStride = planes[0]!!.pixelStride
                    val rowStride = planes[0]!!.rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val pitch = width + rowPadding / pixelStride
                    if (type == 0) {
                        jpegData = JpegEncoder.compress(buffer, width, pitch, height, quality)
                    } else if (type == 1) {
                        val stream = ByteArrayOutputStream()
                        val bitmap = createBitmap(pitch, height)
                        bitmap.copyPixelsFromBuffer(buffer)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                        jpegData = stream.toByteArray()
                        bitmap.recycle()
                    }
                    if (jpegData == null) return
                    val b = ByteBuffer.allocate(4 + jpegData.size)
                    b.order(ByteOrder.LITTLE_ENDIAN)
                    b.putInt(jpegData.size)
                    b.put(jpegData)
                    jpegSize = b.array()
                    try {
                        IO.writeFully(fd, jpegSize, 0, jpegSize.size)
                    } catch (_: IOException) {
                        stop("image")
                    }
                } catch (e: Exception) {
                    Ln.e("onImageAvailable: " + e.message)
                } finally {
                    image?.close()
                }
            }

            count++
            val timeB = System.currentTimeMillis()
            if (timeB - timeA >= 1000) {
                timeA = timeB
                Ln.i("frame rate: " + count + ", jpeg size: " + jpegSize!!.size)
                count = 0
            }
        }
    }

    fun streamScreen(device: Device, fd: SocketChannel?) {
        device.setRotationListener(this)
        var alive: Boolean
        try {
            writeBanner(device, fd, scale)
            do {
                writeRotation(fd)
                val display: IBinder? = createDisplay()
                val contentRect = device.screenInfo.contentRect
                val videoRect = getDesiredSize(contentRect, scale)
                val mImageReader: ImageReader?
                synchronized(imageReaderLock) {
                    mImageReader = ImageReader.newInstance(
                        videoRect.width(), videoRect.height(), PixelFormat.RGBA_8888, 2
                    )
                    bImageReaderDisable = false
                }
                if (imageAvailableListenerImpl == null) {
                    imageAvailableListenerImpl =
                        ImageAvailableListenerImpl(fd, maxFps, quality)
                }
                mImageReader?.setOnImageAvailableListener(imageAvailableListenerImpl, handler)
                val surface = mImageReader?.surface
                setDisplaySurface(display, surface, contentRect, videoRect)
                synchronized(rotationLock) {
                    try {
                        (rotationLock as Object).wait()
                    } catch (_: InterruptedException) {
                    }
                }
                synchronized(imageReaderLock) {
                    bImageReaderDisable = true
                    mImageReader?.close()
                }
                destroyDisplay(display)
                surface?.release()
                alive = this.alive
                Ln.i("alive: $alive")
            } while (alive)
        } catch (e: Exception) {
            e.printStackTrace()
            Ln.e("streamScreen: " + e.message)
        } finally {
            mHandlerThread?.quit()
            device.setRotationListener(null)
        }
    }

    private fun getDesiredSize(contentRect: Rect, resolution: Int): Rect {
        val realWidth = contentRect.width()
        val realHeight = contentRect.height()
        var desiredWidth = realWidth
        var desiredHeight = realHeight
        val h = min(realWidth, realHeight)
        if (h > resolution) {
            desiredWidth = contentRect.width() * resolution / h
            desiredHeight = contentRect.height() * resolution / h
            desiredWidth = (desiredWidth + 4) and 7.inv()
            desiredHeight = (desiredHeight + 4) and 7.inv()
        } else {
            desiredWidth = desiredWidth and 7.inv()
            desiredHeight = desiredHeight and 7.inv()
        }
        Ln.i("realWidth: $realWidth, realHeight: $realHeight, desiredWidth: $desiredWidth, desiredHeight: $desiredHeight")
        return Rect(0, 0, desiredWidth, desiredHeight)
    }

    private fun writeRotation(fd: SocketChannel?) {
        val r = ByteBuffer.allocate(8)
        r.order(ByteOrder.LITTLE_ENDIAN)
        r.putInt(4)
        r.putInt(mRotation.get())
        val rArray = r.array()
        try {
            IO.writeFully(fd, rArray, 0, rArray.size)
        } catch (_: IOException) {
            stop("rotation")
        }
    }

    @Throws(IOException::class)
    private fun writeBanner(device: Device, fd: SocketChannel?, scale: Int) {
        val bannerSize: Byte = 24
        val version: Byte = 2
        val quirks: Byte = 2
        val pid = Process.myPid()
        val contentRect = device.screenInfo.contentRect
        val videoRect = getDesiredSize(contentRect, scale)
        val realWidth = contentRect.width()
        val realHeight = contentRect.height()
        val desiredWidth = videoRect.width()
        val desiredHeight = videoRect.height()
        val orientation = device.rotation.toByte()

        val b = ByteBuffer.allocate(bannerSize.toInt())
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.put(version) //version
        b.put(bannerSize) //banner size
        b.putInt(pid) //pid
        b.putInt(realWidth) //real width
        b.putInt(realHeight) //real height
        b.putInt(desiredWidth) //desired width
        b.putInt(desiredHeight) //desired height
        b.put(orientation) //orientation
        b.put(quirks) //quirks
        val array = b.array()
        IO.writeFully(fd, array, 0, array.size)
        Ln.i("banner\n{\n    version: $version\n    size: $bannerSize\n    real width: $realWidth\n    real height: $realHeight\n    desired width: $desiredWidth\n    desired height: $desiredHeight\n    orientation: $orientation\n    quirks: $quirks\n}\n")
    }

    companion object {
        private fun createDisplay(): IBinder? {
            return SurfaceControl.createDisplay("scrcpy", true)
        }

        private fun setDisplaySurface(
            display: IBinder?, surface: Surface?, deviceRect: Rect?, displayRect: Rect?
        ) {
            SurfaceControl.openTransaction()
            try {
                SurfaceControl.setDisplaySurface(display, surface)
                SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect)
                SurfaceControl.setDisplayLayerStack(display, 0)
            } finally {
                SurfaceControl.closeTransaction()
            }
        }

        private fun destroyDisplay(display: IBinder?) {
            SurfaceControl.destroyDisplay(display)
        }
    }

    fun stop(obj: String) {
        val msg = Message.obtain()
        msg.what = 1
        msg.obj = obj
        handler.sendMessage(msg)
    }
}