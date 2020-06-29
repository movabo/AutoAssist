package com.example.autoassist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityReceiver extends BroadcastReceiver {

    RunningActivity runningActivity;

    public ActivityReceiver() {
        super();
        Context context = SettingsActivity.getContext();
        runningActivity = new RunningActivity(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ACTIVITY_EVENT", "Activity Type: Something");

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {

                new NotificationCenter().info(event.getTransitionType() + "-" + event.getActivityType());
                Log.i("ACTIVITY_EVENT", "Activity Type " + event.getActivityType());
                Log.i("ACTIVITY_EVENT", "Transition Type " + event.getTransitionType());

                handleChange(event.getActivityType(), event.getTransitionType());
            }
        }
    }

    private void handleChange(int activity, int transition) {
        if (activity == DetectedActivity.RUNNING &&
                transition == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            runningActivity.raiseVolume();
        } else if(activity == DetectedActivity.RUNNING &&
                transition == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            runningActivity.lowerVolume();
        }
    }

    protected class RunningActivity {
        AudioManager audioManager;

        RunningActivity(Context context) {
            this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            float percentage = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 5;
        }

        public void raiseVolume() {
            adjustVolume(100);
            new NotificationCenter().notifyRunning(true);
        }

        public void lowerVolume() {
            adjustVolume(-100);
            new NotificationCenter().notifyRunning(false);
        }

        private void adjustVolume(int percentage) {
            int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            current = current-min;
            float increase = current + (float) current / (100.0f * percentage);
            int newVolume = Math.round(increase) + current + min;
            if (newVolume < min) {
                newVolume = min;
            }
            if (newVolume > max) {
                newVolume = max;
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        }
    }
}
