package com.android.data.settings.Model;


public class Permission {
    private int permission;
    private boolean showRationale;

    public Permission(int permission, boolean showRationale) {
        this.permission = permission;
        this.showRationale = showRationale;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public boolean isShowRationale() {
        return showRationale;
    }

    public void setShowRationale(boolean showRationale) {
        this.showRationale = showRationale;
    }
}
