package edu.bluejack22_2.timescape.ui.chats;

import static android.content.ContentValues.TAG;

import static edu.bluejack22_2.timescape.ProjectChatActivity.generateMessageId;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.bluejack22_2.timescape.model.Chat;
import edu.bluejack22_2.timescape.model.Message;

public class ChatsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ChatsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Recent project chats will be shown here");
    }

    public LiveData<List<Chat>> getChatItems(String userId, ChatListAdapter chatListAdapter) {
        MutableLiveData<List<Chat>> chatItems = new MutableLiveData<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Timestamp currentTimestamp = Timestamp.now();

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        // Get the member projects
        Task<QuerySnapshot> memberProjectsTask = db.collection("projects")
                .whereLessThanOrEqualTo("members." + userId + ".date_joined", currentTimestamp)
                .get();
        tasks.add(memberProjectsTask);

        // Get the owner projects
        Task<QuerySnapshot> ownerProjectsTask = db.collection("projects")
                .whereEqualTo("owner", db.collection("users").document(userId))
                .get();
        tasks.add(ownerProjectsTask);

        // Combine both tasks and process the results
        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            List<Chat> chatItemList = new ArrayList<>();
            chatItemList.clear(); // Clear the chat list before processing updated items

            for (Object result : results) {
                QuerySnapshot querySnapshot = (QuerySnapshot) result;
                for (QueryDocumentSnapshot document : querySnapshot) {
                    processChatDocument(document, userId, chatItems, chatItemList, chatListAdapter);
                }
            }

            chatItems.setValue(chatItemList); // Update the MutableLiveData with the new chat list
            chatListAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
        });

        return chatItems;
    }

    private void processChatDocument(QueryDocumentSnapshot document, String userId, MutableLiveData<List<Chat>> chatItems, List<Chat> chatItemList, ChatListAdapter chatListAdapter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String projectId = document.getId();
        String projectTitle = document.getString("title");

        db.collection("chats")
                .document(projectId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((chatSnapshot, chatError) -> {
                    if (chatError != null) {
                        Log.w(TAG, "Chat listen failed.", chatError);
                        return;
                    }

                    List<DocumentSnapshot> messages = chatSnapshot.getDocuments();
                    if (messages != null && !messages.isEmpty()) {
                        DocumentSnapshot latestMessage = messages.get(messages.size() - 1);
                        Message latestMessageObject = latestMessage.toObject(Message.class);

                        int unreadCount = 0;

                        for (DocumentSnapshot messageSnapshot : messages) {
                            Message message = messageSnapshot.toObject(Message.class);
                            if (!message.getSender().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) && (message.getReads() == null || !message.getReads().contains(userId))) {
                                unreadCount++;
                            }
                        }

                        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                        Calendar calendar = Calendar.getInstance();
                        String todayDate = dateFormat.format(calendar.getTime());
                        calendar.add(Calendar.DATE, -1);
                        String yesterdayDate = dateFormat.format(calendar.getTime());
                        String msgDate = dateFormat.format(latestMessageObject.getTimestampAsDate());
                        String hourMinuteOnly = formatter.format(latestMessageObject.getTimestampAsDate());

                        String timestampString = "DATE";
                        if (msgDate.equals(todayDate)) {
                            timestampString = hourMinuteOnly;
                        } else if (msgDate.equals(yesterdayDate)) {
                            timestampString = "Yesterday";
                        } else {
                            timestampString = msgDate;
                        }

                        Chat chat = new Chat(
                                projectId,
                                projectTitle,
                                latestMessageObject,
                                "loading..",
                                timestampString,
                                unreadCount,
                                latestMessageObject.getMessage_type()
                        );

                        latestMessageObject.getSender().get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                chat.setSenderName((String) documentSnapshot.get("displayName"));

                                // Remove any existing chat with the same projectId
                                for (int i = 0; i < chatItemList.size(); i++) {
                                    if (chatItemList.get(i).getProjectId().equals(projectId)) {
                                        chatItemList.remove(i);
                                        break;
                                    }
                                }

                                // Add the updated chat item
                                chatItemList.add(chat);

                                // Sort chatItemList based on the timestamp of the latestMessage
                                Collections.sort(chatItemList, (chat1, chat2) -> {
                                    Timestamp timestamp1 = chat1.getLatestMessage().getTimestamp();
                                    Timestamp timestamp2 = chat2.getLatestMessage().getTimestamp();
                                    return timestamp2.compareTo(timestamp1);
                                });

                                // Update the MutableLiveData and notify the adapter
                                chatItems.setValue(chatItemList);
                                chatListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
    }

    public LiveData<String> getText() {
        return mText;
    }
}
