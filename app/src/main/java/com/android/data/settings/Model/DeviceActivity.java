package com.android.data.settings.Model;

import com.google.gson.Gson;

public class DeviceActivity {

    private String deviceId;
    private String name;
    private Property property;

    public DeviceActivity(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceActivity(String deviceId, String name) {
        this.deviceId = deviceId;
        this.name = name;
    }

    public DeviceActivity(String deviceId, String name, Property property) {
        this.deviceId = deviceId;
        this.name = name;
        this.property = property;
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

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
}
