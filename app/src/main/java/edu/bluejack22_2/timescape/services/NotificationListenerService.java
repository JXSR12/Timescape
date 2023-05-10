package edu.bluejack22_2.timescape.services;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

import static edu.bluejack22_2.timescape.BaseActivity.ACTION_MUTE;
import static edu.bluejack22_2.timescape.BaseActivity.ACTION_REPLY;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_MESSAGE_ID;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_PROJECT_ID;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_PROJECT_NAME;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_REPLY;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import edu.bluejack22_2.timescape.NotificationReceiver;
import edu.bluejack22_2.timescape.ProjectChatActivity;
import edu.bluejack22_2.timescape.ProjectDetailActivity;
import edu.bluejack22_2.timescape.R;

public class NotificationListenerService extends Service {
    public static final String CHANNEL_ID = "NotificationListenerServiceChannel";
    public static final String CHAT_CHANNEL_ID = "NotificationListenerChatChannel";
    public static final String WARNING_CHANNEL_ID = "NotificationListenerWarningChannel";
    public static final String ACTION_STOP = "edu.bluejack22_2.timescape.ACTION_STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        // Perform your notification setup here
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
        );

        manager.createNotificationChannel(serviceChannel);

        NotificationChannel chatChannel = new NotificationChannel(
                CHAT_CHANNEL_ID,
                "Project Chats",
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(chatChannel);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannels();
        String input = "Listening for notifications";
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Listener")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firebaseFirestore.collection("notifications").document(userId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException error) {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            List<Map<String, Object>> notifs = (List<Map<String, Object>>) documentSnapshot.get("notifs");
                            if (notifs != null) {
                                for (Map<String, Object> notificationData : notifs) {
                                    String notificationType = (String) notificationData.get("notificationType");
                                    sendNotification(notificationType, notificationData);

                                    // Remove the notification from the Firestore document
                                    firebaseFirestore.collection("notifications").document(userId)
                                            .update("notifs", FieldValue.delete());
                                }
                            }
                        }
                    }
                });

        Toast.makeText(this, "Notifications service started", Toast.LENGTH_SHORT).show();

        return START_NOT_STICKY;
    }


    @SuppressLint("MissingPermission")
    private void sendNotification(String notificationType, Map<String, Object> notificationData) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationChannel channel = notificationManager.getNotificationChannel(CHAT_CHANNEL_ID);
        NotificationChannel warningChannel = notificationManager.getNotificationChannel(WARNING_CHANNEL_ID);

        if ("PROJECT_CHAT_MESSAGE".equals(notificationType)) {
            if(FirebaseAuth.getInstance().getCurrentUser() == null) return;

            String senderName = (String) notificationData.get("senderName");
            String projectId = (String) notificationData.get("projectId");
            String projectName = (String) notificationData.get("projectName");
            String content = (String) notificationData.get("content");
            String messageId = (String) notificationData.get("messageId");

            // Create an Intent for ProjectChatActivity and add projectId as an extra
            Intent chatActivityIntent = new Intent(this, ProjectChatActivity.class);
            chatActivityIntent.putExtra("projectId", projectId);

            // Wrap the Intent in a PendingIntent
            PendingIntent chatActivityPendingIntent = PendingIntent.getActivity(this, 0, chatActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.drawable.round_question_answer_24) // Replace with your app's notification icon
                    .setContentTitle(senderName + " in " + projectName)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(chatActivityPendingIntent); // Set the PendingIntent as the content intent for the notification

            // Add Reply action with RemoteInput
            String replyLabel = "Reply";
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                    .setLabel(replyLabel)
                    .build();
            int notificationId = new Random().nextInt(1000000);

            Intent replyIntent = new Intent(this, NotificationReceiver.class);
            replyIntent.setAction(ACTION_REPLY);
            replyIntent.putExtra(EXTRA_MESSAGE_ID, messageId);
            replyIntent.putExtra(EXTRA_PROJECT_ID, projectId);
            replyIntent.putExtra(EXTRA_PROJECT_NAME, projectName);
            replyIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE);

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                    R.drawable.round_send_24, replyLabel, replyPendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(replyAction);

            // Add Mute action
            Intent muteIntent = new Intent(this, NotificationReceiver.class);
            muteIntent.setAction(ACTION_MUTE);
            muteIntent.putExtra(EXTRA_PROJECT_ID, projectId);
            muteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent mutePendingIntent = PendingIntent.getBroadcast(this, 0, muteIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action muteAction = new NotificationCompat.Action.Builder(
                    R.drawable.round_notifications_off_24, "Mute", mutePendingIntent).build();
            builder.addAction(muteAction);

            notificationManager.notify(notificationId, builder.build());
        }else if("PROJECT_DEADLINE_WARNING".equals(notificationType)){
            if(FirebaseAuth.getInstance().getCurrentUser() == null) return;

            Double time = (Double) notificationData.get("timeLeft");
            String unit = (String) notificationData.get("timeUnit");
            String projectId = (String) notificationData.get("projectId");
            String projectName = (String) notificationData.get("projectName");

            String unitStr = "TIME_UNIT";
            if(unit.equals("MINUTE")){
                unitStr = "minute(s)";
            }else if(unit.equals("HOUR")){
                unitStr = "hour(s)";
            }else if(unit.equals("DAY")){
                unitStr = "day(s)";
            }else if(unit.equals("WEEK")){
                unitStr = "week(s)";
            }

            String header = "Project Deadline Warning: " + projectName;
            String content = "The project deadline is in " + String.format(Locale.ENGLISH,"%.0f",Math.floor(time)) + " " + unitStr + ". Have you finished what you should do?";

            Intent detailActivityIntent = new Intent(this, ProjectDetailActivity.class);
            detailActivityIntent.putExtra("PROJECT_ID", projectId);

            int notificationId = new Random().nextInt(1000000);
            // Wrap the Intent in a PendingIntent
            PendingIntent detailActivityPendingIntent = PendingIntent.getActivity(this, 0, detailActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.drawable.round_description_24) // Replace with your app's notification icon
                    .setContentTitle(header)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(detailActivityPendingIntent);

            notificationManager.notify(notificationId, builder.build());
        }else if("PROJECT_OPERATION_NOTICE".equals(notificationType)){
            if(FirebaseAuth.getInstance().getCurrentUser() == null) return;

            String actorName = (String) notificationData.get("actorName");
            String actorId = (String) notificationData.get("actorUserId");
            String projectId = (String) notificationData.get("projectId");
            String projectName = (String) notificationData.get("projectName");
            String action = (String) notificationData.get("action"); //CAN BE "ADD" or "REMOVE". (Will add more later)
            String objectUserId = (String) notificationData.get("objectUserName");
            String objectUserName = (String) notificationData.get("objectUserId");

            String header = "Project Operations Notice: " + projectName;
            String actionVerb = "ACTION_VERB";
            if(action.equals("ADD")){
                actionVerb = "added";
            }else if(action.equals("REMOVE")){
                actionVerb = "removed";
            }
            String content = actorName + " has " + actionVerb + " " + objectUserName + (action.equals("ADD") ? " to " : " from ") + "the project.";

            Intent detailActivityIntent = new Intent(this, ProjectDetailActivity.class);
            detailActivityIntent.putExtra("PROJECT_ID", projectId);

            int notificationId = new Random().nextInt(1000000);
            // Wrap the Intent in a PendingIntent
            PendingIntent detailActivityPendingIntent = PendingIntent.getActivity(this, 0, detailActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.drawable.round_people_24) // Replace with your app's notification icon
                    .setContentTitle(header)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(detailActivityPendingIntent);

            notificationManager.notify(notificationId, builder.build());
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

