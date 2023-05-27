package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.bluejack22_2.timescape.model.User;

public class UserProfileViewModel extends ViewModel {
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();

    public void setUser(User user){
        userLiveData.setValue(user);
    }

    public MutableLiveData<User> getUserLiveData() {
        return userLiveData;
    }

    private MutableLiveData<String> updateMessage = new MutableLiveData<>();

    public MutableLiveData<String> getUpdateMessage() {
        return updateMessage;
    }

    public void fetchUserDetails() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();

            // Reference to Firestore collection
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Get user details from Firestore
            db.collection("users").document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Fetch user details from Firestore
                                String displayName = document.getString("displayName");
                                String email = document.getString("email");
                                String phoneNumber = document.getString("phoneNumber");

                                User user = new User(uid, displayName, email, phoneNumber);
                                setUser(user);
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    });
        }
    }


    public void updateDisplayName(Context ctx, String newDisplayName) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(newDisplayName.trim().isEmpty()){
            updateMessage.setValue(ctx.getString(R.string.display_name_cannot_be_empty));
            return;
        }
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                            // Update display name in Firestore
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference userRef = db.collection("users").document(user.getUid());
                            userRef.update("displayName", newDisplayName)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                                        updateMessage.setValue(ctx.getString(R.string.successfully_changed_display_name));
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "Error updating document", e));
                            fetchUserDetails();
                        }
                    });
        }
    }


    public void changePassword(Context ctx, String oldPassword, String newPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // Re-authenticate user
            AuthCredential credential = EmailAuthProvider
                    .getCredential(user.getEmail(), oldPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d(TAG, "User password updated.");
                                            updateMessage.setValue(ctx.getString(R.string.password_changed_successfully));
                                        }
                                    });
                        } else {
                            updateMessage.setValue(ctx.getString(R.string.the_old_password_you_provided_is_incorrect));
                        }
                    });
        }
    }

}
