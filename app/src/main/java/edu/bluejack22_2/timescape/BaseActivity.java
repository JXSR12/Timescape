package edu.bluejack22_2.timescape;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    }

    protected void startServiceIfNotStarted(){
        if (!notificationListenerServiceStarted) {
            startNotificationListenerService();
            notificationListenerServiceStarted = true;
        }
    }

    protected void startNotificationListenerService() {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) return;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(this, NotificationListenerService.class);
            serviceIntent.putExtra("inputExtra", "Running Notification Listener");
            ContextCompat.startForegroundService(this, serviceIntent);
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

    public void signOut() {
        setUserOnlineStatus(false);
        FirebaseAuth.getInstance().signOut();
    }

    private void setUserOnlineStatus(boolean online) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if(FirebaseAuth.getInstance().getCurrentUser() != null){
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference statusRef = db.collection("status").document(currentUserId);
            statusRef.set(Collections.singletonMap("online", online), SetOptions.merge());
        }
    }
}

