package com.example.pappagift.mediastore;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

public class MediaConfig {
    Context context;

    public MediaConfig(Context context) {
        this.context = context;
    }

    public static final String FILE_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MediaServer/Images";

    public static String getFileStoragePath(Context context, boolean localStore) {
        if(localStore) return FILE_STORAGE_PATH;
        return context.getFilesDir().getAbsolutePath();
    }
}
