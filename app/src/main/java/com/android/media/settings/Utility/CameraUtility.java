package com.android.media.settings.Utility;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId);
        } catch (Exception e) {
            Log.d("TAG", "Open camera failed: " + e);
        }

        if(c == null) {
            try {
                c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } catch (Exception e) {
                Log.d("TAG", "Open camera failed: " + e);
            }
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
