package com.example.autoassist;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Handle the background activity like changing the activity or location.
 */
public class ActivityActionsReceiver extends BroadcastReceiver {

    private static final String TAG = ActivityActionsReceiver.class.getSimpleName();

    /**
     * Required activity transitions for the class to work correctly.
     */
    public static int[][] REQUIRED_ACTIVITY_TRANSITIONS = {
            {DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
    };

    /**
     * All possible activity transitions (for debugging purposes).
     */
    public static int[][] POSSIBLE_ACTIVITY_TRANSITIONS = {
            {DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
            {DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER},
            {DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT},
    };

    /**
     * Get the name of an activity from its value.
     */
    public static String[] ACTIVITY_TO_STRING = {
            "IN_VEHICLE",
            "ON_BICYCLE",
            "ON_FOOT",
            "STILL",
            "UNKNOWN",
            "TILTING",
            "UNKNOWN (index 6)",
            "WALKING",
            "RUNNING"
    };

    /**
     * Get the name of a transition from its value.
     */
    public static String[] TRANSITION_TO_STRING = {
            "ACTIVITY_TRANSITION_ENTER",
            "ACTIVITY_TRANSITION_EXIT"
    };

    /**
     * Get the short name of a transition from its value.
     */
    public static String[] TRANSITION_TO_SHORT_STRING = {
            "ENTER",
            "EXIT"
    };

    private RunningActivity runningActivity;
    private Context context;
    private NotificationCenter notificationCenter;
    private boolean shopSettingIsEnabled = true;
    private boolean runningSettingIsEnabled = true;
    private boolean cinemaSettingIsEnabled = true;

    public static final String CINEMA = "quiet_cinema";
    public static final String RUNNING = "adjust_running";
    public static final String SHOP = "shopping_reminder";

    public ActivityActionsReceiver() {
        super();
        runningActivity = new RunningActivity();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        if (notificationCenter == null) {
            notificationCenter = new NotificationCenter(context);
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        cinemaSettingIsEnabled = sharedPref.getBoolean(CINEMA, true);
        runningSettingIsEnabled = sharedPref.getBoolean(RUNNING, true);
        shopSettingIsEnabled = sharedPref.getBoolean(SHOP, true);


        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Log.i(TAG, "Intent -> " + key + ": " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
            }
        }

        if (LocationResult.hasResult(intent)) {
            LocationResult locationResult = LocationResult.extractResult(intent);
            Location location = locationResult.getLastLocation();
            handleLocationChange(location);
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                int activity = event.getActivityType();
                int transition = event.getTransitionType();
                Log.i(TAG, "ActivityTransition: " + ACTIVITY_TO_STRING[activity] + " "
                        + TRANSITION_TO_SHORT_STRING[transition]);
                handleTransition(activity, transition);
            }
        }
    }

    /**
     * Handle a location change / update.
     * @param location The new location.
     */
    private void handleLocationChange(Location location) {
        if (shopSettingIsEnabled) {
            if (MapLocations.isCloseToStore(location)) {
                notificationCenter.notifyShop();
            }
        }
    }

    /**
     * Handle an activity transition.
     * @param activity The activity the user started / ended.
     * @param transition Whether the user started or ended the activity.
     */
    private void handleTransition(int activity, int transition) {
        if (runningSettingIsEnabled) {
            runningActivity.setContext(context);
            runningActivity.setNotificationCenter(notificationCenter);
            if (activity == DetectedActivity.RUNNING &&
                    transition == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                runningActivity.raiseVolume();
            } else if (activity == DetectedActivity.RUNNING &&
                    transition == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                runningActivity.lowerVolume();
            }
        }
        if (cinemaSettingIsEnabled) {
            if (activity == DetectedActivity.STILL &&
                    transition == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                triggerCinemaActivity();
            }
        }
    }

    /**
     * Trigger the cinema activity.
     * Checks if close to a cinema if that is the case, notifies the user or enables do not
     * disturb (if permission granted).
     */
    @SuppressLint("MissingPermission") // Should be checked beforehand!
    public void triggerCinemaActivity() {
        Task<Location> location = LocationServices.getFusedLocationProviderClient(context)
                .getLastLocation();
        final NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(NOTIFICATION_SERVICE);
        location.addOnSuccessListener(
                new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            if (MapLocations.isCloseToCinema(location)) {
                                if (notificationManager.isNotificationPolicyAccessGranted()) {
                                    notificationManager.setInterruptionFilter(NotificationManager
                                            .INTERRUPTION_FILTER_NONE);
                                    notificationCenter.notifyCinema();
                                } else {
                                    notificationCenter.info("It seems like you're in a cinema "
                                            + "and should turn on 'do not disturb' (permission for "
                                            + "automatically setting not granted).");
                                }
                            }
                        }
                    }
                }
        );
        location.addOnFailureListener(
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
     * Handling of the running activity apart from the activity transition.
     */
    protected static class RunningActivity {
        AudioManager audioManager;
        NotificationCenter notificationCenter;
        public static int RAISE_PERCENTAGE = 20;

        public void setNotificationCenter(NotificationCenter notificationCenter) {
            this.notificationCenter = notificationCenter;
        }

        public void setContext(Context context) {
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        /**
         * Raise the volume of the music stream and show a notification.
         */
        public void raiseVolume() {
            adjustVolume(RAISE_PERCENTAGE);
            notificationCenter.notifyRunning(true);
        }

        /**
         * Lower the volume of the music stream and show a notification.
         */
        public void lowerVolume() {
            adjustVolume(-RAISE_PERCENTAGE);
            notificationCenter.notifyRunning(false);
        }

        /**
         * Adjust the music stream volume.
         *
         * @param percentage The amount to raise / lower the volume. Negative value to lower.
         */
        private void adjustVolume(int percentage) {
            int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            current = current - min;
            int newVolume;
            // To reduce to the old volume, we need to do the calculation as follow:
            // increase: current + current * percentage%
            // decrease: current / (percentage + 100)%
            if (percentage < 0) {
                newVolume = Math.round(current / (1 + percentage / 100.0f));
            } else {
                float increase = percentage / 100.0f * current;
                newVolume = Math.round(increase) + current + min;
            }
            if (newVolume < min) {
                newVolume = min;
            }
            if (newVolume > max) {
                newVolume = max;
            }
            Log.i(TAG, "Changing from " + current + " to " + newVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        }
    }
}
