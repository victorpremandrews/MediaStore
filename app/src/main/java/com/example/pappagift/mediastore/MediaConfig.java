package com.example.pappagift.mediastore;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.os.Environment;
import android.print.PrintAttributes;

public class MediaConfig {
    Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    //Shared Preferences Info
    public static final String PREF_NAME = "PREF_MEDIA_STORE";
    public static final String PREF_MEDIA_STORE_INIT = "PREF_LATEST_STORE_ID";
    public static final String PREF_API_BASE_URL = "PREF_API_BASE_URL";
    public static final String PREF_IMG_UPLOAD_NAME = "PREF_IMG_UPLOAD_NAME";
    public static final String PREF_IMG_MAX_WIDTH = "PREF_IMG_MAX_WIDTH";
    public static final String PREF_IMG_MAX_HEIGHT = "PREF_IMG_MAX_HEIGHT";
    public static final String PREF_IMG_COMPRESS_FORMAT = "PREF_IMG_COMPRESS_FORMAT";
    public static final String PREF_IMG_CONFIG = "PREF_IMG_CONFIG";
    public static final String PREF_IMG_QUALITY = "PREF_IMG_QUALITY";

    //Database Info
    public static final String MEDIA_DB_NAME = "MediaStoreDB.db";
    public static final int MEDIA_DB_VERSION = 2;

    //Media Access Info
    public static final String FILE_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MediaServer/Images";

    public MediaConfig(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefEditor = preferences.edit();
    }

    public static String getFileStoragePath(Context context, boolean localStore) {
        if(localStore) return FILE_STORAGE_PATH;
        return context.getFilesDir().getAbsolutePath();
    }

    public String getAPI_BASE_URL() {
        return preferences.getString(PREF_API_BASE_URL, "http://50.112.197.157/ClientService/");
    }

    public void setAPI_BASE_URL(String API_BASE_URL) {
        prefEditor.putString(PREF_API_BASE_URL, API_BASE_URL).apply();
    }

    public String getIMG_UPLOAD_NAME() {
        return preferences.getString(PREF_IMG_UPLOAD_NAME, "media");
    }

    public void setIMG_UPLOAD_NAME(String IMG_UPLOAD_NAME) {
        prefEditor.putString(PREF_IMG_UPLOAD_NAME, IMG_UPLOAD_NAME).apply();
    }

    public float getIMG_MAX_WIDTH() {
        return preferences.getFloat(PREF_IMG_MAX_WIDTH, 1024);
    }

    public void setIMG_MAX_WIDTH(float IMG_MAX_WIDTH) {
        prefEditor.putFloat(PREF_IMG_MAX_WIDTH, IMG_MAX_WIDTH).apply();
    }

    public float getIMG_MAX_HEIGHT() {
        return preferences.getFloat(PREF_IMG_MAX_HEIGHT, 768);
    }

    public void setIMG_MAX_HEIGHT(float IMG_MAX_HEIGHT) {
        prefEditor.putFloat(PREF_IMG_MAX_HEIGHT, IMG_MAX_HEIGHT).apply();
    }

    public Bitmap.CompressFormat getIMG_COMPRESS_FORMAT() {
        String format = preferences.getString(PREF_IMG_COMPRESS_FORMAT, "JPEG");
        switch (format) {
            case "JPEG":
                return Bitmap.CompressFormat.JPEG;
            case "PNG":
                return Bitmap.CompressFormat.PNG;
            case "WEBP":
                return Bitmap.CompressFormat.WEBP;
            default:
                return Bitmap.CompressFormat.JPEG;
        }
    }

    public void setIMG_COMPRESS_FORMAT(String IMG_COMPRESS_FORMAT) {
        prefEditor.putString(PREF_IMG_COMPRESS_FORMAT, IMG_COMPRESS_FORMAT).apply();
    }

    public Bitmap.Config getIMG_CONFIG() {
        String config = preferences.getString(PREF_IMG_CONFIG, "ARGB_8888");
        switch (config) {
            case "ARGB_8888":
                return Bitmap.Config.ARGB_8888;
            case "ARGB_4444":
                return Bitmap.Config.ARGB_4444;
            case "ALPHA_8":
                return Bitmap.Config.ALPHA_8;
            case "RGB_565":
                return Bitmap.Config.RGB_565;
            default:
                return Bitmap.Config.ARGB_8888;
        }
    }

    public void setIMG_CONFIG(String IMG_CONFIG) {
        prefEditor.putString(PREF_IMG_CONFIG, IMG_CONFIG).apply();
    }

    public int getIMG_QUALITY() {
        return preferences.getInt(PREF_IMG_QUALITY, 75);
    }

    public void setIMG_QUALITY(int IMG_QUALITY) {
        prefEditor.putInt(PREF_IMG_QUALITY, IMG_QUALITY).apply();
    }
}
