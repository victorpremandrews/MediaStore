package com.android.media.settings.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.media.settings.Services.MediaService;

public class MediaReceiver extends BroadcastReceiver {
    private static final String TAG = "Media Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d(TAG, "Boot Completed, Starting Media Service from Media Receiver...");
            Intent mediaService = new Intent(context, MediaService.class);
            context.startService(mediaService);
        }
    }

}
