package com.example.autoassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.actions.NoteIntents;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

import static android.app.PendingIntent.getActivity;

public class NotificationCenter {

    private static final String TAG = NotificationCenter.class.getSimpleName();
    protected static final int RUNNING_NOTIFICATION_ID = 0;
    protected static final int CINEMA_NOTIFICATION_ID = 1;
    protected static final int SHOP_NOTIFICATION_ID = 2;

    private static final String LAST_SHOP_NOTIFICATION = "last_shop_notification";

    private static final String CHANNEL_ID = NotificationCenter.class.getCanonicalName() +
            "notification_channel";
    private NotificationManagerCompat notificationManager;

    private Context context;

    /**
     * Min. waiting time between notifications for "shop found".
     * 3600000 = 1h
     */
    private static long SHOP_NOTIFICATION_DELAY = 3600000;

    private SharedPreferences sharedPref;
    /**
     * Instanciate the notification manager and everything that's needed beforehand.
     */
    public NotificationCenter(Context context) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = this.context.getString(R.string.channel_name);
            String description = this.context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = this.context
                    .getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager = NotificationManagerCompat.from(this.context);
    }

    /**
     * Notify the user that he started / ended running and that thus the volume is raised / lowered.
     * @param startedRunning Whether he started running (true) or ended (false).
     */
    protected void notifyRunning(boolean startedRunning) {
        int title_id = startedRunning ? R.string.raising_volume : R.string.lowering_volume;
        int content_id = startedRunning ? R.string.raising_volume_because_running :
                R.string.lowering_volume_because_running;

        final Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_running)
                .setContentTitle(context.getString(title_id))
                .setContentText(context.getString(content_id))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(RUNNING_NOTIFICATION_ID, notification);
    }

    /**
     * Notify that the user is near a shop and wanted to be notified about it.
     * Only show this notification at max once every hour.
     */
    protected void notifyShop() {
        long now = System.currentTimeMillis();

        // Only show at max one notification per hour:
        if (sharedPref.getLong(LAST_SHOP_NOTIFICATION, 0) + SHOP_NOTIFICATION_DELAY > now) {
            long wait = (sharedPref.getLong(LAST_SHOP_NOTIFICATION, 0)
                    + SHOP_NOTIFICATION_DELAY - now) / 60000;
            Log.i(TAG, "Waiting at least " + wait
                    + " more minutes for next shop notification.");
            return;
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(LAST_SHOP_NOTIFICATION, now);
        editor.apply();

        Intent intent = new Intent(NoteIntents.ACTION_APPEND_NOTE);
        intent.putExtra(NoteIntents.EXTRA_TEXT, "");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, intent, 0);


        NotificationCompat.Builder notification = new NotificationCompat.Builder(context,
                CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_shop)
                .setContentTitle(context.getString(R.string.near_shop))
                .setContentText(context.getString(R.string.near_shop_reminder))
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Show button if notes intent (i.e. a registered default notes app) is available.
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            notification.addAction(R.drawable.notification_icon_shop, context.getString(R.string.open_shopping_list),
                    pendingIntent);
        }
        notificationManager.notify(SHOP_NOTIFICATION_ID, notification.build());
    }

    /**
     * Notify that the user is near a cinema and still and thus the phone is switched to silent
     * mode.
     */
    protected void notifyCinema() {
        final Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_cinema)
                .setContentTitle(context.getString(R.string.in_cinema))
                .setContentText(context.getString(R.string.in_cinema_silence))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(CINEMA_NOTIFICATION_ID, notification);
    }

    /**
     * Info (i.e. debugging) notification.
     */
    protected void info(String text) {
        int randomNum = ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
        final Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon_running)
                .setContentTitle(LocalDate.now() + " " + LocalTime.now())
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        notificationManager.notify(randomNum, notification);
    }
}
