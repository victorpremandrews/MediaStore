package com.google.settings.settings.Models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MediaAPIResponse {
    @Expose
    private String status;

    @Expose
    private String msg;

    @SerializedName("data")
    @Expose
    private String data;

    public String getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public String getStatus() {
        return status;
    }
}
