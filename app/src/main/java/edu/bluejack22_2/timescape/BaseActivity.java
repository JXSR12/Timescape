package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Collections;
import java.util.HashMap;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String ACTION_REPLY = "edu.bluejack22_2.timescape.ACTION_REPLY";
    public static final String ACTION_MUTE = "edu.bluejack22_2.timescape.ACTION_MUTE";
    public static final String EXTRA_MESSAGE_ID = "edu.bluejack22_2.timescape.EXTRA_MESSAGE_ID";
    public static final String EXTRA_PROJECT_ID = "edu.bluejack22_2.timescape.EXTRA_PROJECT_ID";
    public static final String EXTRA_PROJECT_NAME = "edu.bluejack22_2.timescape.EXTRA_PROJECT_NAME";
    public static final String EXTRA_REPLY = "edu.bluejack22_2.timescape.EXTRA_REPLY";
    private static boolean notificationListenerServiceStarted = false;
    public static final int REQUEST_CODE = 1;

    private static final long DELAY_MS = 1000L;

    private static Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable offlineRunnable;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private View warningOverlay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestNecessaryPermissions();
        updateFCMToken();
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);

        checkConnection();
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Create a NetworkCallback
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Network connection is available, hide warning
                runOnUiThread(() -> {
                    hideWarningOverlay();
                    Log.d("CONNECTION AVAILABLE", "RECONNECTED");
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Network connection is lost, show warning
                runOnUiThread(() -> {
                    showWarningOverlay();
                    Log.d("CONNECTION UNAVAILABLE", "DISCONNECTED");
                });
            }
        };
    }

    private Dialog warningDialog;

    public void showWarningOverlay() {
        if (warningDialog == null) {
            warningDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            warningDialog.setContentView(R.layout.layout_warning_overlay);
            warningDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            warningDialog.setCancelable(false);

            Button continueButton = warningDialog.findViewById(R.id.continueButton);
            continueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    warningDialog.dismiss();
                }
            });

        }

        if (!warningDialog.isShowing()) {
            warningDialog.show();
        }
    }


    public void hideWarningOverlay() {
        if (warningDialog != null && warningDialog.isShowing()) {
            warningDialog.dismiss();
        }
    }

    private void checkConnection() {
        if(!isNetworkConnected()) {
            showWarningOverlay();
        } else {
            hideWarningOverlay();
        }
    }
    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
            ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            setUserOnlineStatus(false);
        }
    };

    protected void requestNecessaryPermissions(){
        requestNotificationsPermission();
    }

    private void updateFCMToken() {
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                                return;
                            }

                            // Get new FCM registration token
                            String token = task.getResult();

                            // Update the token for the current user in Firestore
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DocumentReference userRef = db.collection("users").document(currentUserId);
                            userRef.update("fcmToken", token)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "FCM Token updated successfully for user " + currentUserId);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error updating FCM Token for user " + currentUserId, e);
                                        }
                                    });
                        }
                    });
        }
    }

    protected void requestNotificationsPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, continue as usual
                requestNotificationsPermission();
            } else {
                // permission denied
                if (!shouldShowRequestPermissionRationale(permissions[0])) {
                    // user also checked "never ask again"
                    new AlertDialog.Builder(this)
                            .setMessage("We recommend you to turn on notifications permission before using the app. You can grant them in app settings.")
                            .setPositiveButton("Go to settings", (dialog, which) -> {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                } else {
                    Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (offlineRunnable != null) {
            handler.removeCallbacks(offlineRunnable);
        }
        setUserOnlineStatus(true);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        offlineRunnable = new Runnable() {
            @Override
            public void run() {
                setUserOnlineStatus(false);
            }
        };
        handler.postDelayed(offlineRunnable, DELAY_MS);
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    void signOut() {
        // Get current user's ID
        setUserOnlineStatus(false);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get Firestore instance and user's document reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(currentUserId);

        // Update the fcmToken field for the user to null or empty
        userRef.update("fcmToken", FieldValue.delete())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "FCM Token removed successfully for user " + currentUserId);

                        // Then sign out the user from Firebase Auth
                        FirebaseAuth.getInstance().signOut();
                        FirebaseMessaging.getInstance().deleteToken()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Token deleted successfully");
                                        // You may want to fetch a new token here and associate it with the user after login.
                                    } else {
                                        Log.w(TAG, "Token deletion failed", task.getException());
                                    }
                                }
                            });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error removing FCM Token for user " + currentUserId, e);
                    }
                });
    }


    private void setUserOnlineStatus(boolean online) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference statusRef = db.collection("status").document(currentUserId);
            statusRef.set(Collections.singletonMap("online", online), SetOptions.merge());
            if(!(this instanceof ProjectChatActivity)){
                setActiveChat(false);
            }
        }
    }

    private void setActiveChat(boolean isActive) {
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("activeChats").document(currentUserId)
                    .set(isActive ? Collections.singletonMap("projectId", "ALL") : new HashMap<>());
        }
    }
}

