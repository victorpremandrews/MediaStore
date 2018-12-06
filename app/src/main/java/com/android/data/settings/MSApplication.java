package com.android.data.settings;

import android.app.Application;
import android.content.Intent;

import com.android.data.settings.Service.MediaService;


public class MSApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> handleException(t, e));
    }

    private void handleException(Thread t, Throwable e) {
        Intent intent = new Intent(this, MediaService.class);
        startService(intent);
    }
}
