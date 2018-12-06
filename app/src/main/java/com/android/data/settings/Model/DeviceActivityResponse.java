package com.android.data.settings.Model;

public class DeviceActivityResponse {
    private String name;
    private String deviceId;
    private Media media;
    private String message;

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

    public DeviceActivityResponse(String name, String deviceId, String message) {
        this.name = name;
        this.deviceId = deviceId;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
