package com.android.media.settings.Utility;

import android.content.Context;

import com.android.media.settings.Interface.MediaAPI;
import com.android.media.settings.Model.MediaAPIResponse;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class StatusUpdaterUtility {
    private MediaUtility mUtility;

    public StatusUpdaterUtility(Context context) {
        mUtility = new MediaUtility(context);
    }

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

    public void updateStatus() {
        MediaAPI api = mUtility.initRetroService();
        Observable<MediaAPIResponse> observable = api.updateDeviceStatus(mUtility.getDeviceId(), mUtility.getUsername());
        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }
}
