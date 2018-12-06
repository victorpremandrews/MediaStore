package com.android.data.settings.Utility;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;

import com.google.gson.Gson;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class CameraUtility {

    public static boolean isCameraExist(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public static List<String> availableCameras() {
        List<String> cameraList = new ArrayList<>();

        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraList.add("CAMERA_FACING_BACK");
            }
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraList.add("CAMERA_FACING_FRONT");
            }

        }
        return cameraList;
    }

    public static int totalCameras() {
        int count = 0;
        try {
            count = Camera.getNumberOfCameras();
        } catch (Exception e) { e.printStackTrace(); }
        return count;
    }

    public static List<Camera.Size> getPreviewSizes(int cameraId) throws Exception {
        Camera camera = getCameraInstance(cameraId);
        Camera.Parameters parameters = camera.getParameters();
        return parameters.getSupportedPreviewSizes();
    }

    public static List<Camera.Size> getPictireSizes(int cameraId) throws Exception {
        Camera camera = getCameraInstance(cameraId);
        Camera.Parameters parameters = camera.getParameters();
        return parameters.getSupportedPictureSizes();
    }

    public static String getSupportedSizes(int camId) throws Exception {
        List<Camera.Size> sizes = null;
        try {
            Camera camera = Camera.open(camId);
            Camera.Parameters parameters = camera.getParameters();
            sizes = parameters.getSupportedPictureSizes();
            camera.release();
        } catch (Exception e) { e.printStackTrace(); }
        return new Gson().toJson(sizes);
    }

    public static boolean isCameraExist(int cameraId) {
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraId) {
                return true;
            }
        }
        return false;
    }

    public static Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public static Camera.Size getBestSize(
            List<Camera.Size> supportedSizes,
            int displayWidth,
            int displayHeight) {

        double temporalDiff = 0;
        double diff = Integer.MAX_VALUE;

        Camera.Size size = null;
        Camera.Size supportedSize = null;

        if (supportedSizes != null) {
            Iterator<Camera.Size> iterator = supportedSizes.iterator();
            while (iterator.hasNext()) {
                supportedSize = iterator.next();
                temporalDiff = Math.sqrt(
                        Math.pow(supportedSize.width - displayWidth, 2) +
                                Math.pow(supportedSize.height - displayHeight, 2));

                if (temporalDiff < diff) {
                    diff = temporalDiff;
                    size = supportedSize;
                }
            }
        }
        return size;
    }

    public static Camera getCameraInstance (int cameraId) throws Exception {
        Camera c = null;
        c = Camera.open(cameraId);

        if(c == null) {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }
        return c;
    }

    public static File getOutputFile() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(path + File.separator +
                "IMG_"+ timeStamp + ".jpg");
        return mediaFile;
    }
}
