package com.android.media.settings.Models;

/**
 * Created by PAPPA GIFT on 14-Apr-17.
 */

public class Media {
    private String id;
    private String path;
    private int status = 0;

    public Media() {
    }

    public Media(String id, String path) {
        this.id = id;
        this.path = path;
    }

    public Media(String id, String path, int status) {
        this.id = id;
        this.path = path;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
