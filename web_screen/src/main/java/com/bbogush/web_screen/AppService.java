package com.bbogush.web_screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

public class AppService extends Service {
    private static final String TAG = "WebRtcManager";
    private static final int SERVICE_ID = 101;
    private static final String NOTIFICATION_CHANNEL_ID = "WebScreenServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "WebScreen notification channel";
    private static final String NOTIFICATION_TITLE = "录屏中...";
    private static final String NOTIFICATION_CONTENT = "点击关闭";
    private final IBinder iBinder = new AppServiceBinder();
    private WebRtcManager webRtcManager = null;
    private HttpServer httpServer = null;
    private boolean isWebServerRunning = false;

    @Override
    public void onCreate() {
        Log.d(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        String channelId = createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true).setContentTitle(NOTIFICATION_TITLE).setContentText(NOTIFICATION_CONTENT).setSmallIcon(R.drawable.ic_stat_name).setCategory(NotificationCompat.CATEGORY_SERVICE).setContentIntent(pendingIntent).build();
        startForeground(SERVICE_ID, notification);
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    private String createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        return NOTIFICATION_CHANNEL_ID;
    }

    public class AppServiceBinder extends Binder {
        AppService getService() {
            return AppService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public void startHttpWebRtc(Intent intent, int port, Context context) {
        Log.d(TAG, "startHttpWebRtc " + port);
        if (!(isWebServerRunning = startHttpServer(port))) return;
        webRtcManager = new WebRtcManager(intent, context, httpServer);
    }

    public void serverStop() {
        if (!isWebServerRunning) return;
        isWebServerRunning = false;
        stopHttpServer();
        webRtcManager.close();
        webRtcManager = null;
    }

    public boolean isServerRunning() {
        return isWebServerRunning;
    }

    public boolean startHttpServer(int httpServerPort) {
        httpServer = new HttpServer(httpServerPort, getApplicationContext(), httpServerInterface);
        try {
            httpServer.start();
        } catch (IOException e) {
            String fmt = getResources().getString(R.string.port_in_use);
            String errorMessage = String.format(Locale.getDefault(), fmt, httpServerPort);
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void stopHttpServer() {
        if (httpServer == null) return;
        Thread thread = new Thread(() -> {
            try {
                httpServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        httpServer = null;
    }

    private final HttpServer.HttpServerInterface httpServerInterface = new HttpServer.HttpServerInterface() {

        @Override
        public void onJoin(HttpServer server) {
            if (webRtcManager == null) return;
            webRtcManager.start(server);
        }

        @Override
        public void onSdp(JSONObject message) {
            if (webRtcManager == null) return;
            webRtcManager.onAnswerReceived(message);
        }

        @Override
        public void onIceCandidate(JSONObject message) {
            if (webRtcManager == null) return;
            webRtcManager.onIceCandidateReceived(message);
        }

        @Override
        public void onBye() {
            if (webRtcManager == null) return;
            webRtcManager.stop();
        }

        @Override
        public void onWebSocketClose() {
            if (webRtcManager == null) return;
            webRtcManager.stop();
        }
    };
}
