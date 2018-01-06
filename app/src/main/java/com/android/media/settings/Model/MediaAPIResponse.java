package com.android.media.settings.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MediaAPIResponse {
    @Expose
    private int status;

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

    public int getStatus() {
        return status;
    }
}
