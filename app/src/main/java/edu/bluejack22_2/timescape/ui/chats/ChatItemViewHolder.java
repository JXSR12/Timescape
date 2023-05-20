package edu.bluejack22_2.timescape.ui.chats;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Chat;
import edu.bluejack22_2.timescape.model.Message;

public class ChatItemViewHolder extends RecyclerView.ViewHolder {

    private ImageView avatarImageView;
    private TextView projectTitleTextView;
    private TextView latestMessageTextView;
    private TextView timestampTextView;
    private TextView unreadBadgeTextView;

    public ChatItemViewHolder(@NonNull View itemView) {
        super(itemView);
        avatarImageView = itemView.findViewById(R.id.avatarImageView);
        projectTitleTextView = itemView.findViewById(R.id.projectTitleTextView);
        latestMessageTextView = itemView.findViewById(R.id.latestMessageTextView);
        timestampTextView = itemView.findViewById(R.id.timestampTextView);
        unreadBadgeTextView = itemView.findViewById(R.id.unreadBadgeTextView);
    }

    public void bind(Chat chatItem, TextDrawable avatarDrawable) {
        avatarImageView.setImageDrawable(avatarDrawable);
        projectTitleTextView.setText(chatItem.getProjectTitle());
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

