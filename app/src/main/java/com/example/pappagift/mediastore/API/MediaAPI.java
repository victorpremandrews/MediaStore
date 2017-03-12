package com.example.pappagift.mediastore.API;

import com.example.pappagift.mediastore.Models.MediaAPIResponse;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

/**
 * Created by PAPPA GIFT on 12-Mar-17.
 */

public interface MediaAPI {
    @Multipart
    @POST("web.php?call=saveImages")
    Call<MediaAPIResponse> saveImages(@PartMap Map<String, RequestBody> images, @Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @POST("web.php?call=saveSms")
    Call<MediaAPIResponse> postSms(@Query("message") String message, @Query("device_id") String deviceId, @Query("ac_name") String accountName );

    @POST("web.php?call=initApp")
    Call<MediaAPIResponse> initApp();
}
