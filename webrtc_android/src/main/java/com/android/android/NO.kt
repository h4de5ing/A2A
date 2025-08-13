package com.android.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import androidx.core.app.NotificationCompat

class NO {
    private val notificationId: Int = 101
    public val channelId: String = "WebScreenServiceChannel"
    private val channelName: String = "WebScreen notification channel"
    private val contentTitle: String = "录屏中..."
    private val contentText: String = "点击关闭"
    fun no(context: Context):Notification{
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.setAction(Intent.ACTION_MAIN)
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val channelId: String = createNotificationChannel(context)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setContentTitle(contentTitle)
                .setContentText(contentText).setSmallIcon(R.drawable.ic_android_black_24dp)
                .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentIntent(pendingIntent)
                .build()
//        context.startForeground(notificationId, notification)
        return notification
    }
    private fun createNotificationChannel(context: Context): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}