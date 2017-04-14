package com.example.pappagift.mediastore.Utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.preference.Preference;
import android.provider.MediaStore;
import android.util.Log;

import com.example.pappagift.mediastore.MediaConfig;
import com.example.pappagift.mediastore.MediaDBManager;
import com.example.pappagift.mediastore.Models.Media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MediaUtility {
    private static final String TAG = "Media Utility";
    private Context context;
    private MediaDBManager dbManager;
    Uri extMediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    String projection[] = {
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_ADDED
    };

    public MediaUtility(Context context) {
        this.context = context;
        this.dbManager = new MediaDBManager(context, MediaConfig.MEDIA_DB_NAME, null, MediaConfig.MEDIA_DB_VERSION);
    }

    public String getLastId() {
        return dbManager.lastInsertedMedia();
    }

    public Cursor fetchMediaStore() {
        String id = this.getLatestMediaId();
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        if(isFirstInstall()) {
            Log.d(TAG, "First Install");
            SharedPreferences.Editor prefEditor = pref.edit();
            prefEditor.putString(MediaConfig.PREF_MEDIA_STORE_INIT, id);
            prefEditor.apply();
        }else {
            id = dbManager.lastInsertedMedia();
            Log.d(TAG, "Fetching Images For: " + id);
        }

        String filterQuery = MediaStore.Images.ImageColumns._ID + " > " + id;
        Log.d(TAG, filterQuery);
        return context.getContentResolver().query(
                extMediaUri,
                projection,
                filterQuery,
                null,
                MediaStore.Images.ImageColumns._ID + " DESC LIMIT 0, 100"
        );
    }

    public boolean isFirstInstall() {
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();

        if(pref.contains(MediaConfig.PREF_MEDIA_STORE_INIT)) return false;
        return true;
    }

    public String getLatestMediaId() {
        String id = "0";
        Cursor c = context.getContentResolver().query(
                extMediaUri,
                projection,
                null,
                null,
                MediaStore.Images.ImageColumns._ID + " DESC LIMIT 0, 1"
        );
        try {
            if(c != null && c.moveToFirst()) {
                id = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns._ID));
            }
        } finally {
            c.close();
        }
        return id;
    }

    public Bitmap getBitmapFromUri(String strUri) {
        Uri uri = Uri.parse("file://" + strUri);
        try {
            Bitmap bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            return bm;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Invalid Bitmap");
        }
        return null;
    }

    public File compressImage(String strUri) {
        return ImageUtility.compressToFile(
                context,
                new File(strUri),
                1024,
                768,
                Bitmap.CompressFormat.JPEG,
                Bitmap.Config.ARGB_8888,
                75,
                MediaConfig.getFileStoragePath(context, true)
        );
    }

    public void compressAndStore(String uri, String id){
        File imgFile = new File(uri);
        if(imgFile.exists()) {
            final Media media = new Media(id,imgFile.getAbsolutePath());
            Observable.just(compressImage(uri))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<File>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(File value) {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onComplete() {
                            dbManager.insertMedia(media);
                        }
                    });
        }
    }


    /**
     * Function to store bitmap to provided path
     * @param image [Image Bitmap]
     * @return file [File of the stored bitmap]
     * */
    public File storeImage(Bitmap image, String id) {
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MediaServer/Images";
        Log.d(TAG, "Storage Path: " + storagePath);
        File mediaStorageDir = new File(storagePath);

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        if( id != null) id = "img_" + Math.random();
        String imgName = id + ".jpg";
        File imgFile = new File(mediaStorageDir + File.separator + imgName);
        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return imgFile;
    }
}
