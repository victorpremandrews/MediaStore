package com.android.media.settings;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.MediaSMS;
import com.android.media.settings.Utility.MediaUtility;

import java.util.ArrayList;
import java.util.List;

public class MediaDBManager extends SQLiteOpenHelper {
    static final String TAG = "DBManager";
    private Context context;

    private static final String TABLE_MEDIA_STORE = "MediaStore";
    private static final String COLUMN_PICS_ID = "_id";
    public static final String COLUMN_PICS_STORE_ID = "store_id";
    public static final String COLUMN_PICS_STORE_URL = "store_url";
    private static final String COLUMN_PICS_IS_UPLOADED = "upload_status"; //0-local, 1-uploaded, 2-upload error, 3-read
    private static final String COLUMN_PICS_READ_TIMESTAMP = "read_timestamp";

    private static final String TABLE_MEDIA_SMS = "MediaSMSStore";
    private static final String COLUMN_SMS_ID = "_id";
    public static final String COLUMN_SMS_FROM = "sender";
    public static final String COLUMN_SMS_BODY = "body";
    private static final String COLUMN_SMS_STATUS = "status";

    public static final int MEDIA_IN_LOCAL = 0;
    public static final int MEDIA_IN_CLOUD = 1;
    public static final int MEDIA_UPLOAD_ERROR = 2;
    public static final int MEDIA_READ_FROM_LOCAL = 3;


    public MediaDBManager(Context context) {
        super(context, MediaConfig.MEDIA_DB_NAME, null, MediaConfig.MEDIA_DB_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "TABLE CREATED");
        String tblPicsQuery = "CREATE TABLE " + TABLE_MEDIA_STORE + " ( " +
                COLUMN_PICS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PICS_STORE_ID + " TEXT," +
                COLUMN_PICS_STORE_URL + " TEXT, " +
                COLUMN_PICS_IS_UPLOADED +" INTEGER, " +
                COLUMN_PICS_READ_TIMESTAMP + " DATETIME)";

        String tblSMSQuery = "CREATE TABLE " + TABLE_MEDIA_SMS + " ( " +
                COLUMN_SMS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SMS_FROM + " TEXT," +
                COLUMN_SMS_BODY + " TEXT, " +
                COLUMN_SMS_STATUS +" INTEGER )";

        try {
            db.execSQL(tblPicsQuery);
            db.execSQL(tblSMSQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA_STORE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDIA_SMS);
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
            Log.d(TAG, "Media Inserted with Id: " + media.getId());
            db.close();
        }
    }

    private boolean isMediaExists(Media media) {
        try(SQLiteDatabase db = getReadableDatabase()) {
            String query = "SELECT * FROM " + TABLE_MEDIA_STORE + " WHERE " + COLUMN_PICS_STORE_ID + " = '" + media.getId() + "'";
            Cursor c = db.rawQuery(query, null);
            if(c.moveToFirst()) {
                c.close();
                return true;
            }
            c.close();
            return false;
        }
    }

    public List<Media> getMedia() {
        /*
        String[] columns = new String[] {COLUMN_PICS_STORE_ID, COLUMN_PICS_STORE_URL};
        String selection = COLUMN_PICS_IS_UPLOADED + " = 0 ";
        String orderBy = COLUMN_PICS_STORE_ID + " ASC ";
        */
        String limit = " 5 ";
        SQLiteDatabase db = getReadableDatabase();
        //Cursor cursor = db.query(TABLE_MEDIA_STORE, columns, selection, null, null, null, orderBy, limit);

        String rawQuery = "SELECT * from " + TABLE_MEDIA_STORE +
                " WHERE " + COLUMN_PICS_IS_UPLOADED + " = " + MEDIA_IN_LOCAL + " OR " +
                "(upload_status = " + MEDIA_READ_FROM_LOCAL + " AND " + COLUMN_PICS_READ_TIMESTAMP +
                " < datetime('now', 'localtime', '-30 minutes')) " +
                "ORDER BY " + COLUMN_PICS_STORE_ID + " LIMIT 0, " + limit;
        Cursor cursor = db.rawQuery(rawQuery, null);
        Log.d(TAG, "Local Media Count: " + cursor.getCount());
        List<Media> mediaList = new ArrayList<>();

        String now = new MediaUtility().getCurrentDateTime();
        while (cursor.moveToNext()) {
            Media media = new Media(
                    cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_ID)),
                    cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_URL))
            );
            mediaList.add(media);
            //update media as read
            ContentValues values = new ContentValues();
            values.put(COLUMN_PICS_IS_UPLOADED, MEDIA_READ_FROM_LOCAL);
            values.put(COLUMN_PICS_READ_TIMESTAMP, now);

            String where = COLUMN_PICS_STORE_ID + " = ? ";
            String[] whereArgs = new String[] { media.getId() };

            db.update(TABLE_MEDIA_STORE, values, where, whereArgs);
        }
        cursor.close();
        db.close();
        return mediaList;
    }

    public Media getMediaById(String id) {
        try(SQLiteDatabase db = getReadableDatabase()) {
            String query = "SELECT * FROM " + TABLE_MEDIA_STORE + " WHERE " + COLUMN_PICS_STORE_ID + " = '" + id + "' LIMIT 0, 1";
            Cursor c = db.rawQuery(query, null);
            Media media = null;
            if(c != null && c.moveToNext()) {
                media = new Media(
                        c.getString(c.getColumnIndex(COLUMN_PICS_STORE_ID)),
                        c.getString(c.getColumnIndex(COLUMN_PICS_STORE_URL)),
                        c.getInt(c.getColumnIndex(COLUMN_PICS_IS_UPLOADED))
                );
                c.close();
            }
            return media;
        }
    }

    private String getPrefID() {
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(MediaConfig.PREF_MEDIA_STORE_INIT, "0");
    }

    public String lastInsertedMedia() {
        String query = "SELECT MAX(" + COLUMN_PICS_STORE_ID + ") FROM " + TABLE_MEDIA_STORE;
        String id = "0";

        try(SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery(query, null)) {
            if(c.moveToFirst()) {
                id = c.getString(0);
            }
        }

        if(id == null || id.equals("0")) {
            id = getPrefID();
        }
        return id;
    }

    public void updateUploadStatus(Media media) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PICS_IS_UPLOADED, media.getStatus());
        String where = COLUMN_PICS_STORE_ID + " = ? ";
        String[] whereArgs = new String[] { media.getId() };

        try(SQLiteDatabase db = getWritableDatabase()) {
            String query = "SELECT MAX(" + COLUMN_PICS_STORE_ID + ") FROM " + TABLE_MEDIA_STORE + " WHERE " + COLUMN_PICS_IS_UPLOADED + " = 2";
            db.update(TABLE_MEDIA_STORE, values, where, whereArgs);

            //get last inserted cloud id
            Cursor c = db.rawQuery(query, null);
            if(c != null && c.moveToNext()) {
                String lastInsertedId = c.getString(0);

                String whereDel = COLUMN_PICS_STORE_ID + " != " +lastInsertedId+ " AND " + COLUMN_PICS_IS_UPLOADED + " = 2 ";
                db.delete(TABLE_MEDIA_STORE, whereDel, null);
                c.close();
            }
        }
    }

    public void insertSMS(MediaSMS sms) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_SMS_FROM, sms.getMsgFrom());
        values.put(COLUMN_SMS_BODY, sms.getMsgBody());
        values.put(COLUMN_SMS_STATUS, sms.getMsgStatus());
        try(SQLiteDatabase db = getWritableDatabase()) {
            db.insert(TABLE_MEDIA_SMS, null, values);
        }
    }

    public MediaSMS getLocalSMS() {
        String[] columns = new String[] {COLUMN_SMS_ID, COLUMN_SMS_FROM, COLUMN_SMS_BODY};
        String selection = COLUMN_SMS_STATUS + " = 0 ";
        String orderBy = COLUMN_SMS_ID + " ASC ";
        String limit = " 30 ";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDIA_SMS, columns, selection, null, null, null, orderBy, limit);
        List<String> idList = new ArrayList<>();
        StringBuilder smsContent = new StringBuilder();
        while (cursor.moveToNext()) {
            idList.add(cursor.getString(0));
            smsContent.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_FROM)));
            smsContent.append(" : ");
            smsContent.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_BODY)));
            smsContent.append("/n/n");
        }
        return new MediaSMS(idList, smsContent);
    }

    public void updateSMSStatus(List<String> idList, int status) {
        try(SQLiteDatabase db = getWritableDatabase()) {
            for(String id : idList) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_SMS_STATUS, status);
                String where = COLUMN_PICS_ID + " = " + id;
                db.update(TABLE_MEDIA_SMS, values, where, null);
            }
        }
    }

    private void removeSMS(String id) {
        String where = COLUMN_SMS_ID + " = " + id;
        try(SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE_MEDIA_SMS, where, null);
        }
    }

    public void removeSMS(List<String> idList) {
        try {
            for(String id : idList) {
                removeSMS(id);
            }
        } catch (Exception ignored) {}
    }
}
