package com.android.media.settings.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.media.settings.Service.MediaService;
import com.android.media.settings.Utility.MediaUtility;

public class MediaReceiver extends BroadcastReceiver {
    private static final String TAG = "Media Receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        MediaUtility mediaUtility = new MediaUtility(context);
        if(!mediaUtility.isServiceRunning(MediaService.class)) {
            context.startService(new Intent(context, MediaService.class));
        }
    }

}
