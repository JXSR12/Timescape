package edu.bluejack22_2.timescape2;

import static android.content.ContentValues.TAG;

import static edu.bluejack22_2.timescape2.ui.chats.ChatAdapter.isUserAtBottom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
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
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import edu.bluejack22_2.timescape2.model.Message;
import edu.bluejack22_2.timescape2.model.Project;
import edu.bluejack22_2.timescape2.model.User;
import edu.bluejack22_2.timescape2.ui.chats.ChatAdapter;

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
    ImageButton muteButton;
    ImageButton liveButton;
    RelativeLayout replyModeLayout;
    RelativeLayout newMessageLayout;
    TextView replyModeText;

    int onlineCount = 0;
    int typingCount = 0;

    private boolean replyMode;
    private String repliedMessageId;

    private MemberAdapter memberAdapter;
    private boolean firstLoadChat = true;

    private AlertDialog waitCamDialog;

    private boolean receiveCameraCallback = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_chat);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageInput = findViewById(R.id.message_input);

        sendMessageButton = findViewById(R.id.send_message_button);
        sendMessageButton.setVisibility(View.GONE);

        attachFileButton = findViewById(R.id.attach_file_button);
        attachImageButton = findViewById(R.id.attach_image_button);
        inviteProjectButton = findViewById(R.id.send_project_invite_button);
        replyModeLayout = findViewById(R.id.reply_mode_layout);
        replyModeText = findViewById(R.id.reply_mode_text);

        newMessageLayout = findViewById(R.id.new_message_layout);
        newMessageLayout.setVisibility(View.GONE);

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
            muteButton = customActionBarView.findViewById(R.id.mute_button);
            liveButton = customActionBarView.findViewById(R.id.live_button);
            checkMuteStatusAndUpdateButton();
            muteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DocumentReference mutedChatRef = db.collection("settings").document(userId)
                            .collection("mutedChats").document(projectId);

                    mutedChatRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Chat is currently muted, so we unmute it.
                                    mutedChatRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(ProjectChatActivity.this, R.string.chat_notifications_enabled, Toast.LENGTH_SHORT).show();
                                            muteButton.setImageResource(R.drawable.outline_notifications_24);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_enable_notifications, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // Chat is not currently muted, so we mute it.
                                    mutedChatRef.set(new HashMap<>()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Toast.makeText(ProjectChatActivity.this, R.string.chat_notifications_disabled, Toast.LENGTH_SHORT).show();
                                            muteButton.setImageResource(R.drawable.outline_notifications_off_24);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ProjectChatActivity.this, R.string.failed_to_disable_notifications, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(ProjectChatActivity.this, R.string.error_checking_notification_setting, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            liveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkCameraAvailabilityAndInitialize();
                }
            });

            // Set the custom view for the ActionBar
            actionBar.setCustomView(customActionBarView);
            actionBar.setDisplayShowCustomEnabled(true);
        }

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
                PopupMenu popupMenu = new PopupMenu(ProjectChatActivity.this, attachImageButton);
                popupMenu.getMenuInflater().inflate(R.menu.attach_menu, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getItemId() == R.id.photo_item){
                            pickImage();
                            return true;
                        }else if (item.getItemId() == R.id.video_item){
                            pickVideo();
                            return true;
                        }
                        return false;
                    }
                });

                popupMenu.show();
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

                if (beforeText.length() > inputText.length() && !inputText.isEmpty()) { // Backspace detected
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

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this implementation
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if the input text is empty
                boolean isInputEmpty = s.toString().trim().isEmpty();

                // Update the visibility of the sendMessageButton
                if (isInputEmpty) {
                    sendMessageButton.setVisibility(View.GONE);
                } else {
                    sendMessageButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed for this implementation
            }
        });

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Check if the user is at the bottom of the recyclerView
                boolean isUserAtBottom = isUserAtBottom(recyclerView, false);

                // Set the visibility of newMessageLayout based on the scroll position
                if (isUserAtBottom) {
                    for(int i = 0; i <= chatAdapter.getUnreadBelowCount(); i++){
                        markMessageAsReadIfVisible(chatAdapter.getMessageAt(chatAdapter.getLastItemPosition() - i), true);
                    }
                    chatAdapter.setUnreadBelowCount(0);
                    newMessageLayout.setVisibility(View.GONE);
                }
            }
        });

        getProjectMembers(projectId);
        initTypingStatusListener();
    }

    private void launchLiveRoom(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String projectId = project.getUid();
        DocumentReference liveRoomRef = db.collection("liveRooms").document(projectId);

        liveRoomRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String activeChannelId = document.getString("activeChannelId");
                        if (activeChannelId == null || activeChannelId.isEmpty()) {
                            createAndJoinNewChannel(liveRoomRef);
                        } else {
                            joinExistingChannel(activeChannelId);
                        }
                    } else {
                        createAndJoinNewChannel(liveRoomRef);
                    }
                } else {
                    Log.d(TAG, "Failed to get live room: ", task.getException());
                }
            }
        });
    }

    private void checkCameraAvailabilityAndInitialize() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.joining_live_room)
                .setMessage(R.string.waiting_for_device_camera_to_be_ready);

        ProgressBar progressBar = new ProgressBar(this);
        builder.setView(progressBar);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                // Handle cancellation
                receiveCameraCallback = false;
                cameraManager.unregisterAvailabilityCallback(cameraAvailabilityCallback);
            }
        });

        waitCamDialog = builder.create();
        waitCamDialog.show();

        cameraManager.registerAvailabilityCallback(cameraAvailabilityCallback, new Handler(Looper.getMainLooper()));
        receiveCameraCallback = true;
    }

    CameraManager.AvailabilityCallback cameraAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            if(!receiveCameraCallback) return;

            if (waitCamDialog != null) {
                waitCamDialog.dismiss();
            }
            waitCamDialog.dismiss();
            receiveCameraCallback = false;
            ((CameraManager) getSystemService(Context.CAMERA_SERVICE)).unregisterAvailabilityCallback(cameraAvailabilityCallback);

            launchLiveRoom();
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);

            AlertDialog.Builder builder = new AlertDialog.Builder(ProjectChatActivity.this);
            builder.setTitle(R.string.camera_unavailable)
                    .setMessage(R.string.your_device_camera_is_inaccessible_at_the_moment_try_re_joining_or_you_can_continue_joining_without_camera)
                    .setCancelable(true)
                    .setPositiveButton(R.string.continue_str, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            receiveCameraCallback = false;
                            ((CameraManager) getSystemService(Context.CAMERA_SERVICE)).unregisterAvailabilityCallback(cameraAvailabilityCallback);
                            launchLiveRoom();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            ((CameraManager) getSystemService(Context.CAMERA_SERVICE)).unregisterAvailabilityCallback(cameraAvailabilityCallback);
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }

    };

    private void createAndJoinNewChannel(DocumentReference liveRoomRef) {
        String newChannelId = UUID.randomUUID().toString();

        liveRoomRef.set(new HashMap<String, Object>() {{
            put("activeChannelId", newChannelId);
        }});

        // start ProjectLiveActivity with new channel ID
        Intent intent = new Intent(ProjectChatActivity.this, ProjectLiveActivity.class);
        intent.putExtra("channelId", newChannelId);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    private void joinExistingChannel(String channelId) {
        // start ProjectLiveActivity with existing channel ID
        Intent intent = new Intent(ProjectChatActivity.this, ProjectLiveActivity.class);
        intent.putExtra("channelId", channelId);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }


    private void checkMuteStatusAndUpdateButton() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference mutedChatRef = db.collection("settings").document(userId)
                .collection("mutedChats").document(projectId);

        mutedChatRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Chat is currently muted
                        muteButton.setImageResource(R.drawable.outline_notifications_off_24);
                    } else {
                        // Chat is not currently muted
                        muteButton.setImageResource(R.drawable.outline_notifications_24);
                    }
                } else {
                    Log.e(TAG, "Error checking mute status", task.getException());
                }
            }
        });
    }


    private void insertMention(EditText messageInput, String mentionDisplayName, String mentionId) {
        SpannableString mentionSpannable = new SpannableString(mentionDisplayName + " ");

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
                boolean lastMessageSelf = false;
                if (snapshots != null) {
                    for (QueryDocumentSnapshot document : snapshots) {
                        Message message = document.toObject(Message.class);
                        // Check if the last message is not from the current user and mark it as read if visible
                        if (document.getId().equals(snapshots.getDocuments().get(snapshots.size()-1).getId())) {
                            if (!message.getSender().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                                markMessageAsReadIfVisible(message, false);
                                lastMessageSelf = false;
                            }else{
                                lastMessageSelf = true;
                            }
                        }

                        messages.add(message);
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }

                chatAdapter.setMessages(messages, chatRecyclerView, newMessageLayout, lastMessageSelf);

                if(firstLoadChat){
                    chatRecyclerView.scrollToPosition(chatAdapter.getLastItemPosition());
                    firstLoadChat = false;
                }
            }
        });


    }

    private void markMessageAsReadIfVisible(Message message, boolean ignoreVisiblity) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) chatRecyclerView.getLayoutManager();
        if (layoutManager != null && isInActivity) {
            int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
            int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
            int lastMessagePosition = chatAdapter.getItemCount() - 1;

            // Check if the last message is visible
            if (ignoreVisiblity || (lastMessagePosition >= firstVisibleItemPosition && lastMessagePosition <= lastVisibleItemPosition)) {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                ArrayList<String> reads = (ArrayList<String>) message.getReads();

                if (reads == null) {
                    reads = new ArrayList<>();
                }

                // Check if the message has not been read by the current user
                if (!reads.contains(currentUserId) && message.getSender() != null && !message.getSender().getId().equals(currentUserId)) {
                    reads.add(currentUserId);
                    message.setReads(reads);

                    // Update message read status in your database
                    updateMessageReadStatus(project.getUid(), message);
                }
            }
        }
    }

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
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
            Uri sourceUri = result.getData().getData();
            startImageEditingActivity(sourceUri);
        }
    });

    private final ActivityResultLauncher<Intent> pickVideoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
            Uri sourceUri = result.getData().getData();
            if (sourceUri != null) {
                String fileName = getFileNameFromUri(getApplicationContext(), sourceUri);
                uploadFileToFirebaseStorage(sourceUri, "videos", fileName);
            }
        }
    });

    private final ActivityResultLauncher<Intent> editImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri resultUri = result.getData().getData();
            if (resultUri != null) {
                String fileName = getFileNameFromUri(getApplicationContext(), resultUri);
                uploadFileToFirebaseStorage(resultUri, "images", fileName);
            }
        }
    });

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        pickVideoLauncher.launch(intent);
    }

    private void startImageEditingActivity(Uri sourceUri) {
        String originalFileName = getFileNameFromUri(this, sourceUri);
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String croppedFileName = originalFileName.substring(0, originalFileName.lastIndexOf(".")) + "-edited" + extension;
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), croppedFileName));

        Intent intent = new Intent(ProjectChatActivity.this, ImageEditingActivity.class);
        intent.putExtra("sourceUri", sourceUri.toString());
        intent.putExtra("destinationUri", destinationUri.toString());
        editImageLauncher.launch(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            if (data != null) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    String fileName = getFileNameFromUri(getApplicationContext(), resultUri);
                    uploadFileToFirebaseStorage(resultUri, "images", fileName);
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }
    }


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
            message.setId(generateMessageId());
            message.setFileName(fileName);

            // Create a map of the message to add the server timestamp
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("id", message.getId());
            messageMap.put("sender", message.getSender());
            messageMap.put("message_type", message.getMessage_type().toString());
            messageMap.put("content", message.getContent());
            messageMap.put("timestamp", FieldValue.serverTimestamp()); // set to server timestamp
            messageMap.put("fileName", message.getFileName());
            // add other fields as necessary

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(messageMap)
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
            message.setId(generateMessageId());
            message.setFileName(fileFullName);

            // Create a map of the message to add the server timestamp
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("id", message.getId());
            messageMap.put("sender", message.getSender());
            messageMap.put("message_type", message.getMessage_type().toString());
            messageMap.put("content", message.getContent());
            messageMap.put("timestamp", FieldValue.serverTimestamp()); // set to server timestamp
            messageMap.put("fileName", message.getFileName());
            // add other fields as necessary

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(messageMap)
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
        message.setId(generateMessageId());

        // Create a map of the message to add the server timestamp
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("id", message.getId());
        messageMap.put("sender", message.getSender());
        messageMap.put("message_type", message.getMessage_type().toString());
        messageMap.put("content", message.getContent());
        messageMap.put("timestamp", FieldValue.serverTimestamp()); // set to server timestamp
        // add other fields as necessary

        // Add the message to the messagesCollection
        messagesCollection.document(message.getId()).set(messageMap)
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

                                // Check if the member has muted the chat
                                firebaseFirestore.collection("settings").document(id)
                                        .collection("mutedChats").document(projectId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                if (documentSnapshot.exists()) {
                                                    // Chat is muted, skip notification
                                                    return;
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
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)));
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
                                        messageToUpdate.setContent(getString(R.string.message_deleted));
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
                    }else if (item.getItemId() == R.id.view_reads) {
                        // Create an alert dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(ProjectChatActivity.this);
                        builder.setTitle(R.string.has_been_read_by);

                        // Create the adapter and set it to the ListView of the AlertDialog
                        List<String> readers = message.getReads();
                        if(readers == null) readers = new ArrayList<>();
                        ReadersAdapter adapter = new ReadersAdapter(ProjectChatActivity.this, readers);
                        builder.setAdapter(adapter, null);

                        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

                        AlertDialog dialog = builder.create();
                        if(readers.size() == 0){
                            dialog.setMessage(getString(R.string.no_reads_yet));
                        }
                        dialog.show();

                        return true;
                    }

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

    private void sendReplyMessage(String repliedMessageId) {
        String messageContent = messageInput.getText().toString().trim();
        Editable inputEditable = messageInput.getEditableText();
        messageInput.setText("");
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
            message.setId(generateMessageId());
            message.setReplyingTo(repliedMessageId);
            mentionedUserIds = getMentionsFromEditable(inputEditable);
            message.setMentions(mentionedUserIds);

            // Create a map of the message to add the server timestamp
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("id", message.getId());
            messageMap.put("sender", message.getSender());
            messageMap.put("message_type", message.getMessage_type().toString());
            messageMap.put("content", message.getContent());
            messageMap.put("timestamp", FieldValue.serverTimestamp()); // set to server timestamp
            messageMap.put("replyingTo", message.getReplyingTo());
            messageMap.put("mentions", message.getMentions());
            // add other fields as necessary

            // Add the message to the messagesCollection
            messagesCollection.document(message.getId()).set(messageMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Reply message sent successfully");
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

    public String convertWithIteration(Map<String, ?> map) {
        StringBuilder mapAsString = new StringBuilder("{");
        for (String key : map.keySet()) {
            mapAsString.append(key + "=" + map.get(key) + ", ");
        }
        mapAsString.delete(mapAsString.length()-2, mapAsString.length()).append("}");
        return mapAsString.toString();
    }

    private void sendMessage() {
        if (replyMode) {
            sendReplyMessage(repliedMessageId);
        } else {
            String messageContent = messageInput.getText().toString().trim();
            Editable inputEditable = messageInput.getEditableText();
            messageInput.setText("");

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
                // We're not setting the timestamp here, it will be set in Firestore.
                message.setId(generateMessageId());
                mentionedUserIds = getMentionsFromEditable(inputEditable);
                message.setMentions(mentionedUserIds);


                // Create a map of the message to add the server timestamp
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("id", message.getId());
                messageMap.put("sender", message.getSender());
                messageMap.put("message_type", message.getMessage_type().toString());
                messageMap.put("content", message.getContent());
                messageMap.put("timestamp", FieldValue.serverTimestamp()); // set to server timestamp
                messageMap.put("mentions", message.getMentions());
                // add other fields as necessary

                // Add the message to the messagesCollection
                messagesCollection.document(message.getId()).set(messageMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Message sent successfully");
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

    public void scrollToBottom(View view) {
        chatRecyclerView.scrollToPosition(chatAdapter.getLastItemPosition());
        newMessageLayout.setVisibility(View.GONE);
    }
}
