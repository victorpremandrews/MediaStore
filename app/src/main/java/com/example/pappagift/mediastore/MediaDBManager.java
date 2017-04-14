package com.example.pappagift.mediastore;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.pappagift.mediastore.Models.Media;

/**
 * Created by PAPPA GIFT on 14-Apr-17.
 */

public class MediaDBManager extends SQLiteOpenHelper {
    public static final String TAG = "DBManager";
    Context context;
    String TABLE_MEDIA_STORE = "MediaStore";
    String COLUMN_PICS_ID = "_id";
    String COLUMN_PICS_STORE_ID = "store_id";
    String COLUMN_PICS_IS_UPLOADED = "upload_status"; //0-local, 1-uploaded, 2-upload error

    public MediaDBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, MediaConfig.MEDIA_DB_NAME, factory, MediaConfig.MEDIA_DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String tblPicsQuery = "CREATE TABLE " + TABLE_MEDIA_STORE + " ( " +
                COLUMN_PICS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                COLUMN_PICS_STORE_ID + " TEXT,"+
                COLUMN_PICS_IS_UPLOADED +" INTEGER )";

        try {
            db.execSQL(tblPicsQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA_STORE);
    }

    public void insertMedia(Media media) {
        if(!isMediaExists(media)) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PICS_STORE_ID, media.getId());
            values.put(COLUMN_PICS_IS_UPLOADED, media.getStatus());

            SQLiteDatabase db = getWritableDatabase();
            db.insert(TABLE_MEDIA_STORE, null, values);
            db.close();
        }
    }

    public boolean isMediaExists(Media media) {
        String query = "SELECT * FROM " + TABLE_MEDIA_STORE + " WHERE " + COLUMN_PICS_STORE_ID + " = " + media.getId();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        try {
            if(c.moveToFirst()) return true;

        }finally {
            c.close();
            db.close();
        }
        return false;
    }

    private String getPrefID() {
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(MediaConfig.PREF_MEDIA_STORE_INIT, "0");
    }

    public String lastInsertedMedia() {
        String query = "SELECT MAX(" + COLUMN_PICS_STORE_ID + ") FROM " + TABLE_MEDIA_STORE;
        String id = "0";
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        try {
            if(c.moveToFirst()) {
                id = c.getString(0);
            }
        } finally {
            c.close();
            db.close();
        }
        if(id == null) {
            id = getPrefID();
        }
        return id;
    }
}
