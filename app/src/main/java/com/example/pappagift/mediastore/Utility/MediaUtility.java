package com.example.pappagift.mediastore.Utility;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.example.pappagift.mediastore.MediaConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class MediaUtility {
    private static final String TAG = "Media Utility";
    private Context context;

    public MediaUtility(Context context) {
        this.context = context;
    }

    public Cursor fetchMediaStore() {
        Uri extMediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String projection[] = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_ADDED
        };
        return context.getContentResolver().query(
                extMediaUri,
                projection,
                null,
                null,
                MediaStore.Images.ImageColumns._ID + " DESC LIMIT 15"
        );
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

    public void compressAndStore(String uri){
        Observable.just(compressImage(uri))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
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
