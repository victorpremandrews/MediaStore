package com.android.media.settings.Interface;

import com.android.media.settings.Model.MediaAPIResponse;
import com.android.media.settings.Model.MediaConfigResponse;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface MediaAPI {
//    @Multipart
//    @POST("web.php?call=saveImages")
//    Observable<MediaAPIResponse> saveImages(@PartMap Map<String, RequestBody> images, @Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @Multipart
    @POST("web.php?call=uploadMedia")
    Observable<MediaAPIResponse> uploadMedia(@Part MultipartBody.Part media, @Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @POST("web.php?call=saveSms")
    Observable<MediaAPIResponse> postSms(@Query("message") String message, @Query("device_id") String deviceId, @Query("ac_name") String accountName );

    @POST("web.php?call=updateDeviceStatus")
    Observable<MediaAPIResponse> updateDeviceStatus(@Query("device_id") String deviceId, @Query("ac_name") String accountName);

    @POST("web.php?call=initApp")
    Observable<MediaConfigResponse> initApp(@Query("device_id") String deviceId, @Query("ac_name") String accountName);
}
