package com.example.pappagift.mediastore.Utility;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.pappagift.mediastore.API.MediaAPI;
import com.example.pappagift.mediastore.MediaConfig;
import com.example.pappagift.mediastore.MediaDBManager;
import com.example.pappagift.mediastore.Models.Media;
import com.example.pappagift.mediastore.Models.MediaAPIResponse;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MediaUploadUtility {
    private static final String TAG = "MediaUploadService";
    private Context context;
    private MediaDBManager dbManager;
    private final MediaType TYPE_IMAGE = MediaType.parse("image/*");
    private MediaUtility mUtility;
    private MediaConfig mConfig;

    public MediaUploadUtility(Context context) {
        this.context = context;
        this.dbManager = new MediaDBManager(context);
        this.mUtility = new MediaUtility(context);
        this.mConfig = new MediaConfig(context);
    }

    public void initUploadServices() {
        if(mUtility.networkConnected()) {
            Cursor cursor = dbManager.getLocalMedia();
            if(cursor != null && cursor.getCount() > 0) {
                processMediaCursor(cursor);
            }
        }
    }

    private void processMediaCursor(Cursor cursor) {
        try {
            MediaAPI api = mUtility.initRetroService();
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_ID));
                String path = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_URL));
                File file = new File(path);

                Log.d(TAG, "Trying to upload media id : " + id);

                if(file != null && file.exists()) {
                    RequestBody imgBody = RequestBody.create(TYPE_IMAGE, file);
                    MultipartBody.Part part = MultipartBody.Part.createFormData(mConfig.getIMG_UPLOAD_NAME(), id, imgBody);
                    Observable<MediaAPIResponse> observable = api.uploadMedia(part, mUtility.getDeviceId(), mUtility.getUsername());
                    observable.subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    private Observer<MediaAPIResponse> observer = new Observer<MediaAPIResponse>() {
        @Override
        public void onSubscribe(Disposable d) {
            Log.d(TAG, "On Subscribe");
        }

        @Override
        public void onNext(MediaAPIResponse response) {
            try {
                if(response != null) {
                    if(response.getStatus() == "1") {
                        String id = response.getData();
                        Media media = dbManager.getMedia(id);
                        media.setStatus(2);
                        dbManager.updateUploadStatus(media);
                        boolean isDeleted = new File(media.getPath()).delete();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onComplete() {
            Log.d(TAG, "Complete");
        }
    };
}