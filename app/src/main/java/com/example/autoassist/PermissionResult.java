package com.example.autoassist;

import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class PermissionResult implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "PERMISSIONS";

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permissions requested with code " + requestCode + ":");
        for (int i = 0; i < permissions.length; i++) {
            Log.i(TAG, permissions[i] + ": " + (grantResults[i] == PackageManager.PERMISSION_GRANTED));
        }
    }
}
