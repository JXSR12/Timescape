package edu.bluejack22_2.timescape;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import edu.bluejack22_2.timescape.model.ProjectMember;

public class ProjectMembersAdapter extends RecyclerView.Adapter<ProjectMembersAdapter.MemberViewHolder> {
    private List<ProjectMember> mMembers;
    private Context mContext;
    private String mOwnerId;
    private boolean mIsCurrentUserOwner;
    private OnRemoveMemberClickListener mOnRemoveMemberClickListener;

    public interface OnRemoveMemberClickListener {
        void onRemoveMember(String userId);
    }

    public ProjectMembersAdapter(List<ProjectMember> members, Context context, String ownerId, OnRemoveMemberClickListener onRemoveMemberClickListener) {
        this.mMembers = members;
        this.mContext = context;
        this.mOwnerId = ownerId;
        this.mIsCurrentUserOwner = false;
        this.mOnRemoveMemberClickListener = onRemoveMemberClickListener;
    }

    public void updateMembers(List<ProjectMember> members) {
        this.mMembers = members;
        notifyDataSetChanged();
    }

    public void setIsCurrentUserOwner(boolean isOwner) {
        this.mIsCurrentUserOwner = isOwner;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        ProjectMember member = mMembers.get(position);
        holder.bind(member, mIsCurrentUserOwner);

        if (mIsCurrentUserOwner && !mOwnerId.equals(member.getUserId())) {
            holder.mRemoveButton.setVisibility(View.VISIBLE);
        } else {
            holder.mRemoveButton.setVisibility(View.GONE);
        }

        holder.mRemoveButton.setOnClickListener(v -> {
            if (mIsCurrentUserOwner && !mOwnerId.equals(member.getUserId())) {
                DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        mOnRemoveMemberClickListener.onRemoveMember(member.getUserId());
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage(R.string.are_you_sure_you_want_to_remove_this_member)
                        .setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener)
                        .show();
            }
        });
    }


    @Override
    public int getItemCount() {
        return mMembers.size();
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private ImageView mAvatar;
        private TextView mName, mDateJoined, mOwnerTag;
        private ImageButton mRemoveButton;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            mAvatar = itemView.findViewById(R.id.avatar);
            mName = itemView.findViewById(R.id.display_name);
            mDateJoined = itemView.findViewById(R.id.joined_date);
            mOwnerTag = itemView.findViewById(R.id.owner_label);
            mRemoveButton = itemView.findViewById(R.id.remove_member_button);
        }

        public void bind(ProjectMember member, boolean isCurrentUserOwner) {
            mAvatar.setImageDrawable(generateAvatar(member.getUserId()));
            mName.setText(member.getDisplayName());
            if(member.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                itemView.setBackgroundColor(Color.parseColor("#AAFFEE63")); //light yellow
            }else{
                itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            if (member.getUserId().equals(mOwnerId)) {
                mName.setTextColor(Color.parseColor("#FFA500")); // orange
                mOwnerTag.setVisibility(View.VISIBLE);
            } else {
                mName.setTextColor(Color.BLACK);
                mOwnerTag.setVisibility(View.GONE);
            }

            DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String joinedDate = dateFormat.format(member.getDate_joined().toDate());
            mDateJoined.setText("Joined on " + joinedDate);

            if (isCurrentUserOwner) {
                mRemoveButton.setVisibility(View.VISIBLE);
            } else {
                mRemoveButton.setVisibility(View.GONE);
            }
        }

        private TextDrawable generateAvatar(String userId) {
            String content = "\\O/";
            Random random = new Random(userId.hashCode());
            int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
            return TextDrawable.builder().buildRound(content, color);
        }
    }
}

