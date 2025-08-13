package com.bbogush.web_screen;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.net.LinkAddress;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.net.Inet6Address;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "WebRtcManager";
    private static final int PERM_MEDIA_PROJECTION_SERVICE = 101;
    private final int httpServerPort = 8080;
    private AppService appService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlUpdate();
        startService();
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, AppService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        AppServiceConnection serviceConnection = new AppServiceConnection();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private class AppServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AppService.AppServiceBinder binder = (AppService.AppServiceBinder) service;
            appService = binder.getService();
            if (!appService.isServerRunning()) askMediaProjectionPermission();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            appService = null;
        }
    }

    private void askMediaProjectionPermission() {
        Log.d(TAG, "askMediaProjectionPermission");
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), PERM_MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERM_MEDIA_PROJECTION_SERVICE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: RESULT_OK");
                appService.startHttpWebRtc(data, httpServerPort, getApplicationContext());
            }
        }
    }


    private void urlUpdate() {
        List<NetworkHelper.IpInfo> ipInfoList = NetworkHelper.getIpInfo(getApplicationContext());
        for (NetworkHelper.IpInfo ipInfo : ipInfoList) {
            List<LinkAddress> addresses = ipInfo.addresses;
            for (LinkAddress address : addresses) {
                if (address.getAddress() instanceof Inet6Address) continue;
                String url = "https://" + address.getAddress().getHostAddress() + ":" + httpServerPort;
                TextView connectionURL = findViewById(R.id.connectionURL);
                connectionURL.setText(url);
                break;
            }
        }
    }
}