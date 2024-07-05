package org.las2mile.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends Activity implements Scrcpy.ServiceCallbacks, SensorEventListener {
    private static final String PREFERENCE_KEY = "default";
    private static int screenWidth = 800;
    private static int screenHeight = 1280;
    private static boolean landscape = false;
    private static boolean first_time = true;
    private static boolean result_of_Rotation = false;
    private static boolean serviceBound = false;
    private String serverAdr = null;
    private SurfaceView surfaceView;
    private Scrcpy scrcpy;
    private long timestamp = 0;
    private static boolean no_control = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            scrcpy = ((Scrcpy.MyServiceBinder) iBinder).getService();
            scrcpy.setServiceCallbacks(MainActivity.this);
            serviceBound = true;
            if (first_time) {
                scrcpy.start(surfaceView, serverAdr, screenHeight, screenWidth);
                int count = 100;
                while (count != 0 && !scrcpy.check_socket_connection()) {
                    count--;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (count == 0) {
                    if (serviceBound) {
                        scrcpy.StopService();
                        unbindService(serviceConnection);
                        serviceBound = false;
                        mainUI();
                    }
                    Toast.makeText(MainActivity.this, "Connection Timed out", Toast.LENGTH_SHORT).show();
                }
            } else {
                scrcpy.setParms(surfaceView, screenWidth, screenHeight);
            }
            if (!no_control) {
                surfaceView.setOnTouchListener((v, event) -> scrcpy.touchEvent(event, surfaceView.getWidth(), surfaceView.getHeight()));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (first_time) mainUI();
        else mirrorScreen();
        SensorManager sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        Sensor proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @SuppressLint("SourceLockedOrientationActivity")
    public void mainUI() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        Button startButton = findViewById(R.id.button_start);
        TextView ip = findViewById(R.id.ip);
        ip.setText("本机IP:" + Tools.getMyIp());
        startButton.setOnClickListener(v -> {
            getAttributes();
            if (!serverAdr.isEmpty()) {
                mirrorScreen();
            } else {
                Toast.makeText(this, "Server Address Empty", Toast.LENGTH_SHORT).show();
            }
        });
        get_saved_preferences();
    }


    public void get_saved_preferences() {
        final EditText editTextServerHost = findViewById(R.id.editText_server_host);
        final Switch aSwitch0 = findViewById(R.id.switch0);
        editTextServerHost.setText(getSharedPreferences(PREFERENCE_KEY, 0).getString("Server Address", ""));
        aSwitch0.setChecked(getSharedPreferences(PREFERENCE_KEY, 0).getBoolean("No Control", false));
    }


    private void getAttributes() {
        EditText editTextServerHost = findViewById(R.id.editText_server_host);
        serverAdr = editTextServerHost.getText().toString();
        getSharedPreferences(PREFERENCE_KEY, 0).edit().putString("Server Address", serverAdr).apply();
        Switch a_Switch0 = findViewById(R.id.switch0);
        no_control = a_Switch0.isChecked();
        getSharedPreferences(PREFERENCE_KEY, 0).edit().putBoolean("No Control", no_control).apply();
    }


    private void swapDimensions() {
        int temp = screenHeight;
        screenHeight = screenWidth;
        screenWidth = temp;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void mirrorScreen() {
        setContentView(R.layout.surface);
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        surfaceView = findViewById(R.id.decoder_surface);
        startScrcpyService();
    }

    private void startScrcpyService() {
        Intent intent = new Intent(this, Scrcpy.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void loadNewRotation() {
        unbindService(serviceConnection);
        serviceBound = false;
        result_of_Rotation = true;
        landscape = !landscape;
        swapDimensions();
        if (landscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) scrcpy.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!first_time && !result_of_Rotation) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            if (serviceBound) scrcpy.resume();
        }
        result_of_Rotation = false;
    }

    @Override
    public void onBackPressed() {
        if (timestamp == 0) {
            timestamp = SystemClock.uptimeMillis();
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        } else {
            long now = SystemClock.uptimeMillis();
            if (now < timestamp + 1000) {
                timestamp = 0;
                if (serviceBound) {
                    scrcpy.StopService();
                    unbindService(serviceConnection);
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
            timestamp = 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values[0] == 0) {
                if (serviceBound) scrcpy.sendKeyEvent(28);//clean
            } else {
                if (serviceBound) scrcpy.sendKeyEvent(29);//A
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}