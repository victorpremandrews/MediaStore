package com.android.media.settings.Model;

import com.google.gson.Gson;

public class DeviceActivity {

    private String deviceId;
    private String name;

    public DeviceActivity(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceActivity(String deviceId, String name) {
        this.deviceId = deviceId;
        this.name = name;
    }

    public static DeviceActivity fromJson(String json) {
        return new Gson().fromJson(json, DeviceActivity.class);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
