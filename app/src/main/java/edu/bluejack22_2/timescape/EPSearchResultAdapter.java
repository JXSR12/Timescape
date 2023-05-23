package edu.bluejack22_2.timescape;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.bluejack22_2.timescape.model.ProjectMember;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.dashboard.InvitedMemberAdapter;

public class EPSearchResultAdapter extends RecyclerView.Adapter<EPSearchResultAdapter.ViewHolder> {
    private final Context context;
    private final List<User> searchResults;
    private final Set<String> projectMemberIds;
    private final Set<String> userIds;
    private final String projectId;

    public EPSearchResultAdapter(Context context, List<User> searchResults, Set<String> userIds, Set<String> projectMemberIds, String projectId) {
        this.context = context;
        this.searchResults = searchResults;
        this.userIds = userIds;
        this.projectMemberIds = projectMemberIds;
        this.projectId = projectId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.invited_member_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = searchResults.get(position);
        holder.bind(user);
        holder.addButton.setOnClickListener(v -> {
            if (projectMemberIds.contains(user.getUid())) {
                Toast.makeText(context, R.string.user_is_already_a_member_of_the_project, Toast.LENGTH_SHORT).show();
            } else {
                addMemberToProject(user.getUid());
            }
        });
    }

    private void addMemberToProject(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference projectRef = db.collection("projects").document(projectId);
        DocumentReference userRef = db.collection("users").document(userId);

        Map<String, Object> newMember = new HashMap<>();
        newMember.put("id", userId);
        newMember.put("role", "collaborator");
        newMember.put("date_joined", FieldValue.serverTimestamp());

        projectRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String projectName = "Project Title";
                    projectName = documentSnapshot.getString("title");

                    String finalProjectName = projectName;
                    // Add the new member to the project's 'members' field
                    projectRef.update("members." + userId, newMember)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, R.string.user_added_to_the_project, Toast.LENGTH_SHORT).show();

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

        public void bind(User user) {
            displayNameTextView.setText(user.getDisplayName());
            emailTextView.setText(user.getEmail());

        }
    }

}
