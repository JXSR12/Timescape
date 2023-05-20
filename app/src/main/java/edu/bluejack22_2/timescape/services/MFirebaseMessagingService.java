package edu.bluejack22_2.timescape.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import edu.bluejack22_2.timescape.helpers.NotificationHelper;

public class MFirebaseMessagingService extends FirebaseMessagingService {

    private NotificationHelper notificationHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null) {
            // It's a notification, handle it here.
        }

        Map<String, String> data = remoteMessage.getData();

        if (data.containsKey("notificationType")) {
            String notificationType = data.get("notificationType");

            Log.d("RECEIVED NOTIF", "RECEIVED A POSSIBLE NOTIF");

            switch (notificationType) {
                case "PROJECT_CHAT_MESSAGE":
                case "PROJECT_OPERATION_NOTICE":
                case "PROJECT_DEADLINE_WARNING":
                    Log.d("RECEIVED NOTIF", "RECEIVED A VALID NOTIF TYPE");
                    notificationHelper.sendNotification(notificationType, data);
                    break;
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "New token: " + token);
    }
}


