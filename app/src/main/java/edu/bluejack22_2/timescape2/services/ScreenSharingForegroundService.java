package edu.bluejack22_2.timescape2.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import edu.bluejack22_2.timescape2.R;

public class ScreenSharingForegroundService extends Service {

    public static final String CHANNEL_ID = "ScreenSharingForegroundService";

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform your notification setup here
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.timescape_screen_sharing))
                .setContentText(getString(R.string.you_are_sharing_your_screen_to_a_live_room))
                .setSmallIcon(R.drawable.round_mobile_screen_share_24)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.timescape_screen_sharing_service),
                NotificationManager.IMPORTANCE_HIGH
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }
}

