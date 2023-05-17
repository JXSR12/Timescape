package edu.bluejack22_2.timescape;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectMember;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.dashboard.ProjectsViewModel;
import edu.bluejack22_2.timescape.ui.dashboard.SearchResultAdapter;

public class ProjectMembersActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private ProjectMembersAdapter mAdapter;
    private ProjectMembersViewModel mViewModel;
    private String mProjectId;
    private Project project;
    private boolean mIsCurrentUserOwner;

    private Set<String> projectMemberIds = new HashSet<>();
    private Button mInviteMemberButton, mProjectInviteLinkButton, mLeaveOrDeleteProjectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_members);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Get the projectId from the intent
        mProjectId = getIntent().getStringExtra("projectId");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("projects")
                .document(mProjectId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }
                    if (value != null && value.exists()) {
                        project = value.toObject(Project.class);
                    }
                });

        mRecyclerView = findViewById(R.id.member_recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mInviteMemberButton = findViewById(R.id.invite_member_button);
        mProjectInviteLinkButton = findViewById(R.id.project_invite_link_button);
        mLeaveOrDeleteProjectButton = findViewById(R.id.leave_or_delete_project_button);

        mViewModel = new ViewModelProvider(this).get(ProjectMembersViewModel.class);

        mAdapter = new ProjectMembersAdapter(new ArrayList<>(), this, "", userId -> mViewModel.removeMember(mProjectId, userId));
        mRecyclerView.setAdapter(mAdapter);

        mViewModel.getmOwnerId().observe(this, ownerId -> {
            mAdapter.setOwnerId(ownerId);
            mIsCurrentUserOwner = ownerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
            mAdapter.setIsCurrentUserOwner(mIsCurrentUserOwner);
            mLeaveOrDeleteProjectButton.setText(mIsCurrentUserOwner ? getString(R.string.delete_project) : getString(R.string.leave_project));
        });

        mViewModel = new ViewModelProvider(this).get(ProjectMembersViewModel.class);

        mViewModel.getMembers().observe(this, members -> {
            mAdapter.updateMembers(members);
            projectMemberIds.clear();
            for(ProjectMember m : members){
                projectMemberIds.add(m.getUserId());
            }
        });

        mViewModel.getmProjectTitle().observe(this, title -> {
            getSupportActionBar().setTitle(title);
        });

        mViewModel.isCurrentUserOwner().observe(this, isOwner -> {
            mAdapter.setIsCurrentUserOwner(isOwner);
        });

        mViewModel.fetchProjectMembers(mProjectId);
        mViewModel.checkCurrentUserOwnership(mProjectId);
        setupClickListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Handle the back button click event
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupClickListeners() {
        mInviteMemberButton.setOnClickListener(v -> {
            if(!project.isPrivate() || project.getOwner().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                showSearchUserDialog();
            }else{
                Snackbar.make(mInviteMemberButton, R.string.only_project_owner_can_invite_members_in_private_projects, BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });

        mProjectInviteLinkButton.setOnClickListener(v -> {
            if(!project.isPrivate() || project.getOwner().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                showInviteLinkDialog();
            }else{
                Snackbar.make(mInviteMemberButton, R.string.only_project_owner_can_generate_invite_links_in_private_projects, BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });

        mLeaveOrDeleteProjectButton.setOnClickListener(v -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (mIsCurrentUserOwner) {
                        deleteProject(mProjectId);
                    } else {
                        mViewModel.leaveProject(mProjectId);
                        closeActivityAndNotifyOthers();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.are_you_sure_you_want_to) + (mIsCurrentUserOwner ? getString(R.string.delete_2) : getString(R.string.leave)) + getString(R.string.this_project))
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener)
                    .show();
        });
    }

    private void showInviteLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_invite_link, null);
        builder.setView(view);

        ProgressBar progressBar = view.findViewById(R.id.invite_link_progress);
        TextView inviteLinkTitle = view.findViewById(R.id.invite_link_title);
        EditText inviteLinkEditText = view.findViewById(R.id.invite_link_edit_text);
        Button shareInviteLinkButton = view.findViewById(R.id.share_invite_link_button);
        Button copyInviteLinkButton = view.findViewById(R.id.copy_invite_link_button);

        inviteLinkEditText.setVisibility(View.GONE);
        shareInviteLinkButton.setVisibility(View.GONE);
        copyInviteLinkButton.setVisibility(View.GONE);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Generate Firebase Dynamic Link
        createDynamicLink()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Uri dynamicLinkUri = task.getResult().getShortLink();

                        progressBar.setVisibility(View.GONE);
                        inviteLinkTitle.setText(R.string.project_invite_link);
                        inviteLinkEditText.setVisibility(View.VISIBLE);
                        inviteLinkEditText.setText(dynamicLinkUri.toString());
                        shareInviteLinkButton.setVisibility(View.VISIBLE);
                        copyInviteLinkButton.setVisibility(View.VISIBLE);

                        shareInviteLinkButton.setOnClickListener(v -> {
                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, dynamicLinkUri.toString());
                            shareIntent.setType("text/plain");
                            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_project_invite_link)));
                        });

                        copyInviteLinkButton.setOnClickListener(v -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText(getString(R.string.project_invite_link_2), dynamicLinkUri.toString());
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(this, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        // Error generating Dynamic Link
                        Toast.makeText(this, R.string.error_generating_invite_link_please_try_again, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });

    }

    private Task<ShortDynamicLink> createDynamicLink() {
        String deepLink = "https://timescapxtnsn.page.link/project_invite?projectId=" + mProjectId;
        String fallbackUrl = "https://sites.google.com/view/timescape-landing/home";

        DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(deepLink))
                .setDomainUriPrefix("https://timescapxtnsn.page.link")
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder()
                                .setFallbackUrl(Uri.parse(fallbackUrl))
                                .build())
                .buildDynamicLink();

        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(dynamicLink.getUri())
                .buildShortDynamicLink()
                .addOnFailureListener(e -> {
                    // Handle errors here.
                });

        return shortLinkTask;
    }



    private void deleteProject(String projectId) {
        // Delete the project document from the 'projects' collection
        FirebaseFirestore.getInstance().collection("projects").document(projectId).delete();

        // Delete the chat document from the 'chats' collection
        FirebaseFirestore.getInstance().collection("chats").document(projectId).delete();

        // Delete all documents in the 'tasks' collection that have a 'project' field referencing the deleted project
        FirebaseFirestore.getInstance().collection("tasks")
                .whereEqualTo("project", FirebaseFirestore.getInstance().collection("projects").document(projectId))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        for (DocumentSnapshot document : task.getResult()) {
                            batch.delete(document.getReference());
                        }
                        batch.commit();
                    }
                });

        // Remove the projectId from the 'project_accesses' array field of all users in the 'users' collection
        FirebaseFirestore.getInstance().collection("users")
                .whereArrayContains("project_accesses", projectId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        for (DocumentSnapshot document : task.getResult()) {
                            batch.update(document.getReference(), "project_accesses", FieldValue.arrayRemove(projectId));
                        }
                        batch.commit().addOnCompleteListener(batchTask -> {
                            if (batchTask.isSuccessful()) {
                                closeActivityAndNotifyOthers();
                            }
                        });
                    }
                });
    }

    private void showSearchUserDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = ProjectMembersActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search_user, null);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ProjectMembersActivity.this);
        builder.setView(dialogView)
                .setTitle(R.string.search_user);

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText searchEditText = dialogView.findViewById(R.id.search_user_input);
        RecyclerView searchRecyclerView = dialogView.findViewById(R.id.search_user_list);
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(ProjectMembersActivity.this));

        // Implement the search functionality
        performSearch(dialogView, searchEditText, searchRecyclerView, Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
    }

    private void performSearch(View dialogView, EditText searchEditText, RecyclerView searchRecyclerView, String currentUserId) {
        final Handler handler = new Handler();
        final Runnable searchRunnable = new Runnable() {
            @Override
            public void run() {
                String query = searchEditText.getText().toString().trim();
                if (query.length() >= 3) {
                    // Display the loading indicator
                    ProgressBar loading = dialogView.findViewById(R.id.loading);
                    loading.setVisibility(View.VISIBLE);

                    // Perform the search and update the searchRecyclerView
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Task<QuerySnapshot> displayNameQuery = db.collection("users")
                            .orderBy("displayName")
                            .startAt(query)
                            .endAt(query + "\uf8ff")
                            .limit(10)
                            .get();

                    Task<QuerySnapshot> emailQuery = db.collection("users")
                            .orderBy("email")
                            .startAt(query)
                            .endAt(query + "\uf8ff")
                            .limit(10)
                            .get();

                    Task<QuerySnapshot> phoneNumberQuery = db.collection("users")
                            .orderBy("phoneNumber")
                            .startAt(query)
                            .endAt(query + "\uf8ff")
                            .limit(10)
                            .get();

                    Task<List<QuerySnapshot>> allQueries = Tasks.whenAllSuccess(displayNameQuery, emailQuery, phoneNumberQuery);

                    allQueries.addOnSuccessListener(querySnapshots -> {
                        List<User> searchResults = new ArrayList<>();
                        Set<String> userIds = new HashSet<>();

                        for (QuerySnapshot querySnapshot : querySnapshots) {
                            for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null && !userIds.contains(user.getUid()) && !user.getUid().equals(currentUserId)) {
                                    userIds.add(user.getUid());
                                    searchResults.add(user);
                                }
                            }
                        }

                        EPSearchResultAdapter searchResultAdapter = new EPSearchResultAdapter(ProjectMembersActivity.this, searchResults, userIds, projectMemberIds, mProjectId);
                        searchRecyclerView.setAdapter(searchResultAdapter);

                        // Hide the loading indicator and update the visibility of the "No matching users" TextView
                        loading.setVisibility(View.GONE);
                        TextView noMatchingUsers = dialogView.findViewById(R.id.no_matching_users);
                        if (searchResults.isEmpty()) {
                            noMatchingUsers.setVisibility(View.VISIBLE);
                        } else {
                            noMatchingUsers.setVisibility(View.GONE);
                        }
                    });
                } else {
                    TextView noMatchingUsers = dialogView.findViewById(R.id.no_matching_users);
                    noMatchingUsers.setVisibility(View.GONE);
                }
            }
        };

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.postDelayed(searchRunnable, 500); // Set debounce delay to 500ms
            }
        });
    }


    private void closeActivityAndNotifyOthers() {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseFirestore.getInstance().collection("projects").document(getIntent().getStringExtra("projectId")).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(!documentSnapshot.exists()){
                    finish();
                }else{
                    DocumentReference ownerRef = documentSnapshot.getDocumentReference("owner");
                    HashMap<String, Map<String, Object>> membersMap = (HashMap<String, Map<String, Object>>) documentSnapshot.get("members");
                    if(!ownerRef.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) && !membersMap.containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        finish();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                finish();
            }
        });
    }
}
