package com.android.data.settings.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MediaConfigResponse {
    @Expose
    private String status;

    @Expose
    private String msg;

    @SerializedName("data")
    @Expose
    private ConfigObj data;

    public String getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public ConfigObj getData() {
        return data;
    }
}
