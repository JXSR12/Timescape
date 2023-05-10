package edu.bluejack22_2.timescape.ui.chats;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import edu.bluejack22_2.timescape.FullScreenImageActivity;
import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Chat;
import edu.bluejack22_2.timescape.model.Message;
import edu.bluejack22_2.timescape.ui.RoundedImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private Context context;
    private FirebaseAuth firebaseAuth;

    private RecyclerView recyclerView;

    public interface MessageLongClickListener {
        void onMessageLongClicked(View view, Message message);
    }

    public Message getMessageAt(int index){
        return messages.get(index);
    }
    private MessageLongClickListener messageLongClickListener;

    public ChatAdapter(Context context, FirebaseAuth firebaseAuth, MessageLongClickListener messageLongClickListener, RecyclerView recyclerView) { // Modify this line
        this.context = context;
        this.firebaseAuth = firebaseAuth; // Add this line
        this.messageLongClickListener = messageLongClickListener;
        this.recyclerView = recyclerView;
    }


    public void setMessages(List<Message> messages, RecyclerView recyclerView) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        } else {
            this.messages.clear();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("E, MMM dd, yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        String todayDate = dateFormat.format(calendar.getTime());
        calendar.add(Calendar.DATE, -1);
        String yesterdayDate = dateFormat.format(calendar.getTime());

        // Initialize a dummy message with an invalid timestamp
        Message previousMessage = new Message();
        previousMessage.setTimestamp(new Timestamp(0, 0));

        for (Message message : messages) {
            // If the previous message has a different date from the current message, add a date separator
            if (isDifferentDate(previousMessage, message)) {
                Message dateSeparator = new Message();
                dateSeparator.setMessage_type(Message.MessageType.DATE_SEPARATOR);

                String msgDate = dateFormat.format(message.getTimestampAsDate());
                if (msgDate.equals(todayDate)) {
                    dateSeparator.setContent("Today");
                } else if (msgDate.equals(yesterdayDate)) {
                    dateSeparator.setContent("Yesterday");
                } else {
                    dateSeparator.setContent(msgDate);
                }

                this.messages.add(dateSeparator);
            }

            // Add the current message
            this.messages.add(message);

            // Update previousMessage
            previousMessage = message;
        }

        notifyDataSetChanged();
        recyclerView.scrollToPosition(getLastItemPosition());
    }


    private boolean isDifferentDate(Message msg1, Message msg2) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String msg1Date = formatter.format(msg1.getTimestampAsDate());
        String msg2Date = formatter.format(msg2.getTimestampAsDate());

        return !msg1Date.equals(msg2Date);
    }

    private boolean isSameSenderAndMinute(Message message1, Message message2) {
        if (message1.getSender() == null || message2.getSender() == null) {
            return false;
        }

        boolean sameSender = message1.getSender().getId().equals(message2.getSender().getId());
        long timeDiff = Math.abs(message1.getTimestampAsDate().getTime() - message2.getTimestampAsDate().getTime());
        boolean withinSameMinute = timeDiff < 60000; // 60 seconds * 1000 milliseconds

        return sameSender && withinSameMinute;
    }

    public int getLastItemPosition() {
        return messages == null ? 0 : messages.size() - 1;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChatViewHolder holder = (ChatViewHolder) v.getTag();
                if (holder != null && holder.repliedMessagePosition >= 0) {
                    scrollToPosition(holder.repliedMessagePosition);
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isCurrentUser = message.getMessage_type() != Message.MessageType.DATE_SEPARATOR && message.getSender().getId().equals(firebaseAuth.getCurrentUser().getUid());
        holder.bind(message, isCurrentUser, position);
        if(message.getMessage_type() == Message.MessageType.REPLY){
            holder.repliedMessagePosition = findPositionOfRepliedMessage(message.getReplyingTo(), messages);
            holder.selfRepliedMessageContent.setTag(holder);
            holder.otherUserRepliedMessageContent.setTag(holder);
        }
//        Log.d("ChatAdapter", "Binding message at position " + position + ": " + message.getContent());
    }

    public void scrollToPosition(int position) {
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(position);
        }
    }


    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    private int findPositionOfRepliedMessage(String repliedMessageId, List<Message> messages) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) != null && messages.get(i).getId() != null && messages.get(i).getId().equals(repliedMessageId)) {
                return i;
            }
        }
        return -1;
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {

        LinearLayout chatItemRoot;

        TextView selfTextMessage;
        TextView otherUserTextMessage;
        RoundedImageView selfImageMessage;
        FrameLayout selfImageMessageFrame;
        FrameLayout otherUserImageMessageFrame;
        RoundedImageView otherUserImageMessage;
        LinearLayout selfFileAttachmentMessage;
        TextView selfFileAttachmentName;
        TextView selfFileAttachmentExtension;
        Button selfFileAttachmentDownloadButton;
        LinearLayout otherUserFileAttachmentMessage;
        TextView otherUserFileAttachmentName;
        TextView otherUserFileAttachmentExtension;
        Button otherUserFileAttachmentDownloadButton;
        TextView selfProjectInviteMessage;
        TextView otherUserProjectInviteMessage;
        TextView dateSeparator;
        TextView selfUnsentMessage;
        TextView otherUserUnsentMessage;
        ImageView avatar;
        TextView displayName;
        TextView timestamp;

        LinearLayout readAndTimestamp;
        LinearLayout otherUserReplyMessageLayout;
        ImageView otherUserRepliedAvatar;
        TextView otherUserRepliedName;
        TextView otherUserRepliedMessageContent;
        TextView otherUserReplyMessage;

        LinearLayout selfReplyMessageLayout;
        ImageView selfRepliedAvatar;
        TextView selfRepliedName;
        TextView selfRepliedMessageContent;
        TextView selfReplyMessage;

        TextView readCount;

        private int repliedMessagePosition;

        public ChatViewHolder(@NonNull View itemView, View.OnClickListener onReplyTopPartClicked) {
            super(itemView);
            chatItemRoot = itemView.findViewById(R.id.chat_item_root);

            selfTextMessage = itemView.findViewById(R.id.self_text_message);
            otherUserTextMessage = itemView.findViewById(R.id.other_user_text_message);
            selfImageMessage = itemView.findViewById(R.id.self_image_message);
            selfImageMessageFrame = itemView.findViewById(R.id.self_image_message_frame);
            otherUserImageMessage = itemView.findViewById(R.id.other_user_image_message);
            otherUserImageMessageFrame = itemView.findViewById(R.id.other_user_image_message_frame);
            selfFileAttachmentMessage = itemView.findViewById(R.id.self_file_attachment_message);
            selfFileAttachmentName = itemView.findViewById(R.id.self_file_attachment_name);
            selfFileAttachmentExtension = itemView.findViewById(R.id.self_file_attachment_extension);
            selfFileAttachmentDownloadButton = itemView.findViewById(R.id.self_file_attachment_download_button);
            otherUserFileAttachmentMessage = itemView.findViewById(R.id.other_user_file_attachment_message);
            otherUserFileAttachmentName = itemView.findViewById(R.id.other_user_file_attachment_name);
            otherUserFileAttachmentExtension = itemView.findViewById(R.id.other_user_file_attachment_extension);
            otherUserFileAttachmentDownloadButton = itemView.findViewById(R.id.other_user_file_attachment_download_button);
            selfProjectInviteMessage = itemView.findViewById(R.id.self_project_invite_message);
            otherUserProjectInviteMessage = itemView.findViewById(R.id.other_user_project_invite_message);
            dateSeparator = itemView.findViewById(R.id.date_separator);
            selfUnsentMessage = itemView.findViewById(R.id.unsent_message);
            otherUserUnsentMessage = itemView.findViewById(R.id.other_user_unsent_message);
            avatar = itemView.findViewById(R.id.avatar);
            displayName = itemView.findViewById(R.id.display_name);
            timestamp = itemView.findViewById(R.id.timestamp);
            readCount = itemView.findViewById(R.id.read_count);

            readAndTimestamp = itemView.findViewById(R.id.read_and_timestamp);

            otherUserReplyMessageLayout = itemView.findViewById(R.id.other_user_reply_message_layout);
            otherUserRepliedAvatar = itemView.findViewById(R.id.other_user_replied_avatar);
            otherUserRepliedName = itemView.findViewById(R.id.other_user_replied_name);
            otherUserRepliedMessageContent = itemView.findViewById(R.id.other_user_replied_message_content);
            otherUserReplyMessage = itemView.findViewById(R.id.other_user_reply_message);

            selfReplyMessageLayout = itemView.findViewById(R.id.self_reply_message_layout);
            selfRepliedAvatar = itemView.findViewById(R.id.self_replied_avatar);
            selfRepliedName = itemView.findViewById(R.id.self_replied_name);
            selfRepliedMessageContent = itemView.findViewById(R.id.self_replied_message_content);
            selfReplyMessage = itemView.findViewById(R.id.self_reply_message);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Message message = messages.get(getAdapterPosition());
                    messageLongClickListener.onMessageLongClicked(itemView, message);
                    return true;
                }
            });

            selfTextMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Message message = messages.get(getAdapterPosition());
                    messageLongClickListener.onMessageLongClicked(itemView, message);
                    return true;
                }
            });

            otherUserTextMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Message message = messages.get(getAdapterPosition());
                    messageLongClickListener.onMessageLongClicked(itemView, message);
                    return true;
                }
            });

            if (otherUserRepliedMessageContent != null) {
                otherUserRepliedMessageContent.setOnClickListener(onReplyTopPartClicked);
            }

            if (selfRepliedMessageContent != null) {
                selfRepliedMessageContent.setOnClickListener(onReplyTopPartClicked);
            }
        }

        public void bind(Message message, boolean isCurrentUser, int position) { // Modify this line
            resetVisibility();

            if(message.getMessage_type() != Message.MessageType.DATE_SEPARATOR){
                // Get the previous and next messages if available
                Message prevMessage = position > 0 ? messages.get(position - 1) : null;
                Message nextMessage = position < messages.size() - 1 ? messages.get(position + 1) : null;

                // Determine if we should show avatar and display name
                boolean showAvatarAndDisplayName = prevMessage == null || !isSameSenderAndMinute(prevMessage, message);

                // Determine if we should show the timestamp
                boolean showTimestamp = nextMessage == null || !isSameSenderAndMinute(message, nextMessage);

                setTimestamp(message, isCurrentUser, showTimestamp); // Pass showTimestamp as a parameter

                if (!isCurrentUser && showAvatarAndDisplayName) { // Check showAvatarAndDisplayName
                    showOtherUserAvatar(message);
                    showOtherUserDisplayName(message);
                }else if (!isCurrentUser){
                    showOtherUserAvatarEmpty(message);
                }
            }

            // Handle the display of different message types
            switch (message.getMessage_type()) {
                case TEXT:
                    handleTextMessage(message, isCurrentUser); // Modify this line
                    break;
                case IMAGE:
                    handleImageMessage(message, isCurrentUser); // Modify this line
                    break;
                case FILE:
                    handleFileAttachmentMessage(message, isCurrentUser); // Modify this line
                    break;
                case PROJECT_INVITE:
                    handleProjectInviteMessage(message, isCurrentUser); // Modify this line
                    break;
                case DATE_SEPARATOR:
                    handleDateSeparator(message);
                    break;
                case UNSENT:
                    handleUnsentMessage(message, isCurrentUser);
                    break;
                case REPLY:
                    handleReplyMessage(message, isCurrentUser);
                    break;
            }
        }

        private void setTimestamp(Message message, boolean isCurrentUser, boolean showTimestamp) {
            timestamp.setVisibility(showTimestamp || isCurrentUser ? View.VISIBLE : View.GONE);
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String formattedTimestamp = formatter.format(message.getTimestampAsDate());
            timestamp.setText(formattedTimestamp);

            // Update the alignment based on the isCurrentUser flag
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) timestamp.getLayoutParams();
            LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) readAndTimestamp.getLayoutParams();
            if (isCurrentUser) {
                layoutParams.gravity = Gravity.END;
                layoutParams2.gravity = Gravity.END;
            } else {
                layoutParams.gravity = Gravity.START;
                layoutParams2.gravity = Gravity.START;
            }
            timestamp.setLayoutParams(layoutParams);
        }

        private void showOtherUserAvatar(Message message) {
            avatar.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) avatar.getLayoutParams();

            float dpValue = 32f;
            float pixelValue = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
            params.height = (int) pixelValue;

            avatar.setLayoutParams(params);
            avatar.setImageDrawable(generateAvatar(message.getSender().getId()));
        }

        private void showOtherUserAvatarEmpty(Message message) {
            avatar.setVisibility(View.INVISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) avatar.getLayoutParams();
            params.height = 1;
            avatar.setLayoutParams(params);
        }

        private TextDrawable generateAvatar(String userId) {
            String content = "\\O/";
            Random random = new Random(userId.hashCode());
            int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
            return TextDrawable.builder().buildRound(content, color);
        }

        private void showOtherUserDisplayName(Message message) {
            displayName.setVisibility(View.VISIBLE);
            message.fetchSenderData().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayNameString = documentSnapshot.getString("displayName");
                    if (displayNameString != null && !displayNameString.isEmpty()) {
                        displayName.setText(displayNameString);
                    } else {
                        displayName.setText(R.string.unknown_user);
                    }
                } else {
                    displayName.setText(R.string.unknown_user);
                }
            });
        }


        private void handleTextMessage(Message message, boolean isCurrentUser) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            HashMap<String, String> mentions = message.getMentions();
            List<String> reads = message.getReads();

            Log.d("Checking for mentions", mentions != null ? "Mentions count: " + mentions.size() : "No mentions");

            if (mentions != null && mentions.containsValue(currentUserId)) {
                chatItemRoot.setBackgroundColor(ContextCompat.getColor(context, R.color.mention_highlight)); // Semi-transparent yellow
                Log.d("Mentioned Message", "This comes from a message mentioning you");
            } else {
                chatItemRoot.setBackgroundColor(Color.TRANSPARENT);
            }

            if (isCurrentUser) {
                selfTextMessage.setVisibility(View.VISIBLE);
                selfTextMessage.setText(message.getContent());
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText("Read " + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }

            } else {
                otherUserTextMessage.setVisibility(View.VISIBLE);
                otherUserTextMessage.setText(message.getContent());
            }
        }


        private void handleImageMessage(Message message, boolean isCurrentUser) {
            if (isCurrentUser) {
                selfImageMessageFrame.setVisibility(View.VISIBLE);
                // Load the image URL into the ImageView using an image loading library like Glide
                Glide.with(itemView.getContext()).load(message.getContent()).into(selfImageMessage);

                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText("Read " + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }

                selfImageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent fullScreenIntent = new Intent(context, FullScreenImageActivity.class);
                        fullScreenIntent.putExtra("image_url", message.getContent());
                        fullScreenIntent.putExtra("file_name", message.getFileName() != null ? message.getFileName() : "Untitled Image");
                        context.startActivity(fullScreenIntent);
                    }
                });

            } else {
                otherUserImageMessageFrame.setVisibility(View.VISIBLE);
                // Load the image URL into the ImageView using an image loading library like Glide
                Glide.with(itemView.getContext()).load(message.getContent()).into(otherUserImageMessage);

                otherUserImageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent fullScreenIntent = new Intent(context, FullScreenImageActivity.class);
                        fullScreenIntent.putExtra("image_url", message.getContent());
                        fullScreenIntent.putExtra("file_name", message.getFileName() != null ? message.getFileName() : "Untitled Image");
                        context.startActivity(fullScreenIntent);
                    }
                });
            }

        }

        private void handleFileAttachmentMessage(Message message, boolean isCurrentUser) {
            if (isCurrentUser) {
                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText("Read " + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }
                selfFileAttachmentMessage.setVisibility(View.VISIBLE);
                selfFileAttachmentName.setText(message.getFileName());
                String fullExt = message.getFileName().substring(message.getFileName().lastIndexOf(".") + 1);
                selfFileAttachmentExtension.setText(fullExt.substring(0, Math.min(fullExt.length(), 3)).toUpperCase());
                selfFileAttachmentDownloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selfFileAttachmentDownloadButton.setVisibility(View.GONE);
                        ProgressBar progressBar = itemView.findViewById(R.id.self_file_attachment_progress_bar);
                        progressBar.setVisibility(View.VISIBLE);

                        String fileUrl = message.getContent();
                        String fileName = message.getFileName();
                        File localFile;

                        try {
                            String appName = context.getString(R.string.app_name);
                            File downloadsFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), appName);
                            if (!downloadsFolder.exists()) {
                                downloadsFolder.mkdirs();
                            }
                            localFile = new File(downloadsFolder, fileName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Error creating local file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                            return;
                        }

                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);
                        storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                try {
                                    String appName = context.getString(R.string.app_name);
                                    String fileNameWithExtension = message.getFileName();

                                    ContentValues values = new ContentValues();
                                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileNameWithExtension);
                                    String fileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf(".") + 1);
                                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
                                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + appName);

                                    ContentResolver resolver = context.getContentResolver();
                                    Uri uri = null;
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                                    }

                                    if (uri != null) {
                                        InputStream inputStream = Files.newInputStream(localFile.toPath());
                                        OutputStream outputStream = resolver.openOutputStream(uri);

                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, bytesRead);
                                        }
                                        inputStream.close();
                                        outputStream.close();
                                    }

                                    progressBar.setVisibility(View.GONE);
                                    selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                    Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                                    Log.d("Download", "File downloaded at: " + uri.toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, "Error saving the file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressBar.setVisibility(View.GONE);
                                selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                Toast.makeText(context, "Failed to download file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                progressBar.setProgress((int) Math.floor(progress));
                            }
                        });

                    }
                });
            } else {
                otherUserFileAttachmentMessage.setVisibility(View.VISIBLE);
                otherUserFileAttachmentName.setText(message.getFileName());
                String fullExt = message.getFileName().substring(message.getFileName().lastIndexOf(".") + 1);
                otherUserFileAttachmentExtension.setText(fullExt.substring(0, Math.min(fullExt.length(), 3)).toUpperCase());
                otherUserFileAttachmentDownloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        otherUserFileAttachmentDownloadButton.setVisibility(View.GONE);
                        ProgressBar progressBar = itemView.findViewById(R.id.other_user_file_attachment_progress_bar);
                        progressBar.setVisibility(View.VISIBLE);

                        String fileUrl = message.getContent();
                        String fileName = message.getFileName();
                        File localFile;

                        try {
                            String appName = context.getString(R.string.app_name);
                            File downloadsFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), appName);
                            if (!downloadsFolder.exists()) {
                                downloadsFolder.mkdirs();
                            }
                            localFile = new File(downloadsFolder, fileName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Error creating local file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            otherUserFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                            return;
                        }

                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);
                        storageReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                try {
                                    String appName = context.getString(R.string.app_name);
                                    String fileNameWithExtension = message.getFileName();

                                    ContentValues values = new ContentValues();
                                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileNameWithExtension);
                                    String fileExtension = fileNameWithExtension.substring(fileNameWithExtension.lastIndexOf(".") + 1);
                                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
                                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);

                                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + appName);

                                    ContentResolver resolver = context.getContentResolver();
                                    Uri uri = null;
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                                    }

                                    if (uri != null) {
                                        InputStream inputStream = Files.newInputStream(localFile.toPath());
                                        OutputStream outputStream = resolver.openOutputStream(uri);

                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                                            outputStream.write(buffer, 0, bytesRead);
                                        }
                                        inputStream.close();
                                        outputStream.close();
                                    }

                                    progressBar.setVisibility(View.GONE);
                                    otherUserFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                    Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                                    Log.d("Download", "File downloaded at: " + uri.toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, "Error saving the file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    otherUserFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressBar.setVisibility(View.GONE);
                                otherUserFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                                Toast.makeText(context, "Failed to download file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                                progressBar.setProgress((int) Math.floor(progress));
                            }
                        });
                    }
                });
            }

        }

        private void handleProjectInviteMessage(Message message, boolean isCurrentUser) {
            if (isCurrentUser) {
                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText("Read " + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }
                selfProjectInviteMessage.setVisibility(View.VISIBLE);
                selfProjectInviteMessage.setText(message.getContent());
            } else {
                otherUserProjectInviteMessage.setVisibility(View.VISIBLE);
                otherUserProjectInviteMessage.setText(message.getContent());
            }
        }

        private void handleDateSeparator(Message message) {
            dateSeparator.setVisibility(View.VISIBLE);
            dateSeparator.setText(message.getContent());
        }

        private void handleUnsentMessage(Message message, boolean isCurrentUser) {
            String unsentText = "message deleted";
            if (isCurrentUser) {
                selfUnsentMessage.setVisibility(View.VISIBLE);
                selfUnsentMessage.setText(unsentText);
            }else{
                otherUserUnsentMessage.setVisibility(View.VISIBLE);
                otherUserUnsentMessage.setText(unsentText);
            }

        }

        private void handleReplyMessage(Message message, boolean isCurrentUser) {
            // Find the replied message in the message list
            Message repliedMessage = null;
            for (Message m : messages) {
                if (m != null && m.getId() != null && m.getId().equals(message.getReplyingTo())) {
                    repliedMessage = m;
                    break;
                }
            }

            // If the replied message is found, set the appropriate fields
            if (repliedMessage != null) {
                if (isCurrentUser) {
                    List<String> reads = message.getReads();
                    if (reads != null && !reads.isEmpty()) {
                        int readCountValue = reads.size();
                        readCount.setText("Read " + readCountValue + " • ");
                        readCount.setVisibility(View.VISIBLE);
                    } else {
                        readCount.setVisibility(View.GONE);
                    }
                    selfReplyMessageLayout.setVisibility(View.VISIBLE);
                    selfReplyMessage.setText(message.getContent());

                    selfRepliedAvatar.setImageDrawable(generateAvatar(repliedMessage.getSender().getId()));
                    showRepliedDisplayName(selfRepliedName, repliedMessage);

                    if(repliedMessage.getMessage_type() == Message.MessageType.FILE){
                        selfRepliedMessageContent.setText(R.string.sent_a_file);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.IMAGE){
                        selfRepliedMessageContent.setText(R.string.sent_an_image);
                    }else{
                        selfRepliedMessageContent.setText(repliedMessage.getContent());
                    }


                } else {
                    otherUserReplyMessageLayout.setVisibility(View.VISIBLE);
                    otherUserReplyMessage.setText(message.getContent());

                    otherUserRepliedAvatar.setImageDrawable(generateAvatar(repliedMessage.getSender().getId()));
                    showRepliedDisplayName(otherUserRepliedName, repliedMessage);

                    if(repliedMessage.getMessage_type() == Message.MessageType.FILE){
                        otherUserRepliedMessageContent.setText(R.string.sent_a_file);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.IMAGE){
                        otherUserRepliedMessageContent.setText(R.string.sent_an_image);
                    }else{
                        otherUserRepliedMessageContent.setText(repliedMessage.getContent());
                    }
                }
            }
        }

        private void showRepliedDisplayName(TextView repliedName, Message message) {
            message.fetchSenderData().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String displayNameString = documentSnapshot.getString("displayName");
                    if (displayNameString != null && !displayNameString.isEmpty()) {
                        repliedName.setText(displayNameString);
                    } else {
                        repliedName.setText(R.string.unknown_user);
                    }
                } else {
                    repliedName.setText(R.string.unknown_user);
                }
            });
        }


        private void resetVisibility() {
            chatItemRoot.setBackgroundColor(Color.TRANSPARENT);
            timestamp.setVisibility(View.GONE);
            readCount.setVisibility(View.GONE);
            displayName.setVisibility(View.GONE);
            avatar.setVisibility(View.GONE);
            selfTextMessage.setVisibility(View.GONE);
            otherUserTextMessage.setVisibility(View.GONE);
            selfImageMessageFrame.setVisibility(View.GONE);
            otherUserImageMessageFrame.setVisibility(View.GONE);
            selfFileAttachmentMessage.setVisibility(View.GONE);
            selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
            otherUserFileAttachmentMessage.setVisibility(View.GONE);
            otherUserFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
            selfProjectInviteMessage.setVisibility(View.GONE);
            otherUserProjectInviteMessage.setVisibility(View.GONE);
            dateSeparator.setVisibility(View.GONE);
            selfUnsentMessage.setVisibility(View.GONE);
            otherUserUnsentMessage.setVisibility(View.GONE);
            selfReplyMessageLayout.setVisibility(View.GONE);
            otherUserReplyMessageLayout.setVisibility(View.GONE);
        }
    }
}
