package org.las2mile.scrcpy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SurfaceService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
