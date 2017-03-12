package com.example.pappagift.mediastore.Utility;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import id.zelory.compressor.Compressor;

/**
 * Created by PAPPA GIFT on 12-Mar-17.
 */

public class MediaUtility {
    private static final String TAG = "Media Utility";

    public MediaUtility() {

    }



    public static Cursor fetchMediaStore(Context context) {
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

    public static Bitmap getBitmapFromUri(Context context, String strUri) {
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

    public static Bitmap compressImage(Context context, String strUri) {
        return Compressor.getDefault(context).compressToBitmap(new File(strUri));
    }

    /**
     * Function to store bitmap to provided path
     * @param image [Image Bitmap]
     * @return file [File of the stored bitmap]
     * */
    public static File storeImage(Context cx, Bitmap image, String id) {
        String path2 = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Images";
        File mediaStorageDir = new File(path2);

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
