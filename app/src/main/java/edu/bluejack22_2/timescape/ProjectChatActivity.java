package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.bluejack22_2.timescape.model.Message;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.chats.ChatAdapter;

public class ProjectChatActivity extends BaseActivity implements ChatAdapter.MessageLongClickListener{
    private Project project;
    private HashSet<String> projectMemberIds;

    private boolean isInActivity = false;
    private ArrayList<User> projectMembers = new ArrayList<>();
    private HashMap<String, String> mentionedUserIds = new HashMap<>();

    private String userId;
    private String projectId = "";
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private ListenerRegistration chatDocListener;
    private boolean isDeletingMention = false;
    TextView actionBarTitle;
    TextView actionBarOnlineText;
    ImageView actionBarOnlineBullet;

    RecyclerView chatRecyclerView;
    ChatAdapter chatAdapter;
    EditText messageInput;
    ImageButton sendMessageButton;
    ImageButton attachFileButton;
    ImageButton attachImageButton;
    ImageButton inviteProjectButton;
    RelativeLayout replyModeLayout;
    TextView replyModeText;

    int onlineCount = 0;
    int typingCount = 0;

    private boolean replyMode;
    private String repliedMessageId;

    private MemberAdapter memberAdapter;
    private boolean firstLoadChat = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageInput = findViewById(R.id.message_input);
        sendMessageButton = findViewById(R.id.send_message_button);
        attachFileButton = findViewById(R.id.attach_file_button);
        attachImageButton = findViewById(R.id.attach_image_button);
        inviteProjectButton = findViewById(R.id.send_project_invite_button);
        replyModeLayout = findViewById(R.id.reply_mode_layout);
        replyModeText = findViewById(R.id.reply_mode_text);

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        projectId = getIntent().getStringExtra("projectId");

        chatAdapter = new ChatAdapter(this, mAuth, this, chatRecyclerView, projectId);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        userId = firebaseAuth.getCurrentUser().getUid();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            // Inflate the custom layout for the ActionBar
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.custom_action_bar, null);

            actionBarTitle = customActionBarView.findViewById(R.id.project_title);
            actionBarOnlineText = customActionBarView.findViewById(R.id.online_status);
            actionBarOnlineBullet = customActionBarView.findViewById(R.id.online_status_bullet);

            // Set the custom view for the ActionBar
            actionBar.setCustomView(customActionBarView);
            actionBar.setDisplayShowCustomEnabled(true);
        }

//        String projectId = getIntent().getStringExtra("projectId");
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        DocumentReference chatDocument = db.collection("chats").document(projectId);
//        DocumentReference projectDocument = db.collection("projects").document(projectId);
//
//        projectDocument
//                .addSnapshotListener((value, error) -> {
//                    if (error != null) {
//                        // Handle error
//                        return;
//                    }
//
//                    if (value != null && value.exists()) {
//                        project = value.toObject(Project.class);
//                        project.setUid(value.getId());
//                        project.setOwner(value.getDocumentReference("owner"));
//                        actionBarTitle.setText(project.getTitle());
//                        setupChatDocumentListener();
//                        fetchAndMarkAllMessagesAsRead();
//                    }
//                });
//
//        db.runTransaction(new Transaction.Function<Void>() {
//            @Override
//            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
//                DocumentSnapshot snapshot = transaction.get(chatDocument);
//                if (!snapshot.exists() || !snapshot.contains("messages")) {
//                    transaction.set(chatDocument, new HashMap<String, Object>() {{
//                        put("messages", new ArrayList<Message>());
//                    }});
//                }
//                return null;
//            }
//        });

        String projectId = getIntent().getStringExtra("projectId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference projectDocument = db.collection("projects").document(projectId);
        CollectionReference messagesCollection = db.collection("chats").document(projectId).collection("messages");

        projectDocument
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }

                    if (value != null && value.exists()) {
                        project = value.toObject(Project.class);
                        project.setUid(value.getId());
                        project.setOwner(value.getDocumentReference("owner"));
                        actionBarTitle.setText(project.getTitle());
                        setupChatDocumentListener();
                        fetchAndMarkAllMessagesAsRead();
                    }
                });


        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        attachFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                pickFileLauncher.launch(intent);
            }
        });

        attachImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            }
        });


        inviteProjectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the user's ID
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Get the document reference of the user
                DocumentReference userRef = db.collection("users").document(userId);

                // Query for projects where the user is a member
                Query memberQuery = db.collection("projects").whereEqualTo("members." + userId, true);

                // Query for projects where the user is the owner
                Query ownerQuery = db.collection("projects").whereEqualTo("owner", userRef);

                // Run the queries
                Task<QuerySnapshot> memberTask = memberQuery.get();
                Task<QuerySnapshot> ownerTask = ownerQuery.get();

                // Continue with both tasks
                Tasks.whenAllSuccess(memberTask, ownerTask).addOnSuccessListener(new OnSuccessListener<List<Object>>() {
                    @Override
                    public void onSuccess(List<Object> list) {
                        // Combine the results
                        List<Project> projects = new ArrayList<>();
                        for (Object object : list) {
                            QuerySnapshot snapshot = (QuerySnapshot) object;
                            for(DocumentSnapshot ds : snapshot.getDocuments()){
                                Project p = ds.toObject(Project.class);
                                if(p != null){
                                    p.setUid(ds.getId());
                                }
                                projects.add(p);
                            }

                        }

                        // Prepare the list of project titles for the AlertDialog
                        final CharSequence[] projectTitles = projects.stream().map(Project::getTitle).toArray(CharSequence[]::new);

                        // Create and show the AlertDialog
                        new AlertDialog.Builder(ProjectChatActivity.this)
                                .setTitle(R.string.select_a_project_to_invite_to)
                                .setItems(projectTitles, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // User selected a project, show the confirmation dialog
                                        new AlertDialog.Builder(ProjectChatActivity.this)
                                                .setTitle(R.string.confirm_invitation)
                                                .setMessage(getString(R.string.are_you_sure_to_send_invite_for) + projectTitles[which] + "'?")
                                                .setPositiveButton(R.string.yes_dial, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which2) {
                                                        // User confirmed, send the project invite message
                                                        sendProjectInviteMessage(projects.get(which).getUid());
                                                    }
                                                })
                                                .setNegativeButton(R.string.no_dial, null)
                                                .show();
                                    }
                                })
                                .show();
                    }
                });
            }
        });


        final View activityRootView = findViewById(android.R.id.content);
        activityRootView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int heightDiff = bottom - oldBottom;
                if (heightDiff < -200) { // If more than 200 pixels, its probably a keyboard...
                    // Keyboard is visible, scroll RecyclerView to the last message
                    chatRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            if (chatAdapter.getItemCount() > 0) {
                                chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                            }
                        }
                    });
                }
            }
        });

        // Initialize the ListView and ArrayAdapter
        RelativeLayout mentionListLayout = findViewById(R.id.mention_list_layout);
        ListView mentionListView = findViewById(R.id.mention_list);
        memberAdapter = new MemberAdapter(this, projectMembers);
        mentionListView.setAdapter(memberAdapter);

        mentionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User clickedUser = memberAdapter.getItem(position);
                String mentionName = "<@" + clickedUser.getDisplayName() +">";
                int selectionStart = messageInput.getSelectionStart();
                int mentionStart = messageInput.getText().toString().lastIndexOf('@', selectionStart);
                messageInput.getText().replace(mentionStart, selectionStart, ""); // Remove the '@' character that was typed before
                insertMention(messageInput, mentionName, clickedUser.getUid());
                mentionListLayout.setVisibility(View.GONE);
            }
        });

        messageInput.addTextChangedListener(new TextWatcher() {
            String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String inputText = s.toString();
                int mentionEndIndex = inputText.lastIndexOf('@');
                int cursorPosition = messageInput.getSelectionStart();

                if (beforeText.length() > inputText.length()) { // Backspace detected
                    if (cursorPosition > 0 && cursorPosition <= s.length()) {
                        MentionClickableSpan[] allSpans = s.getSpans(0, s.length(), MentionClickableSpan.class);
                        boolean foundSpan = false;

                        for (MentionClickableSpan span : allSpans) {
                            int spanStart = s.getSpanStart(span);
                            int spanEnd = s.getSpanEnd(span);

                            if (cursorPosition > spanStart && cursorPosition <= spanEnd) {
                                foundSpan = true;

                                // If cursor is at the end of the Spannable, remove the whole spannable
                                if (cursorPosition == spanEnd) {
                                    s.delete(spanStart, spanEnd);
                                    s.removeSpan(span);
                                } else {
                                    // If cursor is at any other part of the spannable, remove the part of the text and convert it to regular text
                                    s.removeSpan(span);
                                    ForegroundColorSpan defaultColorSpan = new ForegroundColorSpan(getColor(R.color.primaryTextColor)); // Replace with the default text color
                                    s.setSpan(defaultColorSpan, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                                break;
                            }
                        }

                        if (!foundSpan) {
                            mentionListLayout.setVisibility(View.GONE);
                        }

                        if (cursorPosition <= s.length()) {
                            messageInput.setSelection(cursorPosition);
                        }
                    }
                } else if (mentionEndIndex != -1 && cursorPosition > mentionEndIndex) {
                    String filterText = inputText.substring(mentionEndIndex + 1, cursorPosition);
                    memberAdapter.getFilter().filter(filterText, new Filter.FilterListener() {
                        @Override
                        public void onFilterComplete(int count) {
                            if (memberAdapter.getCount() > 0) {
                                mentionListLayout.setVisibility(View.VISIBLE);
                            } else {
                                mentionListLayout.setVisibility(View.GONE);
                            }
                        }
                    });
                } else {
                    mentionListLayout.setVisibility(View.GONE);
                }
            }

        });
        getProjectMembers(projectId);
        initTypingStatusListener();
    }

    private void insertMention(EditText messageInput, String mentionDisplayName, String mentionId) {
        SpannableString mentionSpannable = new SpannableString(mentionDisplayName);

        MentionClickableSpan mentionClickableSpan = new MentionClickableSpan(mentionId);
        mentionSpannable.setSpan(mentionClickableSpan, 0, mentionDisplayName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE); // Replace with desired color
        mentionSpannable.setSpan(colorSpan, 0, mentionDisplayName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int cursorPosition = messageInput.getSelectionStart();
        Editable messageEditable = messageInput.getText();
        messageEditable.insert(cursorPosition, mentionSpannable);
    }

    private HashMap<String, String> getMentionsFromEditable(Editable editable) {
        HashMap<String, String> mentions = new HashMap<>();
        MentionClickableSpan[] mentionSpans = editable.getSpans(0, editable.length(), MentionClickableSpan.class);
        for (MentionClickableSpan mentionSpan : mentionSpans) {
            int spanStart = editable.getSpanStart(mentionSpan);
            int spanEnd = editable.getSpanEnd(mentionSpan);
            String mentionKey = editable.subSequence(spanStart, spanEnd).toString() + "_" + spanStart;
            mentions.put(mentionKey, mentionSpan.getUserId());
        }
        return mentions;
    }

//    private ListenerRegistration setupChatDocumentListener() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        DocumentReference chatDocument = db.collection("chats").document(project.getUid());
//        return chatDocument.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//            @Override
//            public void onEvent(@Nullable DocumentSnapshot snapshot,
//                                @Nullable FirebaseFirestoreException e) {
//                if (e != null) {
//                    Log.w(TAG, "Listen failed.", e);
//                    return;
//                }
//
//                if (snapshot != null && snapshot.exists()) {
//                    List<Message> messages = new ArrayList<>();
//                    List<HashMap<String, Object>> documents = (List<HashMap<String, Object>>) snapshot.get("messages");
//                    boolean idChanges = false;
//                    if(documents == null) return;
//
//                    if(!firstLoadChat) {
//                        HashMap<String, Object> lastMessage = documents.get(documents.size() - 1);
//                        // Check if the last message is not from the current user and mark it as read if visible
//                        String lastMessageSenderId = ((DocumentReference) lastMessage.get("sender")).getId();
//                        if (!lastMessageSenderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
//                            Message message = new Message();
//                            if (lastMessage.get("id") == null) {
//                                String newId = generateMessageId();
//                                message.setId(newId);
//                                lastMessage.put("id", newId); //Update to the firestore to store the newly generated ID.
//                                idChanges = true;
//                            } else {
//                                message.setId((String) lastMessage.get("id"));
//                            }
//                            message.setSender((DocumentReference) lastMessage.get("sender"));
//                            message.setMessage_type(Message.MessageType.valueOf((String) lastMessage.get("message_type")));
//                            message.setContent((String) lastMessage.get("content"));
//                            message.setTimestamp((Timestamp) lastMessage.get("timestamp"));
//                            message.setFileName((String) lastMessage.get("fileName"));
//                            message.setReplyingTo((String) lastMessage.get("replyingTo"));
//                            message.setMentions(lastMessage.get("mentions") != null ? (HashMap<String, String>) lastMessage.get("mentions") : null);
//                            message.setReads(lastMessage.get("reads") != null ? (List<String>) lastMessage.get("reads") : null);
//
//
//                            markMessageAsReadIfVisible(message);
//                        }
//                    }
//
//                    for (HashMap<String, Object> document : documents) {
//                        Message message = new Message();
//                        if(document.get("id") == null){
//                            String newId = generateMessageId();
//                            message.setId(newId);
//                            document.put("id", newId); //Update to the firestore to store the newly generated ID.
//                            idChanges = true;
//                        }else{
//                            message.setId((String) document.get("id"));
//                        }
//                        message.setSender((DocumentReference) document.get("sender"));
//                        message.setMessage_type(Message.MessageType.valueOf((String) document.get("message_type")));
//                        message.setContent((String) document.get("content"));
//                        message.setTimestamp((Timestamp) document.get("timestamp"));
//                        message.setFileName((String) document.get("fileName"));
//                        message.setReplyingTo((String) document.get("replyingTo"));
//                        message.setMentions(document.get("mentions") != null ? (HashMap<String, String>) document.get("mentions") : null);
//                        message.setReads(document.get("reads") != null ? (List<String>) document.get("reads") : null);
//
//                        messages.add(message);
//                    }
//                    if(idChanges){
//                        chatDocument.update("messages", documents);
//                    }
//                    chatAdapter.setMessages(messages, chatRecyclerView);
//                } else {
//                    Log.d(TAG, "Current data: null");
//                }
//
//            }
//        });
//    }

    private ListenerRegistration setupChatDocumentListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");
        return messagesCollection.orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                List<Message> messages = new ArrayList<>();
                boolean idChanges = false;
                if (snapshots != null) {
                    for (QueryDocumentSnapshot document : snapshots) {
                        Message message = document.toObject(Message.class);

                        // Check if the last message is not from the current user and mark it as read if visible
                        if (document.getId().equals(snapshots.getDocuments().get(snapshots.size()-1).getId())) {
                            if (!message.getSender().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                markMessageAsReadIfVisible(message);
                            }
                        }

                        messages.add(message);
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }

                chatAdapter.setMessages(messages, chatRecyclerView);
            }
        });
    }

    private void markMessageAsReadIfVisible(Message message) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager != null && isInActivity) {
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int lastMessagePosition = chatAdapter.getItemCount() - 1;

            // Check if the last message is visible
            if (lastMessagePosition >= firstVisibleItemPosition && lastMessagePosition <= lastVisibleItemPosition) {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                ArrayList<String> reads = (ArrayList<String>) message.getReads();

                if (reads == null) {
                    reads = new ArrayList<>();
                }

                // Check if the message has not been read by the current user
                if (!reads.contains(currentUserId)) {
                    reads.add(currentUserId);
                    message.setReads(reads);

                    // Update message read status in your database
                    updateMessageReadStatus(project.getUid(), message);
                }
            }
        }
    }

//    private void fetchAndMarkAllMessagesAsRead() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        String chatDocumentId = project.getUid(); // Replace with your chat document ID (project ID)
//
//        db.collection("chats")
//                .document(chatDocumentId)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        if (task.isSuccessful()) {
//                            DocumentSnapshot document = task.getResult();
//                            if (document.exists()) {
//                                ArrayList<HashMap<String, Object>> messages = (ArrayList<HashMap<String, Object>>) document.get("messages");
//
//                                if (messages != null) {
//                                    for (HashMap<String, Object> message : messages) {
//                                        String senderId = ((DocumentReference) message.get("sender")).getId();
//                                        if (!senderId.equals(currentUserId)) {
//                                            ArrayList<String> reads = (ArrayList<String>) message.get("reads");
//                                            if (reads == null) {
//                                                reads = new ArrayList<>();
//                                            }
//                                            if (!reads.contains(currentUserId)) {
//                                                reads.add(currentUserId);
//                                                message.put("reads", reads);
//                                            }
//                                        }
//                                    }
//                                    // Update the chat document with the new read status
//                                    db.collection("chats")
//                                            .document(chatDocumentId)
//                                            .update("messages", messages).addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                @Override
//                                                public void onSuccess(Void unused) {
//                                                    firstLoadChat = false;
//                                                }
//                                            });
//                                }
//                            }
//                        }
//                    }
//                });
//    }
//
//
//
//    private void updateMessageReadStatus(String projectId, Message message) {
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        DocumentReference chatDocRef = db.collection("chats").document(projectId);
//        chatDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot document = task.getResult();
//                    if (document.exists()) {
//                        List<HashMap<String, Object>> messages = (List<HashMap<String, Object>>) document.get("messages");
//                        int messageIndex = -1;
//                        for (int i = 0; i < messages.size(); i++) {
//                            if (messages.get(i).get("id").equals(message.getId())) {
//                                messageIndex = i;
//                                break;
//                            }
//                        }
//
//                        if (messageIndex != -1) {
//                            List<String> reads = (List<String>) messages.get(messageIndex).get("reads");
//
//                            if (reads == null) {
//                                reads = new ArrayList<>();
//                            }
//
//                            // Check if the message has not been read by the current user
//                            if (!reads.contains(currentUserId)) {
//                                reads.add(currentUserId);
//                                messages.get(messageIndex).put("reads", reads);
//                                chatDocRef.update("messages", messages)
//                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                            @Override
//                                            public void onSuccess(Void aVoid) {
//                                                Log.d(TAG, "Message read status successfully updated.");
//                                            }
//                                        })
//                                        .addOnFailureListener(new OnFailureListener() {
//                                            @Override
//                                            public void onFailure(@NonNull Exception e) {
//                                                Log.w(TAG, "Error updating message read status", e);
//                                            }
//                                        });
//                            }
//                        }
//                    }
//                } else {
//                    Log.d(TAG, "get failed with ", task.getException());
//                }
//            }
//        });
//
//    }

    private void fetchAndMarkAllMessagesAsRead() {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if(project == null) return;
        String chatDocumentId = project.getUid(); // Replace with your chat document ID (project ID)

        db.collection("chats")
                .document(chatDocumentId)
                .collection("messages")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Message message = document.toObject(Message.class);
                                String senderId = message.getSender().getId();
                                if (!senderId.equals(currentUserId)) {
                                    ArrayList<String> reads = (ArrayList<String>) message.getReads();
                                    if (reads == null) {
                                        reads = new ArrayList<>();
                                    }
                                    if (!reads.contains(currentUserId)) {
                                        reads.add(currentUserId);
                                        message.setReads(reads);
                                        document.getReference().set(message);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void updateMessageReadStatus(String projectId, Message message) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference messageDocRef = db.collection("chats").document(projectId).collection("messages").document(message.getId());
        messageDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Message existingMessage = document.toObject(Message.class);
                        if (existingMessage != null) {
                            List<String> reads = existingMessage.getReads();

                            if (reads == null) {
                                reads = new ArrayList<>();
                            }

                            // Check if the message has not been read by the current user
                            if (!reads.contains(currentUserId)) {
                                reads.add(currentUserId);
                                existingMessage.setReads(reads);
                                messageDocRef.set(existingMessage)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "Message read status successfully updated.");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error updating message read status", e);
                                            }
                                        });
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    private void getProjectMembers(String projectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference projectRef = db.collection("projects").document(projectId);
        CollectionReference usersCollection = db.collection("users");

        projectRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    Map<String, Object> members = (Map<String, Object>) documentSnapshot.get("members");
                    if (members != null) {
                        projectMemberIds = new HashSet<>();
                        projectMemberIds.addAll(members.keySet());
                        projectMemberIds.add(project.getOwner().getId());

                        for(String id : projectMemberIds){
                            usersCollection.document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if(documentSnapshot.exists()){
                                        projectMembers.add(documentSnapshot.toObject(User.class));
                                        Log.d("Project Member Loaded", "Members count: " + projectMembers.size());
                                        memberAdapter.notifyDataSetChanged();
                                        chatAdapter.setCachedMembers(projectMembers);
                                        chatAdapter.notifyDataSetChanged();
                                    }
                                }});
                        }

                        listenForOnlineStatusChanges();
                    } else {
                        Log.w(TAG, "No members found in the project");
                    }
                } else {
                    Log.w(TAG, "Project document not found");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error getting project members", e);
            }
        });
    }

    private void initTypingStatusListener() {
        messageInput.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable typingTimeout = () -> setUserTypingStatus(false);

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setUserTypingStatus(!s.toString().trim().isEmpty());
                handler.removeCallbacks(typingTimeout);
                handler.postDelayed(typingTimeout, 3000);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }


    private void setUserTypingStatus(boolean typing) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference statusRef = db.collection("status").document(currentUserId);
        statusRef.set(Collections.singletonMap("typing", typing), SetOptions.merge());
    }

    private void listenForOnlineStatusChanges() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference statusRef = db.collection("status");
        statusRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w(TAG, "listen:error", error);
                    return;
                }

                onlineCount = 0;
                typingCount = 0;
                List<String> typingUserIds = new ArrayList<>();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                for (QueryDocumentSnapshot doc : value) {
                    if (doc.exists() && !doc.getId().equals(currentUserId) && projectMemberIds.contains(doc.getId())) {
                        Boolean online = doc.getBoolean("online");

                        if (online != null && online) {
                            onlineCount++;
                            Log.d(TAG, "Online User ID: " + doc.getId());
                        }

                        Boolean typing = doc.getBoolean("typing");
                        if (typing != null && typing) {
                            // Add the user to the list of typing users
                            typingUserIds.add(doc.getId());
                        }
                    }
                }

                // Check the active chat room of each typing user
                checkTypingUsersActiveChatRoom(db, typingUserIds, 0);

                Log.d(TAG, "Online Count: " + onlineCount);
                updateOnlineStatusDisplay(onlineCount);
            }
        });
    }

    private void checkTypingUsersActiveChatRoom(FirebaseFirestore db, List<String> typingUserIds, int index) {
        if (index >= typingUserIds.size()) {
            // All users have been checked, update the UI
            updateTypingStatusDisplay(typingCount);
            return;
        }

        String userId = typingUserIds.get(index);
        CollectionReference activeChatsRef = db.collection("activeChats");
        activeChatsRef.document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String activeChatProjectId = document.getString("projectId");
                                if (projectId.equals(activeChatProjectId)) {
                                    // The user is really typing in this project's chat
                                    typingCount++;
                                }
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }

                        // Check the next user
                        checkTypingUsersActiveChatRoom(db, typingUserIds, index + 1);
                    }
                });
    }

    private void updateOnlineStatusDisplay(int onlineCount) {
        if (onlineCount > 0) {
            actionBarOnlineText.setText(onlineCount > 1 ? onlineCount + getString(R.string.other_members_online) : getString(R.string._1_other_member_online));
            actionBarOnlineBullet.setColorFilter(ContextCompat.getColor(this, R.color.green_online));
        } else {
            actionBarOnlineText.setText(R.string.no_other_member_online);
            actionBarOnlineBullet.setColorFilter(ContextCompat.getColor(this, R.color.gray_offline));
        }
    }

    private void updateTypingStatusDisplay(int typingCount) {
        if (typingCount > 0) {
            actionBarOnlineText.setText(typingCount > 1 ? typingCount + getString(R.string.members_are_typing) : getString(R.string._1_member_is_typing));
        } else {
            updateOnlineStatusDisplay(onlineCount);
        }
    }



    private final ActivityResultLauncher<Intent> pickFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
            Uri fileUri = result.getData().getData();
            String fileName = getFileNameFromUri(getApplicationContext(), fileUri);
            uploadFileToFirebaseStorage(fileUri, "files", fileName);
        }
    });

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
            Uri fileUri = result.getData().getData();
            String fileName = getFileNameFromUri(getApplicationContext(), fileUri);
            uploadFileToFirebaseStorage(fileUri, "images", fileName);
        }
    });

    public static String generateMessageId() {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("dummy").document();
        String id = docRef.getId();
        docRef.delete();
        return id;
    }

    @SuppressLint("Range")
    private String getFileNameFromUri(Context context, Uri uri) {
        String fileName = "";
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        } else if (scheme.equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                cursor.close();
            }
        }
        return fileName;
    }


//    private void sendImageMessage(String fileName, String imageURL) {
//        if (!TextUtils.isEmpty(imageURL)) {
//            // Get the current user's ID
//            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            DocumentReference chatDocument = db.collection("chats").document(project.getUid());
//
//            // Create a new message object
//            Message message = new Message();
//            message.setSender(db.collection("users").document(currentUserId));
//            message.setMessage_type(Message.MessageType.IMAGE);
//            message.setContent(imageURL);
//            message.setTimestamp(Timestamp.now());
//            message.setId(generateMessageId());
//            message.setFileName(fileName);
//
//            // Add the message to the chatDocument
//            chatDocument.update("messages", FieldValue.arrayUnion(message))
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            Log.d(TAG, "Message sent successfully");
//                            messageInput.setText("");
//                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Log.w(TAG, "Error sending message", e);
//                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        } else {
//            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void sendFileMessage(String fileFullName, String fileURL) {
//        if (!TextUtils.isEmpty(fileURL)) {
//            // Get the current user's ID
//            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            DocumentReference chatDocument = db.collection("chats").document(project.getUid());
//
//            // Create a new message object
//            Message message = new Message();
//            message.setSender(db.collection("users").document(currentUserId));
//            message.setMessage_type(Message.MessageType.FILE);
//            message.setContent(fileURL);
//            message.setTimestamp(Timestamp.now());
//            message.setFileName(fileFullName);
//            message.setId(generateMessageId());
//
//            // Add the message to the chatDocument
//            chatDocument.update("messages", FieldValue.arrayUnion(message))
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            Log.d(TAG, "Message sent successfully");
//                            messageInput.setText("");
//                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Log.w(TAG, "Error sending message", e);
//                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        } else {
//            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
//        }
//    }

    private void sendImageMessage(String fileName, String imageURL) {
        if (!TextUtils.isEmpty(imageURL)) {
            // Get the current user's ID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");

            // Create a new message object
            Message message = new Message();
            message.setSender(db.collection("users").document(currentUserId));
            message.setMessage_type(Message.MessageType.IMAGE);
            message.setContent(imageURL);
            message.setTimestamp(Timestamp.now());
            message.setId(generateMessageId());
            message.setFileName(fileName);

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Image message sent successfully");
                            messageInput.setText("");
                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), firebaseAuth.getCurrentUser().getDisplayName() + getString(R.string.sent_an_image_attachment), message.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error sending image message", e);
                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileMessage(String fileFullName, String fileURL) {
        if (!TextUtils.isEmpty(fileURL)) {
            // Get the current user's ID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");

            // Create a new message object
            Message message = new Message();
            message.setSender(db.collection("users").document(currentUserId));
            message.setMessage_type(Message.MessageType.FILE);
            message.setContent(fileURL);
            message.setTimestamp(Timestamp.now());
            message.setId(generateMessageId());
            message.setFileName(fileFullName);

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "File message sent successfully");
                            messageInput.setText("");
                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), firebaseAuth.getCurrentUser().getDisplayName() + getString(R.string.sent_a_file_attachment), message.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error sending file message", e);
                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendProjectInviteMessage(String projectId) {
        // Get the current user's ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");

        // Create a new message object
        Message message = new Message();
        message.setSender(db.collection("users").document(currentUserId));
        message.setMessage_type(Message.MessageType.PROJECT_INVITE);
        message.setContent(projectId);
        message.setTimestamp(Timestamp.now());
        message.setId(generateMessageId());

        // Add the message to the messagesCollection
        messagesCollection.document(message.getId()).set(message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Project invite message sent successfully");
                        messageInput.setText("");
                        onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), firebaseAuth.getCurrentUser().getDisplayName() + getString(R.string.sent_a_project_invite), message.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error sending project invite message", e);
                        Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void onMessageSent(String senderName, String displayedContent, String messageId) {
        String projectName = project.getTitle();

        // Retrieve all the members of the project
        firebaseFirestore.collection("projects").document(projectId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String, Map<String, Object>> members = (Map<String, Map<String, Object>>) documentSnapshot.get("members"); // Assuming 'members' field in project document
                        members.put(project.getOwner().getId(), null); // This is to also include the owner in the members list.
                        if (members != null) {
                            for (String id : members.keySet()) {
                                // Skip sending notification to the sender
                                if (id.equals(userId)) {
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


    @Override
    public void onMessageLongClicked(View view, Message message) {
        if (message != null && (message.getMessage_type() != Message.MessageType.DATE_SEPARATOR && message.getMessage_type() != Message.MessageType.UNSENT)) {
            // Create and show a PopupMenu
            View menuItemView = findViewById(R.id.chat_recycler_view);
            PopupMenu popupMenu = new PopupMenu(new ContextThemeWrapper(this, R.style.PopupMenuStyle), view);
            if (message.getSender().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                popupMenu.inflate(R.menu.chat_context_menu);
            }else{
                popupMenu.inflate(R.menu.chat_context_menu_other);
            }

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_reply) {
                        // Implement the messageSenderName to retrieve the displayName from the referenced user in the getSender().

                        message.fetchSenderData().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String displayNameString = documentSnapshot.getString("displayName");
                                if (displayNameString != null && !displayNameString.isEmpty()) {
                                    enterReplyMode(message.getId(), displayNameString);
                                } else {
                                    Toast.makeText(ProjectChatActivity.this, R.string.replied_message_unavailable, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(ProjectChatActivity.this, R.string.replied_message_unavailable, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return true;
                    } else if (item.getItemId() == R.id.copy_message) {
                        // Copy the message to the clipboard
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Message", message.getContent());
                        clipboard.setPrimaryClip(clip);

                        Snackbar.make(menuItemView, R.string.message_copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
                        return true;
                    } else if (item.getItemId() == R.id.share) {
                        // Share the message content to other apps
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, message.getContent());
                        startActivity(Intent.createChooser(shareIntent, "Share message"));
                        return true;
                    }else if (item.getItemId() == R.id.unsend_message) {
                        long currentTime = System.currentTimeMillis();
                        long messageTime = message.getTimestamp().toDate().getTime();
                        long timeDifference = currentTime - messageTime;
                        long millisecondsIn24Hours = 24 * 60 * 60 * 1000;

                        if (timeDifference > millisecondsIn24Hours) {
                            Snackbar.make(menuItemView, R.string.cannot_unsend_messages_sent_more_than_24_hours_ago, Snackbar.LENGTH_SHORT).show();
                            return true;
                        }

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference messageDocRef = db.collection("chats").document(project.getUid()).collection("messages").document(message.getId());

                        messageDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Message messageToUpdate = document.toObject(Message.class);
                                        messageToUpdate.setMessage_type(Message.MessageType.UNSENT);
                                        messageToUpdate.setContent("message deleted");
                                        messageDocRef.set(messageToUpdate)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d(TAG, "Message successfully unsend.");
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.w(TAG, "Error unsend message", e);
                                                    }
                                                });
                                    }
                                } else {
                                    Log.d(TAG, "get failed with ", task.getException());
                                }
                            }
                        });
                        return true;
                    }

//                    }else if (item.getItemId() == R.id.unsend_message) {
//                        long currentTime = System.currentTimeMillis();
//                        long messageTime = message.getTimestamp().toDate().getTime();
//                        long timeDifference = currentTime - messageTime;
//                        long millisecondsIn24Hours = 24 * 60 * 60 * 1000;
//
//                        if (timeDifference > millisecondsIn24Hours) {
//                            Snackbar.make(menuItemView, R.string.cannot_unsend_messages_sent_more_than_24_hours_ago, Snackbar.LENGTH_SHORT).show();
//                            return true;
//                        }
//
//                        FirebaseFirestore db = FirebaseFirestore.getInstance();
//                        DocumentReference chatDocRef = db.collection("chats").document(project.getUid());
//                        chatDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                                if (task.isSuccessful()) {
//                                    DocumentSnapshot document = task.getResult();
//                                    if (document.exists()) {
//                                        List<Message> messages = document.toObject(ChatDocument.class).getMessages();
//                                        int messageIndex = -1;
//                                        for (int i = 0; i < messages.size(); i++) {
//                                            if (messages.get(i).getId().equals(message.getId())) {
//                                                messageIndex = i;
//                                                break;
//                                            }
//                                        }
//
//                                        if (messageIndex != -1) {
//                                            messages.get(messageIndex).setMessage_type(Message.MessageType.UNSENT);
//                                            messages.get(messageIndex).setContent("message deleted");
//                                            chatDocRef.update("messages", messages)
//                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                                        @Override
//                                                        public void onSuccess(Void aVoid) {
//                                                            Log.d(TAG, "Message successfully unsend.");
//                                                        }
//                                                    })
//                                                    .addOnFailureListener(new OnFailureListener() {
//                                                        @Override
//                                                        public void onFailure(@NonNull Exception e) {
//                                                            Log.w(TAG, "Error unsend message", e);
//                                                        }
//                                                    });
//                                        }
//                                    }
//                                } else {
//                                    Log.d(TAG, "get failed with ", task.getException());
//                                }
//                            }
//                        });
//                        return true;
//                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    }



    private void enterReplyMode(String repliedMessageId, String senderName) {
        replyMode = true;
        this.repliedMessageId = repliedMessageId;

        // Hide the attachFileButton, attachImageButton, and sendProjectInviteButton
        attachFileButton.setVisibility(View.GONE);
        attachImageButton.setVisibility(View.GONE);
        inviteProjectButton.setVisibility(View.GONE);

        // Show the reply mode layout
        replyModeLayout.setVisibility(View.VISIBLE);
        // Set the reply mode text ("Replying to 'Replied Sender Name' 's message")
        replyModeText.setText(getString(R.string.replying_to) + senderName + getString(R.string.s_message));
    }

    public void exitReplyMode(View view) {
        replyMode = false;
        repliedMessageId = null;

        // Show the attachFileButton, attachImageButton, and sendProjectInviteButton
        attachFileButton.setVisibility(View.VISIBLE);
        attachImageButton.setVisibility(View.VISIBLE);
        inviteProjectButton.setVisibility(View.VISIBLE);

        // Hide the reply mode layout
        replyModeLayout.setVisibility(View.GONE);
    }

//    private void sendReplyMessage(String repliedMessageId) {
//        String messageContent = messageInput.getText().toString().trim();
//
//        if (!TextUtils.isEmpty(messageContent)) {
//            // Get the current user's ID
//            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//            FirebaseFirestore db = FirebaseFirestore.getInstance();
//            DocumentReference chatDocument = db.collection("chats").document(project.getUid());
//
//            // Create a new message object
//            Message message = new Message();
//            message.setSender(db.collection("users").document(currentUserId));
//            message.setMessage_type(Message.MessageType.REPLY);
//            message.setContent(messageContent);
//            message.setTimestamp(Timestamp.now());
//            message.setId(generateMessageId());
//            message.setReplyingTo(repliedMessageId);
//            mentionedUserIds = getMentionsFromEditable(messageInput.getText());
//            message.setMentions(mentionedUserIds);
//
//            // Add the message to the chatDocument
//            chatDocument.update("messages", FieldValue.arrayUnion(message))
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            Log.d(TAG, "Reply message sent successfully");
//                            messageInput.setText("");
//                            exitReplyMode(null);
//                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Log.w(TAG, "Error sending reply message", e);
//                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_reply_message, Toast.LENGTH_SHORT).show();
//                        }
//                    });
//        } else {
//            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
//        }
//    }

    private void sendReplyMessage(String repliedMessageId) {
        String messageContent = messageInput.getText().toString().trim();

        if (!TextUtils.isEmpty(messageContent)) {
            // Get the current user's ID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");

            // Create a new message object
            Message message = new Message();
            message.setSender(db.collection("users").document(currentUserId));
            message.setMessage_type(Message.MessageType.REPLY);
            message.setContent(messageContent);
            message.setTimestamp(Timestamp.now());
            message.setId(generateMessageId());
            message.setReplyingTo(repliedMessageId);
            mentionedUserIds = getMentionsFromEditable(messageInput.getText());
            message.setMentions(mentionedUserIds);

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(message)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Reply message sent successfully");
                            messageInput.setText("");
                            exitReplyMode(null);
                            onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error sending reply message", e);
                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_reply_message, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
        }
    }


    private void sendMessage() {
        if (replyMode) {
            sendReplyMessage(repliedMessageId);
        } else {
            String messageContent = messageInput.getText().toString().trim();

            if (!TextUtils.isEmpty(messageContent)) {
                // Get the current user's ID
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference messagesCollection = db.collection("chats").document(project.getUid()).collection("messages");

                // Create a new message object
                Message message = new Message();
                message.setSender(db.collection("users").document(currentUserId));
                message.setMessage_type(Message.MessageType.TEXT);
                message.setContent(messageContent);
                message.setTimestamp(Timestamp.now());
                message.setId(generateMessageId());
                mentionedUserIds = getMentionsFromEditable(messageInput.getText());
                message.setMentions(mentionedUserIds);

                // Add the message to the messagesCollection
                messagesCollection.document(message.getId()).set(message)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Message sent successfully");
                                messageInput.setText("");
                                onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error sending message", e);
                                Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
            }
        }
        mentionedUserIds.clear();
    }



//    private void sendMessage() {
//        if (replyMode) {
//            sendReplyMessage(repliedMessageId);
//        } else {
//            String messageContent = messageInput.getText().toString().trim();
//
//            if (!TextUtils.isEmpty(messageContent)) {
//                // Get the current user's ID
//                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//                FirebaseFirestore db = FirebaseFirestore.getInstance();
//                DocumentReference chatDocument = db.collection("chats").document(project.getUid());
//
//                // Create a new message object
//                Message message = new Message();
//                message.setSender(db.collection("users").document(currentUserId));
//                message.setMessage_type(Message.MessageType.TEXT);
//                message.setContent(messageContent);
//                message.setTimestamp(Timestamp.now());
//                message.setId(generateMessageId());
//                mentionedUserIds = getMentionsFromEditable(messageInput.getText());
//                message.setMentions(mentionedUserIds);
//
//                // Add the message to the chatDocument
//                chatDocument.update("messages", FieldValue.arrayUnion(message))
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                Log.d(TAG, "Message sent successfully");
//                                messageInput.setText("");
//                                onMessageSent(firebaseAuth.getCurrentUser().getDisplayName(), message.getContent(), message.getId());
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "Error sending message", e);
//                                Toast.makeText(ProjectChatActivity.this, R.string.failed_to_send_message, Toast.LENGTH_SHORT).show();
//                            }
//                        });
//            } else {
//                Toast.makeText(this, R.string.please_enter_a_message, Toast.LENGTH_SHORT).show();
//            }
//        }
//        mentionedUserIds.clear();
//    }

    private void uploadFileToFirebaseStorage(Uri fileUri, String folderName, String fileName) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(folderName).child(System.currentTimeMillis() + "_" + fileUri.getLastPathSegment());

        // Create custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_progress, null);

        // Find views in the custom layout
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView textView = dialogView.findViewById(R.id.textView);
        textView.setText(R.string.uploading_please_wait);

        // Create alert dialog
        AlertDialog alertDialog = new AlertDialog.Builder(ProjectChatActivity.this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Show the dialog
        alertDialog.show();

        UploadTask uploadTask = storageReference.putFile(fileUri);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                int currentProgress = (int) (100 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                progressBar.setProgress(currentProgress);
            }
        });

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                alertDialog.dismiss();
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        if (folderName.equals("files")) {
                            sendFileMessage(fileName, uri.toString());
                        } else {
                            sendImageMessage(fileName, uri.toString());
                        }
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                alertDialog.dismiss();
                Toast.makeText(ProjectChatActivity.this, getString(R.string.failed_to_upload) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setActiveChat(boolean isActive) {
        firebaseFirestore.collection("activeChats").document(userId)
                .set(isActive ? Collections.singletonMap("projectId", projectId) : new HashMap<>());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isInActivity = true;
        setActiveChat(true);
        fetchAndMarkAllMessagesAsRead();

        FirebaseFirestore.getInstance().collection("projects").document(getIntent().getStringExtra("projectId")).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(!documentSnapshot.exists()){
                    finish();
                }else{
                    DocumentReference ownerRef = documentSnapshot.getDocumentReference("owner");
                    HashMap<String, Map<String, Object>> membersMap = (HashMap<String, Map<String, Object>>) documentSnapshot.get("members");
                    if(!ownerRef.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) && !membersMap.containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        finish();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isInActivity = false;
        setActiveChat(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setActiveChat(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        setActiveChat(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
