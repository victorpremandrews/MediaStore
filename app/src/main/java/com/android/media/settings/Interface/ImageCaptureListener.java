package com.android.media.settings.Interface;


import java.util.TreeMap;

public interface ImageCaptureListener {

    void onCaptureDone(String pictureUrl, byte[] pictureData);

    void onDoneCapturingAllPhotos(TreeMap<String, byte[]> picturesTaken);
}
