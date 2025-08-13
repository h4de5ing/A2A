package com.android.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.input.IInputManager
import android.os.Binder
import android.os.IBinder
import android.os.ServiceManager
import androidx.core.app.NotificationCompat

class AppService : Service() {
    inner class AppServiceBinder : Binder() {
        fun getService(): AppService = this@AppService
    }

    private val iBinder = AppServiceBinder()
    override fun onBind(intent: Intent?): IBinder = iBinder

    override fun onCreate() {
        super.onCreate()
        mInputManager = IInputManager.Stub.asInterface(ServiceManager.getService(INPUT_SERVICE))
    }
    private var mInputManager: IInputManager? = null
    private val notificationId: Int = 101
    private val channelId: String = "WebScreenServiceChannel"
    private val channelName: String = "WebScreen notification channel"
    private val contentTitle: String = "录屏中..."
    private val contentText: String = "点击关闭"
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.setAction(Intent.ACTION_MAIN)
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val channelId: String = createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setContentTitle(contentTitle)
                .setContentText(contentText).setSmallIcon(R.drawable.ic_android_black_24dp)
                .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentIntent(pendingIntent)
                .build()
        startForeground(notificationId, notification)
        return START_STICKY
    }

    private fun createNotificationChannel(): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}