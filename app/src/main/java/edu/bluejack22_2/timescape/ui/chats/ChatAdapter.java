package edu.bluejack22_2.timescape.ui.chats;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import edu.bluejack22_2.timescape.FullScreenImageActivity;
import edu.bluejack22_2.timescape.MainActivity;
import edu.bluejack22_2.timescape.MentionClickableSpan;
import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Chat;
import edu.bluejack22_2.timescape.model.Message;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectMember;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.RoundedImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private Context context;
    private FirebaseAuth firebaseAuth;

    private Map<Integer, Integer> imageMessageIndices = new HashMap<>();

    private int unreadBelowCount = 0;

    private RecyclerView recyclerView;
    private Map<String, String> memberDisplayNameMap = new HashMap<>();
    private String projectId = "NONE";

    private HashMap<String, Project> projectCache = new HashMap<>();

    public interface MessageLongClickListener {
        void onMessageLongClicked(View view, Message message);
    }

    public Message getMessageAt(int index){
        return messages.get(index);
    }
    private MessageLongClickListener messageLongClickListener;

    public ChatAdapter(Context context, FirebaseAuth firebaseAuth, MessageLongClickListener messageLongClickListener, RecyclerView recyclerView, String projectId) {
        this.context = context;
        this.firebaseAuth = firebaseAuth;
        this.messageLongClickListener = messageLongClickListener;
        this.recyclerView = recyclerView;
        this.projectId = projectId;
    }

    public void setCachedMembers(ArrayList<User> projectMembers){
        for(User u : projectMembers){
            memberDisplayNameMap.put(u.getUid(), u.getDisplayName());
        }
    }

    public void setMessages(List<Message> messages, RecyclerView recyclerView, RelativeLayout newMessageLayout, boolean alwaysScroll) {
        int prevCount = this.messages != null ? this.messages.size() : 0;
        boolean shouldScroll = false;

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
                    dateSeparator.setContent(context.getString(R.string.today));
                } else if (msgDate.equals(yesterdayDate)) {
                    dateSeparator.setContent(context.getString(R.string.yesterday));
                } else {
                    dateSeparator.setContent(msgDate);
                }

                this.messages.add(dateSeparator);
            }

            // Add the current message
            this.messages.add(message);

            // Update previousMessage
            previousMessage = message;

            // Check if the message is from the current user and should scroll to the bottom
            if (alwaysScroll) {
                shouldScroll = true;
            }
        }

        int curCount = this.messages.size();

        Log.d("SETMESSAGE", "RAN");
        Log.d("SETMESSAGE", "PREV COUNT: " + prevCount + " | CUR COUNT: " + curCount);
        if (curCount != prevCount) {
            if (shouldScroll || isUserAtBottom(recyclerView, true)) {
                recyclerView.postDelayed(() -> {
                    recyclerView.scrollToPosition(getLastItemPosition());
                }, 50);
                Log.d("SETMESSAGE", "AT BOTTOM");
            } else {
                this.unreadBelowCount++;
                newMessageLayout.setVisibility(View.VISIBLE);
                Log.d("SETMESSAGE", "NOT AT BOTTOM");
            }
        }

        notifyDataSetChanged();
    }

    public void setUnreadBelowCount(int unreadBelowCount) {
        this.unreadBelowCount = unreadBelowCount;
    }

    public int getUnreadBelowCount() {
        return unreadBelowCount;
    }

    public static boolean isUserAtBottom(RecyclerView recyclerView, boolean newMessage) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int lastVisibleItemPosition = 0;
        if (layoutManager != null) {
            lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
        }
        int itemCount = 0;
        if (layoutManager != null) {
            itemCount = layoutManager.getItemCount();
        }
        int addition = newMessage ? 2 : 1;
        return lastVisibleItemPosition == (itemCount - addition);
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
        if (message.getMessage_type() == Message.MessageType.IMAGE) {
            imageMessageIndices.put(position, imageMessageIndices.size());
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

    private void getCachedDisplayName(String userId, final TextView displayName) {
        if (memberDisplayNameMap.containsKey(userId)) {
            displayName.setText(memberDisplayNameMap.get(userId));
        } else {
            displayName.setText(context.getString(R.string.unknown_user));
        }
    }

    private String getCachedDisplayNameAsString(String userId){
        return memberDisplayNameMap.getOrDefault(userId, "UNKNOWN USER");
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {

        private static final int MAX_CHARACTERS_PER_LINE = 100;
        LinearLayout chatItemRoot;

        TextView selfTextMessage;
        TextView otherUserTextMessage;
        RoundedImageView selfImageMessage;
        FrameLayout selfImageMessageFrame;
        ImageView selfImageVideoOverlay;
        FrameLayout otherUserImageMessageFrame;
        ImageView otherUserImageVideoOverlay;
        RoundedImageView otherUserImageMessage;
        LinearLayout selfFileAttachmentMessage;
        TextView selfFileAttachmentName;
        TextView selfFileAttachmentExtension;
        Button selfFileAttachmentDownloadButton;
        LinearLayout otherUserFileAttachmentMessage;
        TextView otherUserFileAttachmentName;
        TextView otherUserFileAttachmentExtension;
        Button otherUserFileAttachmentDownloadButton;
        LinearLayout selfProjectInviteMessage;
        LinearLayout otherUserProjectInviteMessage;
        TextView selfProjectInviteTitle;
        TextView otherUserProjectInviteTitle;
        Button selfJoinButton;
        Button otherJoinButton;
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

        LinearLayout selfRepliedTop;
        LinearLayout otherUserRepliedTop;

        TextView readCount;

        private int repliedMessagePosition;

        public ChatViewHolder(@NonNull View itemView, View.OnClickListener onReplyTopPartClicked) {
            super(itemView);
            chatItemRoot = itemView.findViewById(R.id.chat_item_root);

            selfTextMessage = itemView.findViewById(R.id.self_text_message);
            otherUserTextMessage = itemView.findViewById(R.id.other_user_text_message);
            selfImageMessage = itemView.findViewById(R.id.self_image_message);
            selfImageVideoOverlay = itemView.findViewById(R.id.self_image_video_overlay);
            selfImageMessageFrame = itemView.findViewById(R.id.self_image_message_frame);
            otherUserImageMessage = itemView.findViewById(R.id.other_user_image_message);
            otherUserImageVideoOverlay = itemView.findViewById(R.id.other_user_image_video_overlay);
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
            selfProjectInviteTitle = itemView.findViewById(R.id.self_project_invite_title);
            otherUserProjectInviteTitle = itemView.findViewById(R.id.other_user_project_invite_title);
            selfJoinButton = itemView.findViewById(R.id.self_project_invite_join_button);
            otherJoinButton = itemView.findViewById(R.id.other_user_project_invite_join_button);
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

            otherUserRepliedTop = itemView.findViewById(R.id.other_user_replied_top);
            selfRepliedTop = itemView.findViewById(R.id.self_replied_top);

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
                otherUserRepliedTop.setOnClickListener(onReplyTopPartClicked);
                otherUserRepliedMessageContent.setOnClickListener(onReplyTopPartClicked);
            }

            if (selfRepliedMessageContent != null) {
                selfRepliedTop.setOnClickListener(onReplyTopPartClicked);
                selfRepliedMessageContent.setOnClickListener(onReplyTopPartClicked);
            }

            selfTextMessage.setMovementMethod(LinkMovementMethod.getInstance());
            otherUserTextMessage.setMovementMethod(LinkMovementMethod.getInstance());
            selfReplyMessage.setMovementMethod(LinkMovementMethod.getInstance());
            otherUserReplyMessage.setMovementMethod(LinkMovementMethod.getInstance());
            selfRepliedMessageContent.setMovementMethod(LinkMovementMethod.getInstance());
            otherUserRepliedMessageContent.setMovementMethod(LinkMovementMethod.getInstance());
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
            getCachedDisplayName(message.getSender().getId(), displayName);
        }

        private void handleTextMessage(Message message, boolean isCurrentUser) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            HashMap<String, String> mentions = message.getMentions();
            List<String> reads = message.getReads();

            if (mentions != null && mentions.containsValue(currentUserId)) {
                chatItemRoot.setBackgroundColor(ContextCompat.getColor(context, R.color.mention_highlight)); // Semi-transparent yellow
            } else {
                chatItemRoot.setBackgroundColor(Color.TRANSPARENT);
            }

            if (isCurrentUser) {
                selfTextMessage.setVisibility(View.VISIBLE);
                selfTextMessage.setText(formatMentions(message.getContent(), mentions));

                String content = message.getContent();
                int estimatedLines = content.length() / MAX_CHARACTERS_PER_LINE;
                estimatedLines += content.split("\n|\r").length;

                if (estimatedLines > 20) {
                    selfTextMessage.setMaxLines(5);
                    selfTextMessage.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout readMoreLayout = itemView.findViewById(R.id.self_text_message_read_more);
                    readMoreLayout.setVisibility(View.VISIBLE);
                    readMoreLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showDialog(content, getCachedDisplayNameAsString(message.getSender().getId()));
                        }
                    });
                } else {
                    selfTextMessage.setMaxLines(1000);
                    LinearLayout readMoreLayout = itemView.findViewById(R.id.self_text_message_read_more);
                    readMoreLayout.setVisibility(View.GONE);
                }

                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText(context.getString(R.string.read) + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }

            } else {
                otherUserTextMessage.setVisibility(View.VISIBLE);
                otherUserTextMessage.setText(formatMentions(message.getContent(), mentions));

                String content = message.getContent();
                int estimatedLines = content.length() / MAX_CHARACTERS_PER_LINE;
                estimatedLines += content.split("\n|\r").length;

                if (estimatedLines > 20) {
                    otherUserTextMessage.setMaxLines(5);
                    otherUserTextMessage.setEllipsize(TextUtils.TruncateAt.END);
                    LinearLayout readMoreLayout = itemView.findViewById(R.id.other_user_text_message_read_more);
                    readMoreLayout.setVisibility(View.VISIBLE);
                    readMoreLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showDialog(content, getCachedDisplayNameAsString(message.getSender().getId()));
                        }
                    });
                } else {
                    otherUserTextMessage.setMaxLines(1000);
                    LinearLayout readMoreLayout = itemView.findViewById(R.id.other_user_text_message_read_more);
                    readMoreLayout.setVisibility(View.GONE);
                }
            }
        }


        private void handleImageMessage(Message message, boolean isCurrentUser) {
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL); // Enable caching
            int targetImageSize = calculateTargetImageSize(); // Calculate an appropriate target size
            int thumbnailSize = 32; // Set the thumbnail size

            // Check if the message content is a video
            boolean isVideo = isVideoMessage(message.getContent()); // Implement this method to check the mime type or other conditions

            if (isCurrentUser) {
                selfImageMessageFrame.setVisibility(View.VISIBLE);

                // Load the image URL into the ImageView using an image loading library like Glide
                Glide.with(itemView.getContext())
                        .load(message.getContent())
                        .placeholder(R.drawable.placeholder_image) // Placeholder image
                        .error(R.drawable.error_image) // Error image
                        .thumbnail(Glide.with(itemView.getContext())
                                .load(message.getContent())
                                .apply(requestOptions)
                                .override(thumbnailSize, thumbnailSize))
                        .apply(requestOptions.override(targetImageSize, targetImageSize))
                        .into(selfImageMessage);

                // Set the video overlay visibility based on whether it is a video or not
                if (isVideo) {
                    selfImageVideoOverlay.setVisibility(View.VISIBLE);
                } else {
                    selfImageVideoOverlay.setVisibility(View.GONE);
                }

                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText(context.getString(R.string.read) + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }

                selfImageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getBindingAdapterPosition(); // get the adapter position
                        if (position != RecyclerView.NO_POSITION && imageMessageIndices.containsKey(position)) { // check if item still exists and is an image message
                            Intent fullScreenIntent = new Intent(context, FullScreenImageActivity.class);
                            fullScreenIntent.putExtra("project_id", projectId);
                            fullScreenIntent.putExtra("message_id", message.getId());
                            context.startActivity(fullScreenIntent);
                        }
                    }
                });

            } else {
                otherUserImageMessageFrame.setVisibility(View.VISIBLE);

                // Load the image URL into the ImageView using an image loading library like Glide
                Glide.with(itemView.getContext())
                        .load(message.getContent())
                        .placeholder(R.drawable.placeholder_image) // Placeholder image
                        .error(R.drawable.error_image) // Error image
                        .thumbnail(Glide.with(itemView.getContext())
                                .load(message.getContent())
                                .apply(requestOptions)
                                .override(thumbnailSize, thumbnailSize))
                        .apply(requestOptions.override(targetImageSize, targetImageSize))
                        .into(otherUserImageMessage);

                // Set the video overlay visibility based on whether it is a video or not
                if (isVideo) {
                    otherUserImageVideoOverlay.setVisibility(View.VISIBLE);
                } else {
                    otherUserImageVideoOverlay.setVisibility(View.GONE);
                }

                otherUserImageMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getBindingAdapterPosition(); // get the adapter position
                        if (position != RecyclerView.NO_POSITION && imageMessageIndices.containsKey(position)) { // check if item still exists and is an image message
                            Intent fullScreenIntent = new Intent(context, FullScreenImageActivity.class);
                            fullScreenIntent.putExtra("project_id", projectId);
                            fullScreenIntent.putExtra("message_id", message.getId());
                            context.startActivity(fullScreenIntent);
                        }
                    }
                });
            }
        }

        private boolean isVideoMessage(String url) {
            String expectedSegment = "https://firebasestorage.googleapis.com/v0/b/timescapeandroid.appspot.com/o/videos";
            return url.startsWith(expectedSegment);
        }

        private int calculateTargetImageSize() {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);

            // Calculate the target size based on the device's display dimensions
            int targetSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
            // You can also apply additional logic to adjust the target size as per your requirements

            return targetSize;
        }


        private void handleFileAttachmentMessage(Message message, boolean isCurrentUser) {
            if (isCurrentUser) {
                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText(context.getString(R.string.read) + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }
                selfFileAttachmentMessage.setVisibility(View.VISIBLE);
                selfFileAttachmentName.setText(message.getFileName());
                String fullExt = message.getFileName().substring(message.getFileName().lastIndexOf(".") + 1);
                selfFileAttachmentExtension.setText(fullExt.substring(0, Math.min(fullExt.length(), 3)).toUpperCase());

                selfFileAttachmentDownloadButton.setText(isFileExist(message.getFileName()) ? context.getString(R.string.btn_open) : context.getString(R.string.btn_download));
                selfFileAttachmentDownloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isFileExist(message.getFileName())) {
                            // Open file
                            openFile(message.getFileName());
                        } else {
                            // Download file
                            downloadFile(message);
                        }
                    }
                });
            } else {
                otherUserFileAttachmentMessage.setVisibility(View.VISIBLE);
                otherUserFileAttachmentName.setText(message.getFileName());
                String fullExt = message.getFileName().substring(message.getFileName().lastIndexOf(".") + 1);
                otherUserFileAttachmentExtension.setText(fullExt.substring(0, Math.min(fullExt.length(), 3)).toUpperCase());
                otherUserFileAttachmentDownloadButton.setText(isFileExist(message.getFileName()) ? "OPEN" : "DOWNLOAD");
                otherUserFileAttachmentDownloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isFileExist(message.getFileName())) {
                            // Open file
                            openFile(message.getFileName());
                        } else {
                            // Download file
                            downloadFile(message);
                        }
                    }
                });
            }

        }

        private void openFile(String fileName) {
            String appName = context.getString(R.string.app_name);
            File downloadsFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), appName);
            File localFile = new File(downloadsFolder, fileName);

            Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", localFile);

            // Determine MIME type
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

            Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
            openFileIntent.setDataAndType(fileUri, mimeType);

            openFileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(openFileIntent);
        }


        private void downloadFile(Message message){
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
                Toast.makeText(context, context.getString(R.string.error_creating_local_file) + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                        Toast.makeText(context, R.string.file_downloaded, Toast.LENGTH_SHORT).show();
                        Log.d("Download", "File downloaded at: " + uri.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, context.getString(R.string.error_saving_the_file) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressBar.setVisibility(View.GONE);
                    selfFileAttachmentDownloadButton.setVisibility(View.VISIBLE);
                    Toast.makeText(context, context.getString(R.string.failed_to_download_file) + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull FileDownloadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressBar.setProgress((int) Math.floor(progress));
                }
            });
        }

        private boolean isFileExist(String fileName) {
            String appName = context.getString(R.string.app_name);
            File downloadsFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), appName);
            File localFile = new File(downloadsFolder, fileName);
            return localFile.exists();
        }


        private void handleProjectInviteMessage(Message message, boolean isCurrentUser) {
            LinearLayout projectInviteMessage;
            TextView projectInviteTitle;
            Button joinButton;

            if (isCurrentUser) {
                projectInviteMessage = selfProjectInviteMessage;
                projectInviteTitle = selfProjectInviteTitle;
                joinButton = selfJoinButton;

                List<String> reads = message.getReads();
                if (reads != null && !reads.isEmpty()) {
                    int readCountValue = reads.size();
                    readCount.setText(context.getString(R.string.read) + readCountValue + " • ");
                    readCount.setVisibility(View.VISIBLE);
                } else {
                    readCount.setVisibility(View.GONE);
                }
            } else {
                projectInviteMessage = otherUserProjectInviteMessage;
                projectInviteTitle = otherUserProjectInviteTitle;
                joinButton = otherJoinButton;
            }

            // Show the message
            projectInviteMessage.setVisibility(View.VISIBLE);

            // Check if the user is already in the project
            Project cachedProject = projectCache.get(message.getContent());
            if (cachedProject != null) {
                handleProject(cachedProject, projectInviteTitle, joinButton, message.getContent(), isCurrentUser);
            } else {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("projects").document(message.getContent())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    Project project = documentSnapshot.toObject(Project.class);
                                    if (project != null) {
                                        // Cache the project for future use
                                        projectCache.put(message.getContent(), project);
                                        handleProject(project, projectInviteTitle, joinButton, message.getContent(), isCurrentUser);
                                    }
                                }
                            }
                        });
            }
        }

        private void handleProject(Project project, TextView projectInviteTitle, Button joinButton, String projectId, boolean isCurrentUser) {
            projectInviteTitle.setText(project.getTitle());
            if (project.getMembers().containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid()) || project.getOwner().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                joinButton.setText(R.string.already_in_project);
                joinButton.setEnabled(false);
            } else {
                joinButton.setText(R.string.join_project);
                joinButton.setEnabled(true);
                joinButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addMemberToProject(projectId, FirebaseAuth.getInstance().getCurrentUser().getUid());
                    }
                });
            }
        }

        private void addMemberToProject(String projectId, String userId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference projectRef = db.collection("projects").document(projectId);
            DocumentReference userRef = db.collection("users").document(userId);

            Map<String, Object> newMemberMap = new HashMap<>();
            newMemberMap.put("userId", userId);
            newMemberMap.put("role", "collaborator");
            newMemberMap.put("date_joined", FieldValue.serverTimestamp());

            projectRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        String projectName = "Project Title";
                        projectName = documentSnapshot.getString("title");

                        String finalProjectName = projectName;
                        // Add the new member to the project's 'members' field
                        projectRef.update("members." + userId, newMemberMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, R.string.successfully_joined_the_project, Toast.LENGTH_SHORT).show();
                                    ChatAdapter.this.notifyDataSetChanged();

                                    // Get the current user's ID and display name
                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    String actorUserId = currentUser.getUid();
                                    String actorName = currentUser.getDisplayName();

                                    // Get the added user's display name
                                    userRef.get()
                                            .addOnSuccessListener(docSnap -> {
                                                String objectUserName = docSnap.getString("displayName");

                                                // Send the notification
                                                MainActivity.sendProjectOperationNotification(actorUserId, actorName, projectId, finalProjectName, "ADD", userId, objectUserName);
                                            })
                                            .addOnFailureListener(e -> Log.w("ProjectMembersViewModel", "Error getting added user's display name", e));
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, R.string.failed_to_add_user_to_the_project, Toast.LENGTH_SHORT).show();
                                });
                    }
                }});
        }

        private void handleDateSeparator(Message message) {
            dateSeparator.setVisibility(View.VISIBLE);
            dateSeparator.setText(message.getContent());
        }

        private void handleUnsentMessage(Message message, boolean isCurrentUser) {
            String unsentText = context.getString(R.string.message_deleted);
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

            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            HashMap<String, String> mentions = message.getMentions();
            Log.d("Checking for mentions", mentions != null ? "Mentions count: " + mentions.size() : "No mentions");

            if (mentions != null && mentions.containsValue(currentUserId)) {
                chatItemRoot.setBackgroundColor(ContextCompat.getColor(context, R.color.mention_highlight)); // Semi-transparent yellow
                Log.d("Mentioned Message", "This comes from a message mentioning you");
            } else {
                chatItemRoot.setBackgroundColor(Color.TRANSPARENT);
            }

            // If the replied message is found, set the appropriate fields
            if (repliedMessage != null) {
                if (isCurrentUser) {
                    List<String> reads = message.getReads();
                    if (reads != null && !reads.isEmpty()) {
                        int readCountValue = reads.size();
                        readCount.setText(String.format("%s%d • ", context.getString(R.string.read), readCountValue));
                        readCount.setVisibility(View.VISIBLE);
                    } else {
                        readCount.setVisibility(View.GONE);
                    }
                    selfReplyMessageLayout.setVisibility(View.VISIBLE);
                    selfReplyMessage.setText(formatMentions(message.getContent(), mentions));

                    String content = message.getContent();
                    int estimatedLines = content.length() / MAX_CHARACTERS_PER_LINE;
                    estimatedLines += content.split("\n|\r").length;

                    if (estimatedLines > 20) {
                        selfReplyMessage.setMaxLines(5);
                        selfReplyMessage.setEllipsize(TextUtils.TruncateAt.END);
                        LinearLayout readMoreLayout = itemView.findViewById(R.id.self_reply_message_read_more);
                        readMoreLayout.setVisibility(View.VISIBLE);
                        readMoreLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showDialog(content, getCachedDisplayNameAsString(message.getSender().getId()));
                            }
                        });
                    } else {
                        selfReplyMessage.setMaxLines(1000);
                        LinearLayout readMoreLayout = itemView.findViewById(R.id.self_reply_message_read_more);
                        readMoreLayout.setVisibility(View.GONE);
                    }


                    selfRepliedAvatar.setImageDrawable(generateAvatar(repliedMessage.getSender().getId()));
                    showRepliedDisplayName(selfRepliedName, repliedMessage);

                    if(repliedMessage.getMessage_type() == Message.MessageType.FILE){
                        selfRepliedMessageContent.setText(R.string.sent_a_file);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.IMAGE){
                        selfRepliedMessageContent.setText(R.string.sent_an_image);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.PROJECT_INVITE){
                        selfRepliedMessageContent.setText(R.string.sent_a_project_invite);
                    }else{
                        selfRepliedMessageContent.setText(formatMentions(repliedMessage.getContent(), repliedMessage.getMentions()));
                    }


                } else {
                    otherUserReplyMessageLayout.setVisibility(View.VISIBLE);
                    otherUserReplyMessage.setText(formatMentions(message.getContent(), mentions));

                    String content = message.getContent();
                    int estimatedLines = content.length() / MAX_CHARACTERS_PER_LINE;
                    estimatedLines += content.split("\n|\r").length;

                    if (estimatedLines > 20) {
                        otherUserReplyMessage.setMaxLines(5);
                        otherUserReplyMessage.setEllipsize(TextUtils.TruncateAt.END);
                        LinearLayout readMoreLayout = itemView.findViewById(R.id.other_user_reply_message_read_more);
                        readMoreLayout.setVisibility(View.VISIBLE);
                        readMoreLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showDialog(content, getCachedDisplayNameAsString(message.getSender().getId()));
                            }
                        });
                    } else {
                        otherUserReplyMessage.setMaxLines(1000);
                        LinearLayout readMoreLayout = itemView.findViewById(R.id.other_user_reply_message_read_more);
                        readMoreLayout.setVisibility(View.GONE);
                    }

                    otherUserRepliedAvatar.setImageDrawable(generateAvatar(repliedMessage.getSender().getId()));
                    showRepliedDisplayName(otherUserRepliedName, repliedMessage);

                    if(repliedMessage.getMessage_type() == Message.MessageType.FILE){
                        otherUserRepliedMessageContent.setText(R.string.sent_a_file);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.IMAGE){
                        otherUserRepliedMessageContent.setText(R.string.sent_an_image);
                    }else if (repliedMessage.getMessage_type() == Message.MessageType.PROJECT_INVITE){
                        selfRepliedMessageContent.setText(R.string.sent_a_project_invite);
                    }else{
                        otherUserRepliedMessageContent.setText(formatMentions(repliedMessage.getContent(), repliedMessage.getMentions()));
                    }
                }
            }
        }

        private void showDialog(String messageContent, String senderName) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            // Create scroll view and text view
            ScrollView scrollView = new ScrollView(context);
            TextView messageTextView = new TextView(context);

            // Configure text view
            messageTextView.setText(messageContent);
            messageTextView.setPadding(15, 15, 15, 15); // optional padding
            messageTextView.setTextSize(18); // optional text size

            // Add text view to scroll view
            scrollView.addView(messageTextView);
            scrollView.setPadding(5, 20, 5, 5);

            // Configure the dialog
            builder.setTitle(senderName)
                    .setView(scrollView)
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });

            // Create and show the dialog
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }


        private void showRepliedDisplayName(TextView repliedName, Message message) {
            getCachedDisplayName(message.getSender().getId(), repliedName);
        }

        private Spannable formatMentions(String content, HashMap<String, String> mentions) {
            SpannableStringBuilder formattedContent = new SpannableStringBuilder(content);

            // For each mention in the message
            for (Map.Entry<String, String> entry : mentions.entrySet()) {
                String mentionTag = entry.getKey().split("_")[0];  // The mention tag in the format <@User Name>
                int start = Integer.parseInt(entry.getKey().split("_")[1]); // The index where the mention tag starts
                String userId = entry.getValue();  // The user id of the mentioned user
                String currentName = getCachedDisplayNameAsString(userId);  // The current name of the mentioned user

                // Replace the mention tag with the current name
                String currentMentionTag = "<@" + currentName + ">";
                formattedContent.replace(start, start + mentionTag.length(), currentMentionTag);

                // Mark the current mention tag as a blue span
                MentionClickableSpan mentionClickableSpan = new MentionClickableSpan(userId);
                formattedContent.setSpan(mentionClickableSpan, start, start + currentMentionTag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.BLUE);  // Replace with desired color
                formattedContent.setSpan(colorSpan, start, start + currentMentionTag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            return formattedContent;
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
            selfImageVideoOverlay.setVisibility(View.GONE);
            otherUserImageVideoOverlay.setVisibility(View.GONE);
            itemView.findViewById(R.id.self_reply_message_read_more).setVisibility(View.GONE);
            itemView.findViewById(R.id.other_user_reply_message_read_more).setVisibility(View.GONE);
            itemView.findViewById(R.id.self_text_message_read_more).setVisibility(View.GONE);
            itemView.findViewById(R.id.other_user_reply_message_read_more).setVisibility(View.GONE);
        }
    }
}

