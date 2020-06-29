package com.example.autoassist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.util.Pair;

import androidx.preference.Preference;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class ActivityActionsReceiver extends BroadcastReceiver {

    public static int[][] REQUIRED_ACTIVITY_TRANSITIONS = {
            { DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
    };

    public static int[][] POSSIBLE_ACTIVITY_TRANSITIONS = {
            { DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.ON_FOOT, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
            { DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER },
            { DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_EXIT },
    };

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

    public static String[] TRANSITION_TO_STRING = {
            "ACTIVITY_TRANSITION_ENTER",
            "ACTIVITY_TRANSITION_EXIT"
    };

    public static String[] TRANSITION_TO_SHORT_STRING = {
            "ENTER",
            "EXIT"
    };

    RunningActivity runningActivity;
    SettingsActivity settingsContext;

    public ActivityActionsReceiver() {
        super();
        settingsContext = SettingsActivity.getContext();
        runningActivity = new RunningActivity(settingsContext);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ACTIVITY_EVENT", "Activity Type: Something");

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {

                new NotificationCenter().info(
                        ACTIVITY_TO_STRING[event.getActivityType()] + " "
                                + TRANSITION_TO_SHORT_STRING[event.getTransitionType()]);
                Log.i("ACTIVITY_EVENT", "Activity Type " + event.getActivityType());
                Log.i("ACTIVITY_EVENT", "Transition Type " + event.getTransitionType());

                handleChange(event.getActivityType(), event.getTransitionType());
            }
        }
    }

    private void handleChange(int activity, int transition) {
        if (settingsContext.runningSettingIsEnabled()) {
            if (activity == DetectedActivity.RUNNING &&
                    transition == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                runningActivity.raiseVolume();
            } else if (activity == DetectedActivity.RUNNING &&
                    transition == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                runningActivity.lowerVolume();
            }
        }
    }

    protected static class RunningActivity {
        AudioManager audioManager;

        RunningActivity(Context context) {
            //Preference running = context.findPreference(context.RUNNING);
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            float percentage = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 5;
        }

        public void raiseVolume() {
            adjustVolume(20);
            new NotificationCenter().notifyRunning(true);
        }

        public void lowerVolume() {
            adjustVolume(-20);
            new NotificationCenter().notifyRunning(false);
        }

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
            new NotificationCenter().info("Changing from " + current + " to " + newVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        }
    }
}
