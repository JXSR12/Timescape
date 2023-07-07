package edu.bluejack22_2.timescape2;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;

public class TimescapeApp extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }


    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        setUserOnlineStatus(false);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        setUserOnlineStatus(true);
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

