package edu.bluejack22_2.timescape2.helpers;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static edu.bluejack22_2.timescape2.BaseActivity.ACTION_MUTE;
import static edu.bluejack22_2.timescape2.BaseActivity.ACTION_REPLY;
import static edu.bluejack22_2.timescape2.BaseActivity.EXTRA_MESSAGE_ID;
import static edu.bluejack22_2.timescape2.BaseActivity.EXTRA_PROJECT_ID;
import static edu.bluejack22_2.timescape2.BaseActivity.EXTRA_PROJECT_NAME;
import static edu.bluejack22_2.timescape2.BaseActivity.EXTRA_REPLY;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import edu.bluejack22_2.timescape2.FirestoreHelper;
import edu.bluejack22_2.timescape2.NotificationReceiver;
import edu.bluejack22_2.timescape2.ProjectChatActivity;
import edu.bluejack22_2.timescape2.ProjectDetailActivity;
import edu.bluejack22_2.timescape2.R;
import edu.bluejack22_2.timescape2.model.InboxMessage;

public class NotificationHelper {
    private Context context;
    private NotificationManager notificationManager;

    public static final String CHANNEL_ID = "NotificationListenerServiceChannel";
    public static final String CHAT_CHANNEL_ID = "NotificationListenerChatChannel";
    public static final String WARNING_CHANNEL_ID = "NotificationListenerWarningChannel";

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = context.getSystemService(NotificationManager.class);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        NotificationManager manager = context.getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.foreground_service),
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(serviceChannel);

        NotificationChannel chatChannel = new NotificationChannel(
                CHAT_CHANNEL_ID,
                context.getString(R.string.project_chats),
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(chatChannel);

        NotificationChannel warningChannel = new NotificationChannel(
                WARNING_CHANNEL_ID,
                context.getString(R.string.deadline_warning),
                NotificationManager.IMPORTANCE_HIGH
        );
        manager.createNotificationChannel(warningChannel);
    }

    @SuppressLint("MissingPermission")
    public void sendNotification(String notificationType, Map<String, String> notificationData) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
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
            Intent chatActivityIntent = new Intent(context, ProjectChatActivity.class);
            chatActivityIntent.putExtra("projectId", projectId);

            // Wrap the Intent in a PendingIntent
            PendingIntent chatActivityPendingIntent = PendingIntent.getActivity(context, 0, chatActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel.getId())
                    .setSmallIcon(R.drawable.round_question_answer_24) // Replace with your app's notification icon
                    .setContentTitle(senderName + context.getString(R.string.in) + projectName)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(chatActivityPendingIntent); // Set the PendingIntent as the content intent for the notification

            // Add Reply action with RemoteInput
            String replyLabel = context.getString(R.string.reply_3);
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                    .setLabel(replyLabel)
                    .build();
            int notificationId = getNotificationId(projectId);

            Intent replyIntent = new Intent(context, NotificationReceiver.class);
            replyIntent.setAction(ACTION_REPLY);
            replyIntent.putExtra(EXTRA_MESSAGE_ID, messageId);
            replyIntent.putExtra(EXTRA_PROJECT_ID, projectId);
            replyIntent.putExtra(EXTRA_PROJECT_NAME, projectName);
            replyIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            int uniqueRequestCode = getNotificationId(messageId);
            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, uniqueRequestCode, replyIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                    R.drawable.round_send_24, replyLabel, replyPendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();

            builder.addAction(replyAction);

            // Add Mute action
            Intent muteIntent = new Intent(context, NotificationReceiver.class);
            muteIntent.setAction(ACTION_MUTE);
            muteIntent.putExtra(EXTRA_PROJECT_ID, projectId);
            muteIntent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
            PendingIntent mutePendingIntent = PendingIntent.getBroadcast(context, 0, muteIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action muteAction = new NotificationCompat.Action.Builder(
                    R.drawable.round_notifications_off_24, context.getString(R.string.mute), mutePendingIntent).build();
            builder.addAction(muteAction);

            notificationManager.notify(notificationId, builder.build());
        }else if("PROJECT_DEADLINE_WARNING".equals(notificationType)){
            Log.d("RECEIVED NOTIF", "PROJECT DEADLINE WARNING RECEIVED (Before auth check)");
            if(FirebaseAuth.getInstance().getCurrentUser() == null) return;
            Log.d("RECEIVED NOTIF", "PROJECT DEADLINE WARNING RECEIVED (After auth check)");
            double time = 0.0;
            if(notificationData.get("timeRemaining") != null){
                time = Double.parseDouble(notificationData.get("timeRemaining"));
            }
            String unit = (String) notificationData.get("timeUnit");
            String projectId = (String) notificationData.get("projectId");
            String projectName = (String) notificationData.get("projectName");

            String unitStr = "TIME_UNIT";
            if(unit.equals("MINUTE")){
                unitStr = context.getString(R.string.minute_s);
            }else if(unit.equals("HOUR")){
                unitStr = context.getString(R.string.hour_s);
            }else if(unit.equals("DAY")){
                unitStr = context.getString(R.string.day_s);
            }else if(unit.equals("WEEK")){
                unitStr = context.getString(R.string.week_s);
            }

            String header = context.getString(R.string.project_deadline_warning) + projectName;
            String content = context.getString(R.string.the_project_deadline_is_in) + String.format(Locale.ENGLISH,"%.0f",Math.floor(time)) + " " + unitStr + context.getString(R.string.have_you_finished_what_you_should_do);

            Intent detailActivityIntent = new Intent(context, ProjectDetailActivity.class);
            detailActivityIntent.putExtra("PROJECT_ID", projectId);

            int notificationId = new Random().nextInt(1000000);
            // Wrap the Intent in a PendingIntent
            PendingIntent detailActivityPendingIntent = PendingIntent.getActivity(context, 0, detailActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, warningChannel.getId())
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
            String objectUserId = (String) notificationData.get("objectUserId");
            String objectUserName = (String) notificationData.get("objectUserName");

            //
            String header = context.getString(R.string.project_operations_notice) + projectName;
            String actionVerb = "ACTION_VERB";
            if(action.equals("ADD")){
                actionVerb = context.getString(R.string.op_added);
            }else if(action.equals("REMOVE")){
                actionVerb = context.getString(R.string.op_removed);
            }

            String content = actorName + context.getString(R.string.op_has) + actionVerb + " " + context.getString(R.string.op_you) + (action.equals("ADD") ? context.getString(R.string.op_to) : context.getString(R.string.op_from)) + context.getString(R.string.op_the_project);

            Intent detailActivityIntent = new Intent(context, ProjectDetailActivity.class);
            detailActivityIntent.putExtra("PROJECT_ID", projectId);

            int notificationId = new Random().nextInt(1000000);
            // Wrap the Intent in a PendingIntent
            PendingIntent detailActivityPendingIntent = PendingIntent.getActivity(context, 0, detailActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create a notification with the specified data
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, warningChannel.getId())
                    .setSmallIcon(R.drawable.round_people_24) // Replace with your app's notification icon
                    .setContentTitle(header)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(detailActivityPendingIntent);

            notificationManager.notify(notificationId, builder.build());

            InboxMessage message = new InboxMessage(objectUserId, context.getString(R.string.you_have_been) + actionVerb + (action.equals("ADD") ? context.getString(R.string.op_to) : context.getString(R.string.op_from)) + projectName, content + (action.equals("ADD") ? context.getString(R.string.the_project_should_now_be_visible_to_you_in_your_dashboard_you_can_now_collaborate_and_contribute_to_this_project_be_sure_to_also_check_the_project_chat_in_case_of_any_new_messages) : context.getString(R.string.the_project_should_no_longer_be_visible_in_your_dashboard_and_your_access_to_contribute_to_the_project_has_been_revoked_if_you_think_that_this_is_a_mistake_please_contact_the_owner_of_the_project)), Timestamp.now(), false);
            FirestoreHelper.sendInboxMessage(message);
            Log.d("SendInbox", "Sending to " + objectUserId);
        }
    }

    private int getNotificationId(String projectId) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // return a default value
            return 0;
        }
        byte[] encodedhash = digest.digest(projectId.getBytes(StandardCharsets.UTF_8));
        // Get the last 4 bytes of the hash and convert to int. This assumes relatively low collision.
        return ByteBuffer.wrap(encodedhash, encodedhash.length - 4, 4).getInt();
    }

}
