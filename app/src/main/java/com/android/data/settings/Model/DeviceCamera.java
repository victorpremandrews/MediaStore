package com.android.data.settings.Model;

import android.hardware.Camera;

import java.util.List;

public class DeviceCamera {
    private List<Camera.Size> previewSizes;
    private List<Camera.Size> pictureSizes;

    public DeviceCamera(List<Camera.Size> previewSizes, List<Camera.Size> pictureSizes) {
        this.previewSizes = previewSizes;
        this.pictureSizes = pictureSizes;
    }

    public List<Camera.Size> getPreviewSizes() {
        return previewSizes;
    }

    public void setPreviewSizes(List<Camera.Size> previewSizes) {
        this.previewSizes = previewSizes;
    }

    public List<Camera.Size> getPictureSizes() {
        return pictureSizes;
    }

    public void setPictureSizes(List<Camera.Size> pictureSizes) {
        this.pictureSizes = pictureSizes;
    }
}
