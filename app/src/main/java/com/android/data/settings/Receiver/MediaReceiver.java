package com.android.data.settings.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.data.settings.Service.MediaService;
import com.android.data.settings.Utility.MediaUtility;

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
