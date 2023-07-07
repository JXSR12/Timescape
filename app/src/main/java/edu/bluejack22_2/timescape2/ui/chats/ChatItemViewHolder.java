package edu.bluejack22_2.timescape2.ui.chats;

import static android.content.ContentValues.TAG;

import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.bluejack22_2.timescape2.R;
import edu.bluejack22_2.timescape2.model.Chat;
import edu.bluejack22_2.timescape2.model.Message;

public class ChatItemViewHolder extends RecyclerView.ViewHolder {

    private ImageView avatarImageView;
    private TextView projectTitleTextView;
    private TextView latestMessageTextView;
    private TextView timestampTextView;
    private TextView unreadBadgeTextView;

    private ImageView muteImageView;

    public ChatItemViewHolder(@NonNull View itemView) {
        super(itemView);
        avatarImageView = itemView.findViewById(R.id.avatarImageView);
        projectTitleTextView = itemView.findViewById(R.id.projectTitleTextView);
        latestMessageTextView = itemView.findViewById(R.id.latestMessageTextView);
        timestampTextView = itemView.findViewById(R.id.timestampTextView);
        unreadBadgeTextView = itemView.findViewById(R.id.unreadBadgeTextView);
        muteImageView = itemView.findViewById(R.id.muteImageView);
    }

    public void bind(Chat chatItem, TextDrawable avatarDrawable) {
        avatarImageView.setImageDrawable(avatarDrawable);
        projectTitleTextView.setText(chatItem.getProjectTitle());
        // Get current user
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Check if the chat is muted
        AtomicBoolean isMuted = new AtomicBoolean(false);
        FirebaseFirestore.getInstance()
                .collection("settings")
                .document(userId)
                .collection("mutedChats")
                .document(chatItem.getProjectId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        muteImageView.setVisibility(View.VISIBLE);
                        isMuted.set(true);
                    } else {
                        muteImageView.setVisibility(View.GONE);
                        isMuted.set(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure here
                    Log.e(TAG, itemView.getContext().getString(R.string.failed_to_get_user_settings), e);
                });

        itemView.setOnLongClickListener(v -> {
            // Create a PopupMenu
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            // Inflate the popup_menu.xml layout
            MenuInflater inflater = popup.getMenuInflater();

            // Set click listener for menu items
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.mute_chat) {
                    // Mute the chat
                    FirebaseFirestore.getInstance()
                            .collection("settings")
                            .document(userId)
                            .collection("mutedChats")
                            .document(chatItem.getProjectId())
                            .set(new HashMap<>())
                            .addOnSuccessListener(aVoid -> {
                                muteImageView.setVisibility(View.VISIBLE);
                                isMuted.set(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), R.string.failed_to_disable_chat_notifications, Toast.LENGTH_SHORT).show();
                            });
                    return true;
                } else if (item.getItemId() == R.id.unmute_chat) {
                    // Unmute the chat
                    FirebaseFirestore.getInstance()
                            .collection("settings")
                            .document(userId)
                            .collection("mutedChats")
                            .document(chatItem.getProjectId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                muteImageView.setVisibility(View.GONE);
                                isMuted.set(false);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), R.string.failed_to_enable_chat_notifications, Toast.LENGTH_SHORT).show();
                            });
                    return true;
                }
                return false;
            });

            // Inflate the appropriate menu
            if (isMuted.get()) {
                inflater.inflate(R.menu.unmute_menu, popup.getMenu());
            } else {
                inflater.inflate(R.menu.mute_menu, popup.getMenu());
            }

            // Display the popup
            popup.show();
            return true;
        });



        if(chatItem.getLatestMessage().getMessage_type().equals(Message.MessageType.IMAGE)){
            latestMessageTextView.setText(chatItem.getSenderName() + latestMessageTextView.getContext().getString(R.string.sent_an_image_attachment));
        }else if(chatItem.getLatestMessage().getMessage_type().equals(Message.MessageType.FILE)){
            latestMessageTextView.setText(chatItem.getSenderName() + latestMessageTextView.getContext().getString(R.string.sent_a_file_attachment));
        }else{
            latestMessageTextView.setText(chatItem.getLatestMessage().getContent());
        }

        timestampTextView.setText(chatItem.getTimestamp());

        if (chatItem.getUnreadCount() > 0) {
            unreadBadgeTextView.setVisibility(View.VISIBLE);
            unreadBadgeTextView.setText(String.valueOf(chatItem.getUnreadCount()));
        } else {
            unreadBadgeTextView.setVisibility(View.GONE);
        }
    }
}

