package com.example.pappagift.mediastore.Services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.pappagift.mediastore.MediaDBManager;
import com.example.pappagift.mediastore.Models.Media;
import com.example.pappagift.mediastore.Receiver.MediaReceiver;
import com.example.pappagift.mediastore.Utility.ConfigUpdaterUtility;
import com.example.pappagift.mediastore.Utility.MediaUploadUtility;
import com.example.pappagift.mediastore.Utility.MediaUtility;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class MediaService extends Service {
    private static final String TAG = "Media Service";
    private Timer imgMonitorTimer, imgUploadTimer, configTimer;
    private MediaUtility mUtility;
    private MediaDBManager dbManager;

    public static boolean isMediaMonitoring = false;
    public static boolean isMediaUploading = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Runnable mediaRunnable = new Runnable() {
        @Override
        public void run() {
            initConfigTimer();
            monitorMedia();
            initMediaUpload();
        }
    };

    @Override
    public void onCreate() {
        this.mUtility = new MediaUtility(this);
        this.dbManager = new MediaDBManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver();
        Thread mediaThread = new Thread(mediaRunnable);
        mediaThread.start();
        return START_STICKY;
    }

    private void processMedia() {
        MediaCompressorTask task = new MediaCompressorTask();
        task.execute(mUtility.fetchMediaStore());
    }

    private void monitorMedia() {
        imgMonitorTimer = new Timer();
        imgMonitorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isMediaMonitoring) processMedia();
            }
        }, 0, 1000 * 10);
    }

    private void initMediaUpload() {
        imgUploadTimer = new Timer();
        imgUploadTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(!isMediaUploading) {
                    MediaUploadUtility uploadUtility = new MediaUploadUtility(MediaService.this);
                    uploadUtility.initUploadServices();
                }
            }
        }, 0, 1000 * 60 * 10);
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

    private class MediaCompressorTask extends AsyncTask<Cursor, Void,Void> {

        @Override
        protected void onPreExecute() {
            isMediaMonitoring = true;
        }

        @Override
        protected Void doInBackground(Cursor... params) {
            Cursor imgCursor = params[0];
            if( imgCursor != null && imgCursor.getCount() > 0) {
                Log.d(TAG, "Media Count : " + imgCursor.getCount());
                try{
                    while (imgCursor.moveToNext()) {
                        String imgPath = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                        String id = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                        mUtility.compressAndStore(imgPath, id);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    imgCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isMediaMonitoring = false;
        }
    }

    @Override
    public void onDestroy() {
        imgMonitorTimer.cancel();
        imgUploadTimer.cancel();
        configTimer.cancel();
    }
}
