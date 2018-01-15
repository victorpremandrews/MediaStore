package com.android.media.settings.Model;

public class DeviceActivityResponse {
    private String name;
    private String deviceId;
    private Media media;

    public DeviceActivityResponse(String deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceActivityResponse(String name, Media media) {
        this.name = name;
        this.media = media;
    }

    public DeviceActivityResponse(String name, String deviceId, Media media) {
        this.name = name;
        this.deviceId = deviceId;
        this.media = media;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Media getMedia() {
        return media;
    }

    public void setMedia(Media media) {
        this.media = media;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
