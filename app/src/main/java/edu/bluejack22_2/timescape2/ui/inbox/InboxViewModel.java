package edu.bluejack22_2.timescape2.ui.inbox;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.bluejack22_2.timescape2.model.InboxMessage;

public class InboxViewModel extends ViewModel {

    private MutableLiveData<List<InboxMessage>> allInboxMessagesLiveData;
    private MutableLiveData<List<InboxMessage>> unreadInboxMessagesLiveData;
    private MutableLiveData<List<InboxMessage>> readInboxMessagesLiveData;

    public InboxViewModel() {
        allInboxMessagesLiveData = new MutableLiveData<>();
        unreadInboxMessagesLiveData = new MutableLiveData<>();
        readInboxMessagesLiveData = new MutableLiveData<>();
    }

    public LiveData<List<InboxMessage>> getAllInboxMessagesLiveData() {
        return allInboxMessagesLiveData;
    }

    public LiveData<List<InboxMessage>> getUnreadInboxMessagesLiveData() {
        return unreadInboxMessagesLiveData;
    }

    public LiveData<List<InboxMessage>> getReadInboxMessagesLiveData() {
        return readInboxMessagesLiveData;
    }

    public void fetchInboxMessages(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("inbox_messages").document(userId);
        userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.w("InboxViewModel", "Error fetching messages", error);
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                List<InboxMessage> allMessages = new ArrayList<>();
                List<InboxMessage> unreadMessages = new ArrayList<>();
                List<InboxMessage> readMessages = new ArrayList<>();

                List<Map<String, Object>> messagesData = (List<Map<String, Object>>) documentSnapshot.get("messages");

                if (messagesData != null) {
                    for (Map<String, Object> messageData : messagesData) {
                        InboxMessage message = new InboxMessage();
                        message.setReceiverUserId((String) messageData.get("receiverUserId"));
                        message.setTitle((String) messageData.get("title"));
                        message.setContent((String) messageData.get("content"));
                        message.setSentTime((Timestamp) messageData.get("sentTime"));
                        message.setRead((Boolean) messageData.get("read"));

                        allMessages.add(message);

                        if (message.isRead()) {
                            readMessages.add(message);
                        } else {
                            unreadMessages.add(message);
                        }
                    }
                }

                allInboxMessagesLiveData.setValue(allMessages);
                unreadInboxMessagesLiveData.setValue(unreadMessages);
                readInboxMessagesLiveData.setValue(readMessages);
            } else {
                Log.d("InboxViewModel", "No messages found");
            }
        });
    }

    public void markMessageAsRead(InboxMessage message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("inbox_messages").document(message.getReceiverUserId());

        userRef.update("messages", FieldValue.arrayRemove(message)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                message.setRead(true);
                userRef.update("messages", FieldValue.arrayUnion(message));
            }
        });
    }

    public void markMessageAsUnread(InboxMessage message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("inbox_messages").document(message.getReceiverUserId());

        userRef.update("messages", FieldValue.arrayRemove(message)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                message.setRead(false);
                userRef.update("messages", FieldValue.arrayUnion(message));
            }
        });
    }
}

