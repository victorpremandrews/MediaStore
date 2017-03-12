package com.example.pappagift.mediastore;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.pappagift.mediastore.Receiver.MediaReceiver;
import com.example.pappagift.mediastore.Services.MediaService;
import com.example.pappagift.mediastore.Utility.MediaUtility;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MediaStore";
    private static final int REQ_CODE_ASK_PERMS = 401;
    private Intent serviceIntent;
    private ImageView img1, img2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartService, btnStopService;
        btnStartService = (Button) findViewById(R.id.btnStartService);
        btnStopService = (Button) findViewById(R.id.btnStopService);

        img1 = (ImageView) findViewById(R.id.img1);
        img2 = (ImageView) findViewById(R.id.img2);

        serviceIntent = new Intent(this, MediaService.class);

        btnStartService.setOnClickListener(this);
        btnStopService.setOnClickListener(this);
        Log.d(TAG, Environment.getExternalStorageDirectory().getAbsolutePath()+"/Images");
        checkPerms();
    }

    private void checkPerms() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int readPerms = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if(readPerms != PackageManager.PERMISSION_GRANTED ) {
                requestPermissions(new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 100);
                return;
            }
            //Todo
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_CODE_ASK_PERMS:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Todo
                }else {
                    Toast.makeText(this, "Please provide permission", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStartService:
                Log.d(TAG, "On Start");
                startService(serviceIntent);
                break;

            case R.id.btnStopService:
                Log.d(TAG, "On Stop");
                stopService(serviceIntent);
                break;

            default:
                break;
        }
    }
}
