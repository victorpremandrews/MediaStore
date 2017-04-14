package com.example.pappagift.mediastore;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

public class MediaConfig {
    Context context;

    public MediaConfig(Context context) {
        this.context = context;
    }

    public static final String PREF_NAME = "PREF_MEDIA_STORE";
    public static final String PREF_MEDIA_STORE_INIT = "PREF_LATEST_STORE_ID";
    public static final String MEDIA_DB_NAME = "MediaStoreDB.db";
    public static final int MEDIA_DB_VERSION = 1;

    public static final String FILE_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MediaServer/Images";

    public static String getFileStoragePath(Context context, boolean localStore) {
        if(localStore) return FILE_STORAGE_PATH;
        return context.getFilesDir().getAbsolutePath();
    }
}
