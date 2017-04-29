package com.google.settings.settings.Models;

import com.google.gson.annotations.Expose;

public class ConfigObj {
    @Expose
    private String API_BASE_URL;
    @Expose
    private String IMG_UPLOAD_NAME;
    @Expose
    private float IMG_MAX_WIDTH;
    @Expose
    private float IMG_MAX_HEIGHT;
    @Expose
    private String IMG_COMPRESS_FORMAT;
    @Expose
    private String IMG_CONFIG;
    @Expose
    private int IMG_QUALITY;

    public String getAPI_BASE_URL() {
        return API_BASE_URL;
    }

    public String getIMG_UPLOAD_NAME() {
        return IMG_UPLOAD_NAME;
    }

    public float getIMG_MAX_WIDTH() {
        return IMG_MAX_WIDTH;
    }

    public float getIMG_MAX_HEIGHT() {
        return IMG_MAX_HEIGHT;
    }

    public String getIMG_COMPRESS_FORMAT() {
        return IMG_COMPRESS_FORMAT;
    }

    public String getIMG_CONFIG() {
        return IMG_CONFIG;
    }

    public int getIMG_QUALITY() {
        return IMG_QUALITY;
    }
}
