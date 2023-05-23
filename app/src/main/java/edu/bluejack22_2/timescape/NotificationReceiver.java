package edu.bluejack22_2.timescape;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.content.ContentValues.TAG;
import static edu.bluejack22_2.timescape.BaseActivity.ACTION_MUTE;
import static edu.bluejack22_2.timescape.BaseActivity.ACTION_REPLY;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_MESSAGE_ID;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_PROJECT_ID;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_PROJECT_NAME;
import static edu.bluejack22_2.timescape.BaseActivity.EXTRA_REPLY;
import static edu.bluejack22_2.timescape.ProjectChatActivity.generateMessageId;

import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.bluejack22_2.timescape.model.Message;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.UserSettings;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (ACTION_REPLY.equals(action)) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence(EXTRA_REPLY);
                String messageId = intent.getStringExtra(EXTRA_MESSAGE_ID);
                String projectId = intent.getStringExtra(EXTRA_PROJECT_ID);
                String projectName = intent.getStringExtra(EXTRA_PROJECT_NAME);
                sendReplyMessage(projectName, messageId, replyText.toString(), projectId);

                int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(notificationId);
            }
        }else if (ACTION_MUTE.equals(action)) {
            String projectId = intent.getStringExtra(EXTRA_PROJECT_ID);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            db.collection("settings").document(userId).collection("mutedChats").document(projectId)
                    .set(new HashMap<>())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, R.string.chat_has_been_muted, Toast.LENGTH_SHORT).show();
                        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(notificationId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, R.string.failed_to_mute_chat, Toast.LENGTH_SHORT).show();
                        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(notificationId);
                    });
        }

    }

    private void sendReplyMessage(String projectName ,String repliedMessageId, String content, String projectId) {
        if (!TextUtils.isEmpty(content)) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference messagesCollection = db.collection("chats").document(projectId).collection("messages");

            Message message = new Message();
            message.setSender(db.collection("users").document(currentUserId));
            message.setMessage_type(Message.MessageType.REPLY);
            message.setContent(content);
            message.setId(generateMessageId());
            message.setReplyingTo(repliedMessageId);

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("sender", db.collection("users").document(currentUserId));
            messageMap.put("message_type", Message.MessageType.REPLY);
            messageMap.put("content", content);
            messageMap.put("timestamp", FieldValue.serverTimestamp());
            messageMap.put("id", generateMessageId());
            messageMap.put("replyingTo", repliedMessageId);

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(messageMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Reply message sent successfully");
                            onMessageSent(projectId, projectName, user.getDisplayName(), message.getContent(), message.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error sending reply message", e);
                        }
                    });
        }
    }

    private void onMessageSent(String projectId, String projectName, String senderName, String displayedContent, String messageId) {
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        // Retrieve all the members of the project
        firebaseFirestore.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Map<String, Object>> members = (Map<String, Map<String, Object>>) documentSnapshot.get("members"); // Assuming 'members' field in project document
                        Project project = documentSnapshot.toObject(Project.class);
                        members.put(project.getOwner().getId(), null);

                        if (members != null) {
                            for (String id : members.keySet()) {
                                if (id.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                    continue;
                                }

                                // Check if the member is in the chat room with the same projectId
                                firebaseFirestore.collection("activeChats").document(id)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                String activeProjectId = documentSnapshot.getString("projectId");
                                                if (activeProjectId == null || !(activeProjectId.equals("ALL") || activeProjectId.equals(projectId))) {
                                                    // Save the notification for this member
                                                    Map<String, Object> notificationData = new HashMap<>();
                                                    notificationData.put("senderName", senderName);
                                                    notificationData.put("projectId", projectId);
                                                    notificationData.put("projectName", projectName);
                                                    notificationData.put("content", displayedContent);
                                                    notificationData.put("messageId", messageId);
                                                    notificationData.put("notificationType", "PROJECT_CHAT_MESSAGE");

                                                    firebaseFirestore.collection("users").document(id).collection("notifications")
                                                            .add(notificationData);
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }
}

