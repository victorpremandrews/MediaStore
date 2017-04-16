package com.example.pappagift.mediastore.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.pappagift.mediastore.MediaDBManager;
import com.example.pappagift.mediastore.Models.MediaSMS;

public class MediaSMSReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                    insertSMS(smsMessage.getOriginatingAddress(), smsMessage.getMessageBody(), 0, context);
                }
            } else {
                Bundle data = intent.getExtras();
                readSMS(data, context);
            }
        }
    }

    private void readSMS(Bundle bundle, Context context) {
        SmsMessage[] msgs = null;
        if (bundle != null){
            try{
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for(int i=0; i<msgs.length; i++){
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                    insertSMS(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody(), 0, context);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void insertSMS(String from, String body, int status, Context context) {
        MediaDBManager dbManager = new MediaDBManager(context);
        dbManager.insertSMS(new MediaSMS(body, from, status));
    }
}
