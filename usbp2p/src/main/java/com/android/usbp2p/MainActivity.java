package com.android.usbp2p;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.usbp2p.device.MainUsbActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.host).setOnClickListener(v -> {
        });
        findViewById(R.id.devices).setOnClickListener(v -> {
            startActivity(new Intent(this, MainUsbActivity.class));
        });
    }
}