package edu.bluejack22_2.timescape2.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.bluejack22_2.timescape2.R;
import edu.bluejack22_2.timescape2.model.User;

public class InvitedMemberAdapter extends RecyclerView.Adapter<InvitedMemberAdapter.ViewHolder> {
    private List<User> invitedMembers;

    public InvitedMemberAdapter(List<User> invitedMembers) {
        this.invitedMembers = invitedMembers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.invited_member_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User member = invitedMembers.get(position);
        holder.memberName.setText(member.getDisplayName());
        holder.memberEmail.setText(member.getEmail());
        holder.addButton.setImageResource(R.drawable.baseline_remove_24); // Set the remove icon
        // Set the avatar

        // Add a click listener to remove the member from the invitedMembers list
        holder.addButton.setOnClickListener(view -> {
            invitedMembers.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, invitedMembers.size());
        });
    }

    @Override
    public int getItemCount() {
        return invitedMembers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView memberName;
        TextView memberEmail;
        ImageView memberAvatar;

        ImageButton addButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.member_name);
            memberEmail = itemView.findViewById(R.id.member_email);
            memberAvatar = itemView.findViewById(R.id.member_avatar);
            addButton = itemView.findViewById(R.id.add_member);
        }
    }
}
