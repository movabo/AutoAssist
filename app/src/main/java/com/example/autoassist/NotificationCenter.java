package com.example.autoassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDate;
import java.time.LocalTime;

import static android.app.PendingIntent.getActivity;
import static android.provider.Settings.System.getString;

public class NotificationCenter {

    protected static final int RUNNING_NOTIFICATION_ID = 0;
    protected static final int CINEMA_NOTIFICATION_ID = 1;
    protected static final int SHOP_NOTIFICATION_ID = 2;

    private static final String CHANNEL_ID = "notification_channel";
    private NotificationManagerCompat notificationManager;

    private int currentNotification = 5;
    private Context settingsContext;

    /**
     * Instanciate the notification manager and everything that's needed beforehand.
     */
    public NotificationCenter() {
        settingsContext = SettingsActivity.getContext();
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = settingsContext.getString(R.string.channel_name);
            String description = settingsContext.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = settingsContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager = NotificationManagerCompat.from(settingsContext);
    }

    /**
     * Notify the user that he started / ended running and that thus the volume is raised / lowered.
     * @param startedRunning Whether he started running (true) or ended (false).
     */
    protected void notifyRunning(boolean startedRunning) {
        int title_id = startedRunning ? R.string.raising_volume : R.string.lowering_volume;
        int content_id = startedRunning ? R.string.raising_volume_because_running : R.string.lowering_volume_because_running;

        final Notification notification = new NotificationCompat.Builder(settingsContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_running)
                .setContentTitle(settingsContext.getString(title_id))
                .setContentText(settingsContext.getString(content_id))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(RUNNING_NOTIFICATION_ID, notification);
    }

    /**
     * Notify that the user is near a shop and wanted to be notified about it.
     */
    protected void notifyShop() {
        final Notification notification = new NotificationCompat.Builder(settingsContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_shop)
                .setContentTitle(settingsContext.getString(R.string.near_shop))
                .setContentText(settingsContext.getString(R.string.near_shop_reminder))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(SHOP_NOTIFICATION_ID, notification);
    }

    /**
     * Notify that the user is near a cinema and still and thus the phone is switched to silent mode.
     */
    protected void notifyCinema() {
        final Notification notification = new NotificationCompat.Builder(settingsContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_cinema)
                .setContentTitle(settingsContext.getString(R.string.in_cinema))
                .setContentText(settingsContext.getString(R.string.in_cinema_silence))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(CINEMA_NOTIFICATION_ID, notification);
    }

    /**
     * Info (i.e. debugging) notification.
     */
    protected void info(String text) {
        final Notification notification = new NotificationCompat.Builder(settingsContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_running)
                .setContentTitle(LocalDate.now() + " " + LocalTime.now())
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(currentNotification, notification);
        currentNotification++;
    }
}
