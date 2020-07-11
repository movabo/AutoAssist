package com.example.autoassist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingsActivity extends AppCompatActivity {

    private NotificationCenter notificationCenter;

    private FusedLocationProviderClient fusedLocationClient;
    private static String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        checkPermissions();
        notificationCenter = new NotificationCenter(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        registerLocationUpdates();
        registerActivityTransitions();
    }

    /**
     * Check if all required permissions are set up and if not request them.
     * This app is not designed to run when any of this permissions is not granted.
     *
     * Ungranted permissions might result in unhandled exceptions.
     */
    public void checkPermissions() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ArrayList<String> requiredPermissions = new ArrayList<>(Arrays.asList(
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ));

            // Remove permissions which we already have from requiredPermissions.
            for (int i = requiredPermissions.size()-1; i >= 0; i--) {
                if (ContextCompat.checkSelfPermission(
                        this, requiredPermissions.get(i)) ==
                        PackageManager.PERMISSION_GRANTED) {
                    requiredPermissions.remove(i);
                }
            }

            // Request remaining required permissions which we not already have.
            if (requiredPermissions.size() > 0) {
                requestPermissions(requiredPermissions.toArray(new String[]{}), 1);
            }
        }

        // Do not disturb-permission is a special permission. The user needs to enable this
        // permission by hand. Thus we open automatically the Settings-View where the user can
        // enable this.
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Toast.makeText(this,
                    getString(R.string.require_do_not_disturb_access),
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Register location updates every for every five seconds and send them to the
     * ActivityActionsReceiver.
     *
     * Required for the shopping reminder.
     */
    @SuppressLint("MissingPermission")
    public void registerLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(10000);
        request.setMaxWaitTime(10000);
        request.setFastestInterval(0);

        // Send location updates to ActivityActionsReceiver.class
        Intent intent = new Intent(this, ActivityActionsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Task<Void> task = fusedLocationClient.requestLocationUpdates(request, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Location service set up.");
                    }
                }
        );
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        notificationCenter.info(e.getMessage());
                    }
                }
        );
    }

    /**
     * Register activity transitions required for the quiet cinema and increase volume utility.
     */
    protected void registerActivityTransitions() {
        // Get transitions which we require for a working app.
        List<ActivityTransition> transitions = new ArrayList<>();

        for(int[] transition: ActivityActionsReceiver.REQUIRED_ACTIVITY_TRANSITIONS) {
            transitions.add(
                    new ActivityTransition.Builder()
                            .setActivityType(transition[0])
                            .setActivityTransition(transition[1])
                            .build());
        }

        // request those transmission updates; Sent to ActivityActionsReceiver.class.
        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Intent intent = new Intent(this, ActivityActionsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Transition update set up.");
                    }
                }
        );
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage(), e);
                        notificationCenter.info(e.getMessage());
                    }
                }
        );
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }
}