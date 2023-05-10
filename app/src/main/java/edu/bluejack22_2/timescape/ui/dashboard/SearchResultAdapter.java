package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.User;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private final Context context;
    private final List<User> searchResults;
    private final List<User> invitedMembers;
    private final InvitedMemberAdapter invitedMemberAdapter;

    public SearchResultAdapter(Context context, List<User> searchResults, List<User> invitedMembers, InvitedMemberAdapter invitedMemberAdapter) {
        this.context = context;
        this.searchResults = searchResults;
        this.invitedMembers = invitedMembers;
        this.invitedMemberAdapter = invitedMemberAdapter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View searchResultItemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.invited_member_item, parent, false);
        return new ViewHolder(searchResultItemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = searchResults.get(position);
        holder.displayNameTextView.setText(user.getDisplayName());
        holder.emailTextView.setText(user.getEmail());
        // Set the avatarImageView with the user's avatar (if available)

        // Check if the user is already in the invitedMembers list and disable the add button if necessary
        boolean userAlreadyAdded = false;
        for (User invitedMember : invitedMembers) {
            if (invitedMember != null && invitedMember.getUid().equals(user.getUid())) {
                userAlreadyAdded = true;
                break;
            }
        }

        holder.addButton.setEnabled(!userAlreadyAdded);

        holder.addButton.setOnClickListener(view -> {
            invitedMembers.add(user);
            invitedMemberAdapter.notifyItemInserted(invitedMembers.size() - 1);
            holder.addButton.setEnabled(false);
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayNameTextView;
        TextView emailTextView;
        ImageView avatarImageView;
        ImageButton addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            displayNameTextView = itemView.findViewById(R.id.member_name);
            emailTextView = itemView.findViewById(R.id.member_email);
            avatarImageView = itemView.findViewById(R.id.member_avatar);
            addButton = itemView.findViewById(R.id.add_member);

        }
    }
}
