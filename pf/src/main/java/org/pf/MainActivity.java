/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.h4de5ing.pf.R;
import com.logger.Log;
import com.logger.LogFragment;
import com.logger.LogWrapper;
import com.logger.MessageOnlyLogFilter;

/**
 * Forward a single TCP port to an outside host + port. Only supports one socket
 * at a time. Source based on com.example.android.networkconnect.
 */
public class MainActivity extends FragmentActivity {

    public static final String TAG = "Network Connect";
    public static final String PREFS_NAME = "PortForwardingSettings";
    public static final String USAGE_UPDATE = "org.pf.USAGE_UPDATE";

    Menu menu;
    boolean forwarding = true;
    String titleForwarding = "Forwarding. Tap to stop.";
    String titlePaused = "Paused. Tap to begin.";
    SimpleTextFragment introFragment;
    // Reference to the fragment showing events, so we can clear it with a button
    // as necessary.
    private LogFragment mLogFragment;

    TextView localPortView;
    TextView remotePortView;
    TextView remoteHostView;

    int remotePort;
    int localPort;
    String remoteHost;
    BroadcastReceiver updateReceiver = null;
    LocalBroadcastManager bm;

    private void killThread() {
        Intent i = new Intent(this, PortForward.class);
        stopService(i);
    }

    private void readTextViews() {
        remotePort = Integer.parseInt(remotePortView.getText().toString().trim());
        localPort = Integer.parseInt(localPortView.getText().toString().trim());
        remoteHost = remoteHostView.getText().toString().trim();
    }

    private void newThread() {
        readTextViews();
        Intent i = new Intent(this, PortForward.class)
                .putExtra("localPort", localPort)
                .putExtra("remotePort", remotePort)
                .putExtra("remoteHost", remoteHost);

        startService(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForUpdate();
    }

    private void listenForUpdate() {
        try {
            bm.unregisterReceiver(updateReceiver);
        } catch (IllegalArgumentException ignored) {
        }

        IntentFilter f = new IntentFilter();
        f.addAction(MainActivity.USAGE_UPDATE);
        bm.registerReceiver(updateReceiver, f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bm.unregisterReceiver(updateReceiver);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            finish();
            return;
        }

        Log.d(TAG, "Creating Activity");

        setContentView(R.layout.sample_main);

        // Initialize text fragment that displays intro text.
        introFragment = (SimpleTextFragment)
                getSupportFragmentManager().findFragmentById(R.id.intro_fragment);
        introFragment.setText(R.string.welcome_message);
        introFragment.getTextView().setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f);
        introFragment.getTextView().setText("Waiting to begin.");
        initializeLogging();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        remotePort = settings.getInt("remotePort", -1);
        localPort = settings.getInt("localPort", -1);
        remoteHost = settings.getString("remoteHost", "");

        localPortView = (TextView) findViewById(R.id.local_port);
        remotePortView = (TextView) findViewById(R.id.remote_port);
        remoteHostView = (TextView) findViewById(R.id.remote_host);

        if (localPort > 0) {
            localPortView.setText(String.valueOf(localPort));
        }
        if (remotePort > 0) {
            remotePortView.setText(String.valueOf(remotePort));
        }
        if (!remoteHost.equals("")) {
            remoteHostView.setText(remoteHost);
            newThread();

        } else {
            forwarding = false;
        }

        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MainActivity.USAGE_UPDATE)) {

                    introFragment.getTextView().setText(
                            String.format("Up: %.2fkb\nDown: %.2fkb",
                                    intent.getIntExtra("bUp", -2000) / 1000.0,
                                    intent.getIntExtra("bDown", -2000) / 1000.0));
                }
            }
        };

        bm = LocalBroadcastManager.getInstance(this);

        listenForUpdate();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        setForwardingString(menu.findItem(R.id.fetch_action));
        return true;
    }

    private void setForwardingString(MenuItem i) {
        if (forwarding) {
            i.setTitle(titleForwarding);
        } else {
            i.setTitle(titlePaused);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.fetch_action) {
            this.forwarding = !this.forwarding;
            if (this.forwarding) {
                newThread();
            } else {
                killThread();
            }
            setForwardingString(item);
            return true;
        } else if (id == R.id.clear_action) {
            mLogFragment.getLogView().setText("");
            readTextViews();
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("localPort", localPort);
            editor.putInt("remotePort", remotePort);
            editor.putString("remoteHost", remoteHost);
            editor.commit();
            return true;
        }
        return false;
    }

    /**
     * Create a chain of targets that will receive log data
     */
    public void initializeLogging() {

        // Using Log, front-end to the logging chain, emulates
        // android.util.log method signatures.

        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        // A filter that strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        mLogFragment =
                (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);
        msgFilter.setNext(mLogFragment.getLogView());
    }
}
