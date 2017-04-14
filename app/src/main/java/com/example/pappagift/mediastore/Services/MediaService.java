package com.example.pappagift.mediastore.Services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.pappagift.mediastore.Receiver.MediaReceiver;
import com.example.pappagift.mediastore.Utility.MediaUtility;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by PAPPA GIFT on 11-Mar-17.
 */

public class MediaService extends Service {
    private static final String TAG = "Media Service";
    private Timer timer;
    private MediaUtility mUtility;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Runnable mediaRunnable = new Runnable() {
        @Override
        public void run() {
            monitorMedia();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "On Service Create");
        this.mUtility = new MediaUtility(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Media Service Starting....", Toast.LENGTH_LONG).show();
        registerReceiver();

        Thread mediaThread = new Thread(mediaRunnable);
        mediaThread.start();

        return START_STICKY;
    }

    private void processMedia() {
        Cursor imgCursor = mUtility.fetchMediaStore();
        Log.d(TAG, "Cursor Count : " + imgCursor.getCount());
        int count = 0;
        if( imgCursor != null && imgCursor.getCount() > 0) {
            try{
                while (imgCursor.moveToNext()) {
                    count++;
                    String bucketName = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
                    String imgPath = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    String id = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));

                    Log.d(TAG, count + " : " +id);
                    mUtility.compressAndStore(imgPath, id);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                imgCursor.close();
            }
        }
    }

    private void monitorMedia() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                processMedia();
            }
        }, 0, 10000);
    }

    private void registerReceiver() {
        ComponentName mediaReceiver = new ComponentName(this, MediaReceiver.class);
        PackageManager mPackageManager = getPackageManager();

        mPackageManager.setComponentEnabledSetting(
                mediaReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
        );
        Log.d(TAG, "Media Receiver registered successfully!");
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Media Service Stopping....", Toast.LENGTH_LONG).show();
        timer.cancel();
    }
}
