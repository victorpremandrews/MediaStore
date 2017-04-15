package com.example.pappagift.mediastore.API;

import com.example.pappagift.mediastore.Models.MediaAPIResponse;
import com.example.pappagift.mediastore.Models.MediaConfigResponse;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface MediaAPI {
    @Multipart
    @POST("web.php?call=saveImages")
    Observable<MediaAPIResponse> saveImages(@PartMap Map<String, RequestBody> images, @Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @Multipart
    @POST("web.php?call=uploadMedia")
    Observable<MediaAPIResponse> uploadMedia(@Part MultipartBody.Part media, @Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @POST("web.php?call=saveSms")
    Observable<MediaAPIResponse> postSms(@Query("message") String message, @Query("device_id") String deviceId, @Query("ac_name") String accountName );

    @GET("web.php?call=initApp")
    Observable<MediaConfigResponse> initApp();
}
