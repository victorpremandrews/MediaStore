package com.android.data.settings.Model;


import android.hardware.Camera;

import com.google.gson.Gson;

public class Property {
    public static final String SNAP_MODE_SINGLE = "single";
    public static final String SNAP_MODE_BURST_10 = "burst_10";
    public static final String SNAP_MODE_BURST_STREAM = "burst_stream";

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int pictureResolution = 0;
    private int previewResolution = 0;
    private int rotation = 270;
    private String focusMode = Camera.Parameters.FOCUS_MODE_AUTO;
    private String snapMode = SNAP_MODE_SINGLE;
    private int snapDelay = 1000;
    private int streamDelay = 3000;
    private boolean resetService = false;

    public static String toJson(Property property) {
        return new Gson().toJson(property);
    }

    public static Property fromJson(String strProperty) {
        return new Gson().fromJson(strProperty, Property.class);
    }

    public int getSnapDelay() {
        return snapDelay;
    }

    public void setSnapDelay(int snapDelay) {
        this.snapDelay = snapDelay;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public int getPictureResolution() {
        return pictureResolution;
    }

    public void setPictureResolution(int pictureResolution) {
        this.pictureResolution = pictureResolution;
    }

    public int getPreviewResolution() {
        return previewResolution;
    }

    public void setPreviewResolution(int previewResolution) {
        this.previewResolution = previewResolution;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public String getFocusMode() {
        return focusMode;
    }

    public void setFocusMode(String focusMode) {
        this.focusMode = focusMode;
    }

    public String getSnapMode() {
        return snapMode;
    }

    public void setSnapMode(String snapMode) {
        this.snapMode = snapMode;
    }

    public boolean isResetService() {
        return resetService;
    }

    public void setResetService(boolean resetService) {
        this.resetService = resetService;
    }

    public int getStreamDelay() {
        return streamDelay;
    }

    public void setStreamDelay(int streamDelay) {
        this.streamDelay = streamDelay;
    }
}
