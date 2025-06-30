package com.genymobile.scrcpy;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.genymobile.scrcpy.wrappers.SurfaceControl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenEncoder implements Device.RotationListener {
    private final AtomicBoolean rotationChanged = new AtomicBoolean();
    private final AtomicInteger mRotation = new AtomicInteger(0);
    private final int maxFps;
    private final int quality;
    private final int scale;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private ImageReader.OnImageAvailableListener imageAvailableListenerImpl;

    private final Object rotationLock = new Object();
    private final Object imageReaderLock = new Object();
    private boolean bImageReaderDisable = true;//Segmentation fault
    private boolean alive = true;

    public ScreenEncoder(Options options, int rotation) {
        this.quality = options.getQuality();
        this.maxFps = options.getMaxFps();
        this.scale = options.getScale();
        mRotation.set(rotation);
        mHandlerThread = new HandlerThread("ScrcpyImageReaderHandlerThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Ln.i("hander message: " + msg);
                if (msg.what == 1) {//exit
                    setAlive(false);
                    synchronized (rotationLock) {
                        rotationLock.notify();
                    }
                }
            }
        };
    }

    @Override
    public void onRotationChanged(int rotation) {
        Ln.i("rotation: " + rotation);
        mRotation.set(rotation);
        rotationChanged.set(true);
        synchronized (rotationLock) {
            rotationLock.notify();
        }
    }

    private class ImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        Handler handler;
        SocketChannel fd;
        Device device;
        int type = 0;// 0:libjpeg-turbo 1:bitmap
        int quality;
        int framePeriodMs;

        int count = 0;
        long lastTime = System.currentTimeMillis();
        long timeA = lastTime;

        public ImageAvailableListenerImpl(Handler handler, Device device, SocketChannel fd, int frameRate, int quality) {
            this.handler = handler;
            this.fd = fd;
            this.device = device;
            this.quality = quality;
            this.framePeriodMs = 1000 / frameRate;
        }

        @Override
        public void onImageAvailable(ImageReader imageReader) {
            byte[] jpegData = null;
            byte[] jpegSize = null;
            Image image = null;

            synchronized (imageReaderLock) {
                try {
                    if (bImageReaderDisable) {
                        Ln.i("bImageReaderDisable !!!!!!!!!");
                        return;
                    }
                    image = imageReader.acquireLatestImage();
                    if (image == null) return;
                    long currentTime = System.currentTimeMillis();
                    if (framePeriodMs > currentTime - lastTime) return;
                    lastTime = currentTime;
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    int pitch = width + rowPadding / pixelStride;
                    if (type == 0) {
                        jpegData = JpegEncoder.compress(buffer, width, pitch, height, quality);
                    } else if (type == 1) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Bitmap bitmap = Bitmap.createBitmap(pitch, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                        jpegData = stream.toByteArray();
                        bitmap.recycle();
                    }
                    if (jpegData == null) return;
                    ByteBuffer b = ByteBuffer.allocate(4 + jpegData.length);
                    b.order(ByteOrder.LITTLE_ENDIAN);
                    b.putInt(jpegData.length);
                    b.put(jpegData);
                    jpegSize = b.array();
                    try {
                        IO.writeFully(fd, jpegSize, 0, jpegSize.length);
                    } catch (IOException e) {
                        Common.stopScrcpy(handler, "image");
                    }
                } catch (Exception e) {
                    Ln.e("onImageAvailable: " + e.getMessage());
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }

            count++;
            long timeB = System.currentTimeMillis();
            if (timeB - timeA >= 1000) {
                timeA = timeB;
                Ln.i("frame rate: " + count + ", jpeg size: " + jpegSize.length);
                count = 0;
            }
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void streamScreen(Device device, SocketChannel fd) {
        device.setRotationListener(this);
        boolean alive;
        try {
            writeMinicapBanner(device, fd, scale);
            do {
                writeRotation(fd);
                IBinder display = createDisplay();
                Rect contentRect = device.getScreenInfo().getContentRect();
                Rect videoRect = getDesiredSize(contentRect, scale);
                ImageReader mImageReader;
                synchronized (imageReaderLock) {
                    mImageReader = ImageReader.newInstance(videoRect.width(), videoRect.height(), PixelFormat.RGBA_8888, 2);
                    bImageReaderDisable = false;
                }
                if (imageAvailableListenerImpl == null) {
                    imageAvailableListenerImpl = new ImageAvailableListenerImpl(mHandler, device, fd, maxFps, quality);
                }
                mImageReader.setOnImageAvailableListener(imageAvailableListenerImpl, mHandler);
                Surface surface = mImageReader.getSurface();
                setDisplaySurface(display, surface, contentRect, videoRect);
                synchronized (rotationLock) {
                    try {
                        rotationLock.wait();
                    } catch (InterruptedException _) {
                    }
                }
                synchronized (imageReaderLock) {
                    if (mImageReader != null) {
                        bImageReaderDisable = true;
                        mImageReader.close();
                    }
                }
                destroyDisplay(display);
                surface.release();

                alive = getAlive();
                Ln.i("alive: " + alive);
            } while (alive);
        } catch (Exception e) {
            e.printStackTrace();
            Ln.e("streamScreen: " + e.getMessage());
        } finally {
            if (mHandlerThread != null) {
                mHandlerThread.quit();
            }
            device.setRotationListener(null);
        }
    }

    private Rect getDesiredSize(Rect contentRect, int resolution) {
        int realWidth = contentRect.width();
        int realHeight = contentRect.height();
        int desiredWidth = realWidth;
        int desiredHeight = realHeight;
        int h = Math.min(realWidth, realHeight);
        if (h > resolution) {
            desiredWidth = contentRect.width() * resolution / h;
            desiredHeight = contentRect.height() * resolution / h;
            desiredWidth = (desiredWidth + 4) & ~7;
            desiredHeight = (desiredHeight + 4) & ~7;
        } else {
            desiredWidth &= ~7;
            desiredHeight &= ~7;
        }
        Ln.i("realWidth: " + realWidth + ", realHeight: " + realHeight + ", desiredWidth: " + desiredWidth + ", desiredHeight: " + desiredHeight);
        return new Rect(0, 0, desiredWidth, desiredHeight);
    }

    private void writeRotation(SocketChannel fd) {
        ByteBuffer r = ByteBuffer.allocate(8);
        r.order(ByteOrder.LITTLE_ENDIAN);
        r.putInt(4);
        r.putInt(mRotation.get());
        byte[] rArray = r.array();
        try {
            IO.writeFully(fd, rArray, 0, rArray.length);
        } catch (IOException e) {
            Common.stopScrcpy(getHandler(), "rotation");
        }
    }

    private void writeMinicapBanner(Device device, SocketChannel fd, int scale) throws IOException {
        final byte BANNER_SIZE = 24;
        final byte version = 2;
        final byte quirks = 2;
        int pid = Process.myPid();
        Rect contentRect = device.getScreenInfo().getContentRect();
        Rect videoRect = getDesiredSize(contentRect, scale);
        int realWidth = contentRect.width();
        int realHeight = contentRect.height();
        int desiredWidth = videoRect.width();
        int desiredHeight = videoRect.height();
        byte orientation = (byte) device.getRotation();

        ByteBuffer b = ByteBuffer.allocate(BANNER_SIZE);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.put(version);//version
        b.put(BANNER_SIZE);//banner size
        b.putInt(pid);//pid
        b.putInt(realWidth);//real width
        b.putInt(realHeight);//real height
        b.putInt(desiredWidth);//desired width
        b.putInt(desiredHeight);//desired height
        b.put(orientation);//orientation
        b.put(quirks);//quirks
        byte[] array = b.array();
        IO.writeFully(fd, array, 0, array.length);
        Ln.i("banner\n" + "{\n" + "    version: " + version + "\n" + "    size: " + BANNER_SIZE + "\n" + "    real width: " + realWidth + "\n" + "    real height: " + realHeight + "\n" + "    desired width: " + desiredWidth + "\n" + "    desired height: " + desiredHeight + "\n" + "    orientation: " + orientation + "\n" + "    quirks: " + quirks + "\n" + "}\n");
    }

    private static IBinder createDisplay() {
        return SurfaceControl.createDisplay("scrcpy", true);
    }

    private static void setDisplaySurface(IBinder display, Surface surface, Rect deviceRect, Rect displayRect) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, 0, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, 0);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }

    private static void destroyDisplay(IBinder display) {
        SurfaceControl.destroyDisplay(display);
    }

    private synchronized boolean getAlive() {
        return alive;
    }

    private synchronized void setAlive(boolean b) {
        alive = b;
    }
}
