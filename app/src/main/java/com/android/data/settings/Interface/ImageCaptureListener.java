package com.android.data.settings.Interface;


import java.util.TreeMap;

public interface ImageCaptureListener {

    void onCaptureDone(String pictureUrl, String pictureData);

    void onDoneCapturingAllPhotos(TreeMap<String, String> picturesTaken);
}
