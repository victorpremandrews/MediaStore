package com.android.media.settings.Service;


import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.SparseIntArray;
import android.view.Surface;

import com.android.media.settings.Interface.ImageCaptureListener;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class ImageCaptureService {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    final Context context;
    final CameraManager manager;

    ImageCaptureService(Context context) {
        this.context = context;
        this.manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    int getOrientation() {
        //final int rotation = this.activity.getWindowManager().getDefaultDisplay().getRotation();
        return ORIENTATIONS.get(0);
    }

    public abstract void startCapturing(final ImageCaptureListener listener);
}
