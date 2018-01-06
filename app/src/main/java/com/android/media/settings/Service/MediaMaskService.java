package com.android.media.settings.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.media.settings.MediaConfig;
import com.android.media.settings.Utility.MediaUtility;


public class MediaMaskService extends Service {
    private static final String TAG = "MediaMaskService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Media fake service created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(MediaConfig.NOTIFICATION_ID, new MediaUtility(this).getNotification());
        Log.d(TAG, "Media fake service started");
        stopForeground(true);
        stopSelf();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
