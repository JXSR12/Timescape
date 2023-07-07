package edu.bluejack22_2.timescape2;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.bluejack22_2.timescape2.model.ProjectMember;

public class ProjectMembersViewModel extends ViewModel {
    private MutableLiveData<List<ProjectMember>> mMembers;
    private MutableLiveData<Boolean> mIsCurrentUserOwner;
    private MutableLiveData<String> mProjectTitle;
    private MutableLiveData<String> mOwnerId;

    public ProjectMembersViewModel() {
        mMembers = new MutableLiveData<List<ProjectMember>>();
        mIsCurrentUserOwner = new MutableLiveData<>(false);
        mOwnerId = new MutableLiveData<>("UNKNOWN");
        mProjectTitle = new MutableLiveData<>("UNKNOWN");
    }

    public LiveData<List<ProjectMember>> getMembers() {
        return mMembers;
    }
    public LiveData<Boolean> isCurrentUserOwner() {
        return mIsCurrentUserOwner;
    }

    public LiveData<String> getmOwnerId() {
        return mOwnerId;
    }

    public MutableLiveData<String> getmProjectTitle() {
        return mProjectTitle;
    }

    public void fetchProjectMembers(String projectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("projects").document(projectId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("ProjectMembersViewModel", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Map<String, Object> membersMap = (Map<String, Object>) snapshot.get("members");

                        DocumentReference ownerRef = snapshot.getDocumentReference("owner");
                        mOwnerId.setValue(ownerRef.getId());

                        mProjectTitle.setValue(snapshot.getString("title"));

                        List<ProjectMember> members = new ArrayList<>();
                        AtomicInteger fetchedMembers = new AtomicInteger(0);

                        Timestamp createdDate = snapshot.getTimestamp("created_date");
                        HashMap<String, Object> ownerMap = new HashMap<>();
                        ownerMap.put("date_joined", createdDate);
                        ownerMap.put("role", "owner");

                        membersMap.put(ownerRef.getId(), ownerMap);

                        for (String userId : membersMap.keySet()) {
                            Map<String, Object> memberData = (Map<String, Object>) membersMap.get(userId);
                            Timestamp dateJoinedTimestamp = (Timestamp) memberData.get("date_joined");
                            String role = (String) memberData.get("role");

                            // Fetch user data for each member
                            db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        String displayName = userSnapshot.getString("displayName");

                                        ProjectMember member = new ProjectMember(userId, displayName, dateJoinedTimestamp, role);
                                        members.add(member);

                                        if (fetchedMembers.incrementAndGet() == membersMap.size()) {
                                            mMembers.setValue(members);
                                        }
                                    })
                                    .addOnFailureListener(userError -> Log.w("ProjectMembersViewModel", "Error fetching user data", userError));
                        }
                    } else {
                        Log.d("ProjectMembersViewModel", "Current data: null");
                    }
                });
    }

    public void checkCurrentUserOwnership(String projectId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            db.collection("projects").document(projectId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            DocumentReference ownerRef = documentSnapshot.getDocumentReference("owner");

                            if (ownerRef != null && ownerRef.getId().equals(currentUserId)) {
                                mIsCurrentUserOwner.setValue(true);
                            } else {
                                mIsCurrentUserOwner.setValue(false);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.w("ProjectMembersViewModel", "Error checking ownership", e));
        }
    }

    public void removeMember(String projectId, String userId, boolean notify) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference projectRef = db.collection("projects").document(projectId);

        projectRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    String projectName = documentSnapshot.getString("title");

                    projectRef.update("members." + userId, FieldValue.delete())
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ProjectMembersViewModel", "Member removed successfully");
                                // Get the current user's ID and display name
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                String actorUserId = currentUser.getUid();
                                String actorName = currentUser.getDisplayName();

                                // Get the removed user's display name
                                db.collection("users").document(userId).get()
                                        .addOnSuccessListener(docSnap -> {
                                            String objectUserName = docSnap.getString("displayName");
                                            // Send the notification
                                            if(notify){
                                                MainActivity.sendProjectOperationNotification(actorUserId, actorName, projectId, projectName, "REMOVE", userId, objectUserName);
                                            }

                                            // Remove the document from the 'projectAccesses' subcollection of the removed user
                                            db.collection("users").document(userId)
                                                    .collection("projectAccesses").document(projectId)
                                                    .delete()
                                                    .addOnSuccessListener(aVoid1 -> Log.d("ProjectMembersViewModel", "Project access removed successfully for user"))
                                                    .addOnFailureListener(e -> Log.w("ProjectMembersViewModel", "Error removing project access for user", e));
                                        })
                                        .addOnFailureListener(e -> Log.w("ProjectMembersViewModel", "Error getting removed user's display name", e));
                            })
                            .addOnFailureListener(e -> Log.w("ProjectMembersViewModel", "Error removing member", e));
                }
            }
        });
    }



    public void leaveProject(String projectId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            removeMember(projectId, currentUserId, false);

            // Remove the document from the 'projectAccesses' subcollection of the current user
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                    .collection("projectAccesses").document(projectId)
                    .delete();
        }
    }
}
