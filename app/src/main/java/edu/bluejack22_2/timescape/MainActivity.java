package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Menu;
import android.widget.FrameLayout;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import edu.bluejack22_2.timescape.databinding.ActivityMainBinding;
import edu.bluejack22_2.timescape.model.ApkVersion;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectMember;
import edu.bluejack22_2.timescape.model.User;
import edu.bluejack22_2.timescape.ui.dashboard.DashboardViewModel;
import edu.bluejack22_2.timescape.ui.dashboard.InvitedMemberAdapter;
import edu.bluejack22_2.timescape.ui.dashboard.SearchResultAdapter;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;


public class MainActivity extends BaseActivity {
    public static final int INSTALL_PERMISSION_REQUEST_CODE = 123;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private DashboardViewModel dashboardViewModel;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    TextView quickViewUserName, quickViewUserEmail;
    ImageView quickViewProfilePic;

    AlertDialog showQrDialog;

    final boolean[] isDeepLinkProcessed = {false};

    UpdaterViewModel updaterViewModel;
    ApkVersion latestApkVersion;

    ActivityResultLauncher<Intent> mStartForResult;

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
        binding.appBarMain.fabJoinQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Add project form
                showScanProjectQRDialog();
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
        binding.navViewTop.setNavigationItemSelectedListener(item1 -> onNavigationItemSelected(item1));
        binding.navViewBottom.setNavigationItemSelectedListener(item -> onNavigationItemSelected(item));

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

        updaterViewModel = new ViewModelProvider(this).get(UpdaterViewModel.class);
        updaterViewModel.getLatestApkVersion().observe(this, latestApkVersion2 -> checkAndUpdate(latestApkVersion2, false));

        mStartForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updaterViewModel.getLatestApkVersion().observe(this, latestApkVersion1 -> checkAndUpdate(latestApkVersion1, true));
                }
            });
    }

    private void checkAndUpdate(ApkVersion latestApkVersion, boolean showToast) {
        PackageInfo pInfo;
        try {
             pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        int currentVersionCode = pInfo != null ? pInfo.versionCode : 0;

        this.latestApkVersion = latestApkVersion;
        if (latestApkVersion.getVersionCode() > currentVersionCode) {
            showUpdateDialog(latestApkVersion);
        }else{
            if(showToast){
                Toast.makeText(this, R.string.you_are_running_the_latest_available_version_of_timescape, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create an instance of ActivityResultLauncher
    private final ActivityResultLauncher<Intent> settingsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(this, R.string.permission_not_granted_can_t_install_new_version_of_the_app, Toast.LENGTH_LONG).show();
                } else {
                    downloadAndInstallApk(latestApkVersion.getFileUrl());
                }
            }
    );

    private void showUpdateDialog(ApkVersion latestApkVersion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.timescape_updater);
        builder.setMessage("Timescape " + latestApkVersion.getVersionName() + getString(R.string.is_available_to_download_and_install_update_the_app_to_get_the_latest_features_and_fixes));
        builder.setPositiveButton(R.string.install, (dialog, which) -> {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + getPackageName()));
                settingsActivityResultLauncher.launch(intent);
            } else {
                downloadAndInstallApk(latestApkVersion.getFileUrl());
            }
        });
        builder.setNegativeButton(R.string.later, (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void downloadAndInstallApk(String fileUrl) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri uri = Uri.parse(fileUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "timescape-latest.apk");

        File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "timescape-latest.apk");
        if (apkFile.exists()) {
            apkFile.delete();
        }

        final AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Downloading latest app package")
                .setCancelable(false)
                .setView(R.layout.dialog_progress_general_text)
                .show();

        long downloadID = downloadManager.enqueue(request);
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get the downloaded file path from the DownloadManager query
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadID);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    String filePath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    File downloadedFile = new File(Uri.parse(filePath).getPath());
                    installApk(downloadedFile);
                }
                cursor.close();

                progressDialog.dismiss();
                context.unregisterReceiver(this);
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }






    private void installApk(File apkFile) {
        {
            Uri apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", apkFile);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }


    private void onDialogClosed() {
        dashboardViewModel.setDialogDismissed(true);
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            this.signOut();
            return true;
        }

        if (id == R.id.nav_account) {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            mStartForResult.launch(intent);

            return true;
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    private void showScanProjectQRDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_scan_qr, null);
        builder.setView(view);

        ProgressBar qrCodeProgress = view.findViewById(R.id.qr_code_progress);
        TextView qrCodeStatus = view.findViewById(R.id.qr_code_status);

        qrCodeProgress.setVisibility(View.GONE);
        qrCodeStatus.setVisibility(View.GONE);

        showQrDialog = builder.create();
        showQrDialog.setCancelable(true);
        showQrDialog.show();

        // Check camera permission
        setupCamera(showQrDialog, qrCodeProgress, qrCodeStatus);
    }

    private void setupCamera(AlertDialog dialog, ProgressBar qrCodeProgress, TextView qrCodeStatus) {
        SurfaceView cameraPreview = dialog.findViewById(R.id.camera_preview);
        FrameLayout focusWindow = dialog.findViewById(R.id.focus_window);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        CameraSource cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(480, 480)
                .setAutoFocusEnabled(true)
                .build();

        SurfaceHolder holder = cameraPreview.getHolder();
        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(holder);
                        Log.d("Camera Started", "CAMERA SUCCESSFULLY STARTED");
                    }else{
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        Log.d("Camera Error", "CAMERA FAILED TO START");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Camera Exception", "Error starting camera source: " + e.getMessage());
                    Log.d("Camera Error Caught", "CAMERA FAILED TO START");
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0) {
                    String qrCodeText = qrCodes.valueAt(0).displayValue;
                    Handler handler = new Handler(Looper.getMainLooper());
                    if (qrCodeText.startsWith("https://jex.ink/c/")) {
                        handler.post(() -> {
                            qrCodeStatus.setVisibility(View.VISIBLE);
                            qrCodeStatus.setText(R.string.resolving_invite);
                            qrCodeProgress.setVisibility(View.VISIBLE);
                            focusWindow.setVisibility(View.INVISIBLE);
                        });

                        // Process the dynamic link
                        processDynamicLink(qrCodeText, dialog);
                    } else {
                        handler.post(() -> {
                            qrCodeStatus.setText(getString(R.string.invalid_qr_code));
                            qrCodeStatus.setVisibility(View.VISIBLE);
                        });
                    }
                }else{
                    qrCodeStatus.setVisibility(View.GONE);
                }
            }
        });
    }

    private void processDynamicLink(String qrCodeText, AlertDialog dialog) {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(Uri.parse(qrCodeText))
                .addOnCompleteListener(this, new OnCompleteListener<PendingDynamicLinkData>() {
                    @Override
                    public void onComplete(@NonNull Task<PendingDynamicLinkData> task) {
                        if (task.isSuccessful()) {
                            Uri deepLink = null;
                            if (task.getResult() != null) {
                                deepLink = task.getResult().getLink();
                            }

                            if (deepLink != null && !isDeepLinkProcessed[0]) {
                                // Execute the link
                                executeDynamicLink(deepLink);
                                isDeepLinkProcessed[0] = true;
                            }
                        } else {
                            Toast.makeText(MainActivity.this, R.string.error_processing_content, Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });
    }

    private void executeDynamicLink(Uri deepLink) {
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLink);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showQrDialog.dismiss();
                showScanProjectQRDialog();
                Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == INSTALL_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Toast.makeText(MainActivity.this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                // permission denied
                Toast.makeText(MainActivity.this, "Permission denied to install applications.", Toast.LENGTH_SHORT).show();
            }
        }
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
                    SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.mmm_dd_yyyy_at_hh_mm), Locale.getDefault());
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
                Snackbar.make(dialogView, R.string.all_fields_must_be_filled, Snackbar.LENGTH_SHORT).show();
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
                                                    sendProjectOperationNotification(actorUserId, actorName, documentReference.getId(), getString(R.string.new_project) + projectName, "ADD", member.getUid(), objectUserName);
                                                })
                                                .addOnFailureListener(e -> Log.w("ProjectCreation", "Error getting added user's display name", e));
                                    }
                                }
                            });

                            dialog.dismiss();
                            Snackbar.make(dialogView, R.string.project_created_successfully, Snackbar.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            // Show error message
                            Snackbar.make(dialogView, getString(R.string.error_creating_project) + e.getMessage(), Snackbar.LENGTH_LONG).show();
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
                .setTitle(R.string.search_user);

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
                            Toast.makeText(this, R.string.you_are_already_in_the_project, Toast.LENGTH_SHORT).show();
                            Intent detailIntent = new Intent(MainActivity.this, ProjectDetailActivity.class);
                            detailIntent.putExtra("PROJECT_ID", projectId);
                            startActivity(detailIntent);
                        } else {
                            // Add user as a new member
                            DocumentReference userRef = db.collection("users").document(userId);
                            ProjectMember newMember = new ProjectMember(userRef.getId(), "", Timestamp.now(), "collaborator");

                            projectRef.update("members." + userRef.getId(), newMember)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, R.string.you_have_been_added_to_the_project, Toast.LENGTH_SHORT).show();
                                        Intent detailIntent = new Intent(MainActivity.this, ProjectDetailActivity.class);
                                        detailIntent.putExtra("PROJECT_ID", projectId);
                                        startActivity(detailIntent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, R.string.failed_to_join_project, Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.failed_to_fetch_project_information, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    public static void sendProjectOperationNotification(String actorUserId, String actorName, String projectId, String projectName, String action, String objectUserId, String objectUserName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference notificationsCollection = db.collection("users").document(objectUserId).collection("notifications");

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("actorUserId", actorUserId);
        notificationData.put("actorName", actorName);
        notificationData.put("projectId", projectId);
        notificationData.put("projectName", projectName);
        notificationData.put("action", action);
        notificationData.put("objectUserId", objectUserId);
        notificationData.put("objectUserName", objectUserName);
        notificationData.put("notificationType", "PROJECT_OPERATION_NOTICE");

        notificationsCollection.add(notificationData)
                .addOnSuccessListener(documentReference -> Log.d("Notification", "Notification sent successfully"))
                .addOnFailureListener(e -> Log.w("Notification", "Error sending notification", e));
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