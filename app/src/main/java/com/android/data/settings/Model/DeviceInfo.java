package com.android.data.settings.Model;

import com.google.gson.Gson;

import java.util.List;

public class DeviceInfo {
    private int camCount = 0;
    private List<DeviceCamera> cameras;

    public int getCamCount() {
        return camCount;
    }

    public void setCamCount(int camCount) {
        this.camCount = camCount;
    }

    public List<DeviceCamera> getCameras() {
        return cameras;
    }

    public void setCameras(List<DeviceCamera> camera) {
        this.cameras = camera;
    }

    public static DeviceInfo fromJson(String json) throws Exception {
        return new Gson().fromJson(json, DeviceInfo.class);
    }

    public static String toJson(DeviceInfo deviceInfo) throws Exception {
        return new Gson().toJson(deviceInfo);
    }
}
