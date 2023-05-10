package edu.bluejack22_2.timescape.ui.inbox;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.InboxMessage;

public class InboxAdapter extends RecyclerView.Adapter<InboxAdapter.ViewHolder> {

    private Context context;
    private List<InboxMessage> messages;
    private InboxViewModel viewModel;

    public InboxAdapter(List<InboxMessage> inboxMessages, Context context, InboxViewModel viewModel) {
        this.messages = inboxMessages;
        this.context = context;
        this.viewModel = viewModel;
    }

    public void setMessages(List<InboxMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.inbox_message_item, parent, false);
        return new ViewHolder(view);
    }

    public void submitList(List<InboxMessage> messages){
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InboxMessage message = messages.get(position);
        holder.bind(message);

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            // Open BottomSheetDialog
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_message, null);

            // Set message details
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView titleTextView = bottomSheetView.findViewById(R.id.message_title);
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView timestampTextView = bottomSheetView.findViewById(R.id.message_timestamp);
            @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView contentTextView = bottomSheetView.findViewById(R.id.message_content);

            titleTextView.setText(message.getTitle());
            timestampTextView.setText(getFriendlyTimeAgo(message.getSentTime()));
            contentTextView.setText(message.getContent());

            dialog.setContentView(bottomSheetView);
            dialog.show();

            // Mark message as read
            if (!message.isRead()) {
                viewModel.markMessageAsRead(message);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            // Show popup menu
            PopupMenu popup = new PopupMenu(context, v);
            popup.getMenuInflater().inflate(R.menu.inbox_mark_menu, popup.getMenu());

            // Update menu items based on isRead value
            MenuItem markAsReadItem = popup.getMenu().findItem(R.id.action_mark_as_read);
            MenuItem markAsUnreadItem = popup.getMenu().findItem(R.id.action_mark_as_unread);
            markAsReadItem.setVisible(!message.isRead());
            markAsUnreadItem.setVisible(message.isRead());

            popup.setOnMenuItemClickListener(item -> {
                if(item.getItemId() == R.id.action_mark_as_read) {
                    viewModel.markMessageAsRead(message);
                    return true;
                }else if (item.getItemId() == R.id.action_mark_as_unread) {
                    viewModel.markMessageAsUnread(message);
                    return true;
                }
                return false;
            });

            popup.show();
            return true;
        });
    }

    public String getFriendlyTimeAgo(Timestamp timestamp) {
        long diffInSeconds = (System.currentTimeMillis() - timestamp.toDate().getTime()) / 1000;
        String friendlyTimeAgo;

        if (diffInSeconds < 60) {
            friendlyTimeAgo = diffInSeconds + "s ago";
        } else if (diffInSeconds < 3600) {
            friendlyTimeAgo = (diffInSeconds / 60) + "m ago";
        } else if (diffInSeconds < 86400) {
            friendlyTimeAgo = (diffInSeconds / 3600) + "h ago";
        } else if (diffInSeconds < 2592000) {
            friendlyTimeAgo = (diffInSeconds / 86400) + "d ago";
        } else if (diffInSeconds < 31536000) {
            friendlyTimeAgo = (diffInSeconds / 2592000) + "mo ago";
        } else {
            friendlyTimeAgo = (diffInSeconds / 31536000) + "y ago";
        }

        return friendlyTimeAgo;
    }


    @Override
    public int getItemCount() {
        return messages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView timeAgoTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.title);
            timeAgoTextView = itemView.findViewById(R.id.sent_time);
        }

        public void bind(InboxMessage message) {
            titleTextView.setText(message.getTitle());
            timeAgoTextView.setText(getFriendlyTimeAgo(message.getSentTime()));
            if (message.isRead()) {
                titleTextView.setTypeface(null, Typeface.NORMAL);
                titleTextView.setTextColor(ContextCompat.getColor(context, R.color.orange_acc0));
            } else {
                titleTextView.setTypeface(null, Typeface.BOLD);
                titleTextView.setTextColor(ContextCompat.getColor(context, R.color.orange_def2));
            }
        }
    }
}





