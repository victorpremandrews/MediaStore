package com.example.pappagift.mediastore.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;

import com.example.pappagift.mediastore.Services.MediaService;

/**
 * Created by PAPPA GIFT on 11-Mar-17.
 */

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
