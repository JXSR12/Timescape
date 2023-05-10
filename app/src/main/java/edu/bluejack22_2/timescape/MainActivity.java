package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import edu.bluejack22_2.timescape.databinding.ActivityMainBinding;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectMember;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.dashboard.DashboardViewModel;
import edu.bluejack22_2.timescape.ui.dashboard.InvitedMemberAdapter;
import edu.bluejack22_2.timescape.ui.dashboard.SearchResultAdapter;

public class MainActivity extends BaseActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private DashboardViewModel dashboardViewModel;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    TextView quickViewUserName, quickViewUserEmail;
    ImageView quickViewProfilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.navViewTop.getHeaderView(0);

        quickViewProfilePic = headerView.findViewById(R.id.userProfileIcon);
        quickViewUserName = headerView.findViewById(R.id.textViewUserName);
        quickViewUserEmail = headerView.findViewById(R.id.textViewUserEmail);

        mAuth = FirebaseAuth.getInstance();

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Add project form
                showAddProjectDialog();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;

        NavigationView navigationViewTop = binding.navViewTop;
        NavigationView navigationViewBottom = binding.navViewBottom;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_notifs, R.id.nav_chats, R.id.nav_account, R.id.nav_logout)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationViewTop, navController);
        NavigationUI.setupWithNavController(navigationViewBottom, navController);
        binding.navViewTop.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        binding.navViewBottom.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        mAuthStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                updateUI(user);
            } else {
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        };

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        handleProjectInviteDeeplink(getIntent());
    }

    private void onDialogClosed(){
        dashboardViewModel.setDialogDismissed(true);
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            this.signOut();
            return true;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        EditText titleEditText = dialogView.findViewById(R.id.title_input);
        EditText descriptionEditText = dialogView.findViewById(R.id.description_input);
        EditText deadlineDateEditText = dialogView.findViewById(R.id.deadline_input);
        SwitchMaterial visibilitySwitch = dialogView.findViewById(R.id.visibility_switch);
        Button createProjectButton = dialogView.findViewById(R.id.create_project_button);

        List<User> invitedMembers = new ArrayList<>();
        InvitedMemberAdapter adapter = new InvitedMemberAdapter(invitedMembers);
        RecyclerView invitedMembersRecyclerView = dialogView.findViewById(R.id.invited_members_list);
        invitedMembersRecyclerView.setAdapter(adapter);
        invitedMembersRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));


        Button addMemberButton = dialogView.findViewById(R.id.add_member_button);
        addMemberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchUserDialog(invitedMembers, adapter);
            }
        });


        // Set up the DatePicker and TimePicker dialogs
        final Calendar calendar = Calendar.getInstance();
        deadlineDateEditText.setOnClickListener(v -> {
            new DatePickerDialog(MainActivity.this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                new TimePickerDialog(MainActivity.this, (timeView, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
                    deadlineDateEditText.setText(sdf.format(calendar.getTime()));
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = builder.create();

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // Call the onDialogClosed() method when the dialog is dismissed
                onDialogClosed();
            }
        });

        createProjectButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String deadlineDateStr = deadlineDateEditText.getText().toString().trim();
            boolean isPrivate = visibilitySwitch.isChecked();

            if (title.isEmpty() || description.isEmpty() || deadlineDateStr.isEmpty()) {
                Snackbar.make(dialogView, "All fields must be filled", Snackbar.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            DocumentReference ownerRef = currentUser != null ? db.collection("users").document(currentUser.getUid()) : null;

            Timestamp deadlineDate = new Timestamp(calendar.getTime());
            Timestamp createdDate = Timestamp.now();

            Map<String, Object> members = new HashMap<>();

            for (User member : invitedMembers) {
                Map<String, Object> memberData = new HashMap<>();
                memberData.put("date_joined", Timestamp.now()); // Member automatically joins, can leave any time.
                memberData.put("role", "collaborator");
                members.put(member.getUid(), memberData);
            }

                // Create the project document with the added members
                Map<String, Object> projectData = new HashMap<>();
                projectData.put("title", title);
                projectData.put("description", description);
                projectData.put("deadline_date", deadlineDate);
                projectData.put("private", isPrivate);
                projectData.put("owner", ownerRef);
                projectData.put("created_date", createdDate);
                projectData.put("last_modified_date", createdDate);
                projectData.put("members", members);

                db.collection("projects")
                        .add(projectData)
                        .addOnSuccessListener(documentReference -> {
                            // Close the dialog and show success message
                            db.collection("projects").document(documentReference.getId()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    String projectName = documentSnapshot.getString("title");
                                    for (User member : invitedMembers) {
                                        String actorUserId = currentUser.getUid();
                                        String actorName = currentUser.getDisplayName();
                                        // Get the added user's display name
                                        db.collection("users").document(member.getUid()).get()
                                                .addOnSuccessListener(docSnap -> {
                                                    String objectUserName = docSnap.getString("displayName");
                                                    // Send the notification
                                                    sendProjectOperationNotification(actorUserId, actorName, documentReference.getId(), "new project " + projectName, "ADD", member.getUid(), objectUserName);
                                                })
                                                .addOnFailureListener(e -> Log.w("ProjectCreation", "Error getting added user's display name", e));
                                    }
                                }
                            });

                            dialog.dismiss();
                            Snackbar.make(dialogView, "Project created successfully!", Snackbar.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            // Show error message
                            Snackbar.make(dialogView, "Error creating project: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
            });

        dialog.show();
    }

    private void showSearchUserDialog(List<User> invitedMembers, InvitedMemberAdapter adapter) {
        // Inflate the dialog layout
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search_user, null);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView)
                .setTitle("Search User");

        AlertDialog dialog = builder.create();
        dialog.show();

        EditText searchEditText = dialogView.findViewById(R.id.search_user_input);
        RecyclerView searchRecyclerView = dialogView.findViewById(R.id.search_user_list);
        searchRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        // Implement the search functionality and update the invited members list
        performSearch(dialogView, searchEditText, searchRecyclerView, invitedMembers, adapter, Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
    }


    private void performSearch(View dialogView, EditText searchEditText, RecyclerView searchRecyclerView, List<User> invitedMembers, InvitedMemberAdapter invitedMemberAdapter, String currentUserId) {
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

                        SearchResultAdapter searchResultAdapter = new SearchResultAdapter(MainActivity.this, searchResults, invitedMembers, invitedMemberAdapter);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleProjectInviteDeeplink(intent);
    }

    private void handleProjectInviteDeeplink(Intent intent) {
        Uri deepLinkUri = intent.getData();
        if (deepLinkUri != null) {
            String projectId = deepLinkUri.getQueryParameter("projectId");
            if (projectId != null && !projectId.isEmpty()) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                DocumentReference projectRef = db.collection("projects").document(projectId);
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                projectRef.get().addOnSuccessListener(documentSnapshot -> {
                    Project project = documentSnapshot.toObject(Project.class);
                    if (project != null) {
                        Map<String, Map<String, Object>> members = project.getMembers();
                        DocumentReference ownerRef = project.getOwner();

                        if (ownerRef.getId().equals(userId) || (members != null && members.containsKey(userId))) {
                            Toast.makeText(this, "You are already in the project", Toast.LENGTH_SHORT).show();
                            Intent detailIntent = new Intent(MainActivity.this, ProjectDetailActivity.class);
                            detailIntent.putExtra("PROJECT_ID", projectId);
                            startActivity(detailIntent);
                        } else {
                            // Add user as a new member
                            DocumentReference userRef = db.collection("users").document(userId);
                            ProjectMember newMember = new ProjectMember(userRef.getId(), "", Timestamp.now(), "collaborator");

                            projectRef.update("members." + userRef.getId(), newMember)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "You have been added to the project", Toast.LENGTH_SHORT).show();
                                        Intent detailIntent = new Intent(MainActivity.this, ProjectDetailActivity.class);
                                        detailIntent.putExtra("PROJECT_ID", projectId);
                                        startActivity(detailIntent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to join project", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch project information", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    public static void sendProjectOperationNotification(String actorUserId, String actorName, String projectId, String projectName, String action, String objectUserId, String objectUserName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference notificationRef = db.collection("notifications").document(objectUserId);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("actorUserId", actorUserId);
        notificationData.put("actorName", actorName);
        notificationData.put("projectId", projectId);
        notificationData.put("projectName", projectName);
        notificationData.put("action", action);
        notificationData.put("objectUserId", objectUserId);
        notificationData.put("objectUserName", objectUserName);

        notificationRef.update("notifs", FieldValue.arrayUnion(notificationData))
                .addOnSuccessListener(aVoid -> Log.d("Notification", "Notification sent successfully"))
                .addOnFailureListener(e -> {
                    Log.w("Notification", "Error sending notification", e);
                    // If the document doesn't exist, create it with the 'notifs' field
                    notificationRef.set(Collections.singletonMap("notifs", Collections.singletonList(notificationData)));
                });
    }



    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    private void getUserData(FirebaseUser user) {
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String displayName = documentSnapshot.getString("displayName");
                            String email = documentSnapshot.getString("email");

                            quickViewUserName.setText(displayName);
                            quickViewUserEmail.setText(email);
                        } else {
                            Log.w(TAG, "No such document");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error getting user data", e);
                    });
        }
    }




    private void updateUI(FirebaseUser user) {
        boolean isGoogleAccount = false;

        for (UserInfo userInfo : user.getProviderData()) {
            if (userInfo.getProviderId().equals("google.com")) {
                isGoogleAccount = true;
                break;
            }
        }

        if (isGoogleAccount) {
            getUserData(user);

            Glide.with(this)
                    .asBitmap()
                    .load(user.getPhotoUrl())
                    .apply(new RequestOptions().override(quickViewProfilePic.getWidth(), quickViewProfilePic.getHeight()))
                    .centerCrop()
                    .circleCrop()
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            runOnUiThread(() -> quickViewProfilePic.setImageBitmap(resource));
                            return false;
                        }
                    })
                    .submit();
        } else {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("users").document(user.getUid());

            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        quickViewUserName.setText(document.getString("displayName"));
                        quickViewUserEmail.setText(user.getEmail());
                    } else {
                        Log.d("Firestore", "No such document");
                    }
                } else {
                    Log.d("Firestore", "get failed with ", task.getException());
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startServiceIfNotStarted();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}