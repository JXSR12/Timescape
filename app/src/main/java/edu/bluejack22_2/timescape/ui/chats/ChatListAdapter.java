package edu.bluejack22_2.timescape.ui.chats;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;

import java.util.List;
import java.util.Random;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Chat;

public class ChatListAdapter extends RecyclerView.Adapter<ChatItemViewHolder> {
    public interface OnChatItemClickListener {
        void onChatItemClick(String projectId);
    }

    private List<Chat> chatItemList;
    private Context context;
    private OnChatItemClickListener listener;

    public ChatListAdapter(Context context, List<Chat> chatItemList, OnChatItemClickListener listener) {
        this.context = context;
        this.chatItemList = chatItemList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chats_item, parent, false);
        return new ChatItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatItemViewHolder holder, int position) {
        Chat chatItem = chatItemList.get(position);
        TextDrawable avatarDrawable = generateAvatar(chatItem.getProjectId(), chatItem.getProjectTitle());
        holder.bind(chatItem, avatarDrawable);

        // Set the click listener
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onChatItemClick(chatItem.getProjectId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatItemList.size();
    }

    public void setChatItems(List<Chat> chatItems) {
        this.chatItemList = chatItems;
    }

    private TextDrawable generateAvatar(String projectId, String title) {
        String firstLetter = String.valueOf(title.charAt(0)).toUpperCase();
        Random random = new Random(projectId.hashCode());
        int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return TextDrawable.builder().buildRound(firstLetter, color);
    }
}

