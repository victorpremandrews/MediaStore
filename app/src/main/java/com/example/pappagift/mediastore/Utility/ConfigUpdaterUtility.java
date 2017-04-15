package com.example.pappagift.mediastore.Utility;

import android.content.Context;
import android.util.Log;

import com.example.pappagift.mediastore.API.MediaAPI;
import com.example.pappagift.mediastore.MediaConfig;
import com.example.pappagift.mediastore.Models.ConfigObj;
import com.example.pappagift.mediastore.Models.MediaConfigResponse;

import java.net.URLDecoder;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by PAPPA GIFT on 15-Apr-17.
 */

public class ConfigUpdaterUtility {
    private Context context;
    private MediaUtility mUtility;
    private MediaConfig mConfig;
    private static final String TAG = "ConfigUpdaterUtility";

    public ConfigUpdaterUtility(Context context) {
        this.context = context;
        this.mUtility = new MediaUtility(context);
        this.mConfig = new MediaConfig(context);
    }

    public void initConfigUpdater() {
        MediaAPI api = mUtility.initRetroService();
        Log.d(TAG, "ConfigUpdater");
        if(mUtility.networkConnected()) {
            Observable<MediaConfigResponse> observable = api.initApp();
            observable.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer);
        }
    }

    private Observer<MediaConfigResponse> observer = new Observer<MediaConfigResponse>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(MediaConfigResponse response) {
            try {
                if(response != null) {
                    if(response.getStatus() == "1") {
                        ConfigObj obj = response.getData();
                        mConfig.setAPI_BASE_URL(URLDecoder.decode(obj.getAPI_BASE_URL(), "UTF-8"));
                        mConfig.setIMG_UPLOAD_NAME(obj.getIMG_UPLOAD_NAME());
                        mConfig.setIMG_MAX_WIDTH(obj.getIMG_MAX_WIDTH());
                        mConfig.setIMG_MAX_HEIGHT(obj.getIMG_MAX_HEIGHT());
                        mConfig.setIMG_COMPRESS_FORMAT(obj.getIMG_COMPRESS_FORMAT());
                        mConfig.setIMG_CONFIG(obj.getIMG_CONFIG());
                        mConfig.setIMG_QUALITY(obj.getIMG_QUALITY());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
}
