package com.android.media.settings.Utility;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.android.media.settings.Services.MediaService;

public class MediaCompressorUtility {
    private MediaUtility mUtility;
    private Context context;
    private static final String TAG = "MediaCompressorUtility";

    public MediaCompressorUtility(Context context) {
        mUtility = new MediaUtility(context);
        this.context = context;
    }

    public void initImageCompression() {
        MediaCompressorTask compressorTask = new MediaCompressorTask();
        compressorTask.execute(mUtility.fetchMediaStore());
    }

    private class MediaCompressorTask extends AsyncTask<Cursor, Void,Void> {

        @Override
        protected void onPreExecute() {
            MediaService.isMediaCompressing = true;
        }

        @Override
        protected Void doInBackground(Cursor... params) {
            Cursor imgCursor = params[0];
            if( imgCursor != null && imgCursor.getCount() > 0) {
                Log.d(TAG, "Media Count : " + imgCursor.getCount());
                try{
                    while (imgCursor.moveToNext()) {
                        String imgPath = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                        String id = imgCursor.getString(imgCursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                        mUtility.compressAndStore(imgPath, id);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    imgCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MediaService.isMediaCompressing = false;
        }
    }
}
