package edu.bluejack22_2.timescape;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.content.ContentValues.TAG;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.services.NotificationListenerService;

public abstract class BaseActivity extends AppCompatActivity {

    public static final String ACTION_REPLY = "edu.bluejack22_2.timescape.ACTION_REPLY";
    public static final String ACTION_MUTE = "edu.bluejack22_2.timescape.ACTION_MUTE";
    public static final String EXTRA_MESSAGE_ID = "edu.bluejack22_2.timescape.EXTRA_MESSAGE_ID";
    public static final String EXTRA_PROJECT_ID = "edu.bluejack22_2.timescape.EXTRA_PROJECT_ID";
    public static final String EXTRA_PROJECT_NAME = "edu.bluejack22_2.timescape.EXTRA_PROJECT_NAME";
    public static final String EXTRA_REPLY = "edu.bluejack22_2.timescape.EXTRA_REPLY";
    private static boolean notificationListenerServiceStarted = false;
    public static final int REQUEST_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startServiceIfNotStarted();
        updateFCMToken();
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
    }

    private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
            ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            setUserOnlineStatus(false);
        }
    };

    protected void startServiceIfNotStarted(){
        if (!notificationListenerServiceStarted) {
            startNotificationListenerService();
            notificationListenerServiceStarted = true;
        }
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

    protected void startNotificationListenerService() {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) return;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            Intent serviceIntent = new Intent(this, NotificationListenerService.class);
//            serviceIntent.putExtra("inputExtra", "Running Notification Listener");
//            ContextCompat.startForegroundService(this, serviceIntent);
//            Toast.makeText(this, "[DEV] Notification Listener Service is suspended, we are testing new FCM based notification system.", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotificationListenerService();
            } else {
                // Show a message to the user explaining why the permission is needed
                Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setUserOnlineStatus(false);
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

