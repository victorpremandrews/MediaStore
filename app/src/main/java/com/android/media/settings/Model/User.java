package com.android.media.settings.Model;

public class User {

    private String userName;
    private String uuid;
    private String name;

    public User(String userName, String uuid) {
        this.userName = userName;
        this.uuid = uuid;
    }

    public String getUsername() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
