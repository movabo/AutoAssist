package com.example.autoassist;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;


public class SettingsActivity extends AppCompatActivity {

    public static final String CINEMA = "quiet_cinema";
    public static final String RUNNING = "adjust_running";
    public static final String SHOP = "shopping_reminder";

    private static SettingsActivity mContext;

    private NotificationCenter notificationCenter;
    private SettingsFragment settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        settings = new SettingsFragment();
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settings)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        checkPermissions();
        notificationCenter = new NotificationCenter();
        // notifyCinema();
        // notifyRunning(true);
        // notifyShop();
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement stackTraceElement : stack) {
            Log.i("STACK", stackTraceElement.toString());
        }
        registerRunningTransitions();
    }

    public boolean runningSettingIsEnabled() {
        return settings.findPreference(RUNNING).isEnabled();
    }

    public boolean cinemaSettingIsEnabled() {
        return settings.findPreference(CINEMA).isEnabled();
    }

    public boolean shopSettingIsEnabled() {
        return settings.findPreference(SHOP).isEnabled();
    }

    public void checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION) !=
                 PackageManager.PERMISSION_GRANTED) {
            // You can directly ask for the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissions(
                        new String[] { Manifest.permission.ACTIVITY_RECOGNITION },
                        1
                );
            }
        }
    }

    public static SettingsActivity getContext() {
        return mContext;
    }

    protected void registerRunningTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();

        for(int[] transition: ActivityActionsReceiver.REQUIRED_ACTIVITY_TRANSITIONS) {
            transitions.add(
                    new ActivityTransition.Builder()
                            .setActivityType(transition[0])
                            .setActivityTransition(transition[1])
                            .build());
        }

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        Intent intent = new Intent(this, ActivityActionsReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        notificationCenter.info("Transition update set up.");
                    }
                }
        );
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("TRANSITION", e.getMessage());
                        e.printStackTrace();
                        notificationCenter.info(e.getMessage());
                    }
                }
        );
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference cinema  = this.findPreference(CINEMA);
            Preference running = this.findPreference(RUNNING);
            Preference shop    = this.findPreference(SHOP);
        }
    }
}