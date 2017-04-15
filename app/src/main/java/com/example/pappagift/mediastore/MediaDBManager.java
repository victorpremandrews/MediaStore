package com.example.pappagift.mediastore;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.pappagift.mediastore.Models.Media;

public class MediaDBManager extends SQLiteOpenHelper {
    static final String TAG = "DBManager";
    private Context context;
    private String TABLE_MEDIA_STORE = "MediaStore";
    private static String COLUMN_PICS_ID = "_id";
    public static final String COLUMN_PICS_STORE_ID = "store_id";
    public static final String COLUMN_PICS_STORE_URL = "store_url";
    private static final String COLUMN_PICS_IS_UPLOADED = "upload_status"; //0-local, 1-uploaded, 2-upload error

    public MediaDBManager(Context context) {
        super(context, MediaConfig.MEDIA_DB_NAME, null, MediaConfig.MEDIA_DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String tblPicsQuery = "CREATE TABLE " + TABLE_MEDIA_STORE + " ( " +
                COLUMN_PICS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PICS_STORE_ID + " TEXT," +
                COLUMN_PICS_STORE_URL + " TEXT, " +
                COLUMN_PICS_IS_UPLOADED +" INTEGER )";
        try {
            db.execSQL(tblPicsQuery);
            Log.d(TAG, "TABLE " + TABLE_MEDIA_STORE + " CREATED!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA_STORE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void insertMedia(Media media) {
        if(!isMediaExists(media)) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_PICS_STORE_ID, media.getId());
            values.put(COLUMN_PICS_STORE_URL, media.getPath());
            values.put(COLUMN_PICS_IS_UPLOADED, media.getStatus());

            SQLiteDatabase db = getWritableDatabase();
            db.insert(TABLE_MEDIA_STORE, null, values);
            db.close();
        }
    }

    private boolean isMediaExists(Media media) {
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

    public Cursor getLocalMedia() {
        String[] columns = new String[] {COLUMN_PICS_STORE_ID, COLUMN_PICS_STORE_URL};
        String selection = COLUMN_PICS_IS_UPLOADED + " = ? ";
        String[] selectionArgs = new String[] { "0" };
        String orderBy = COLUMN_PICS_ID + " ASC ";
        String limit = " 3 ";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDIA_STORE, columns, selection, selectionArgs, null, null, orderBy, limit);
        return cursor;
    }

    public Media getMedia(String id) {
        String query = "SELECT * FROM " + TABLE_MEDIA_STORE + " WHERE " + COLUMN_PICS_STORE_ID + " = " + id + " LIMIT 0, 1";
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        Media media = null;
        try {
            if(c != null && c.moveToNext()) {
                media = new Media(
                        c.getString(c.getColumnIndex(COLUMN_PICS_STORE_ID)),
                        c.getString(c.getColumnIndex(COLUMN_PICS_STORE_URL)),
                        c.getInt(c.getColumnIndex(COLUMN_PICS_IS_UPLOADED))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
            db.close();
        }
        return media;
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

    public boolean updateUploadStatus(Media media) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PICS_IS_UPLOADED, media.getStatus());
        String where = COLUMN_PICS_STORE_ID + " = ? ";
        String[] whereArgs = new String[] { media.getId() };

        SQLiteDatabase db = getWritableDatabase();
        try {
            db.update(TABLE_MEDIA_STORE, values, where, whereArgs);
            return true;
        } catch(Exception e) {
            return false;
        } finally {
            db.close();
        }
    }
}
