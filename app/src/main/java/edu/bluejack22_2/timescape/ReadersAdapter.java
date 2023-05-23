package edu.bluejack22_2.timescape;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Random;

public class ReadersAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> userIds;

    public ReadersAdapter(Context context, List<String> userIds) {
        super(context, R.layout.reader_item, userIds);
        this.context = context;
        this.userIds = userIds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.reader_item, parent, false);
        ImageView avatar = (ImageView) rowView.findViewById(R.id.avatar);
        TextView displayName = (TextView) rowView.findViewById(R.id.display_name);

        // Fetch the user data for the reader at this position
        String userId = userIds.get(position);
        // Assume you have a method to fetch user data by user id and set the avatar and display name
        fetchUserData(userId, avatar, displayName);

        return rowView;
    }

    private void fetchUserData(String userId, ImageView avatar, TextView displayName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String displayNameString = documentSnapshot.getString("displayName");
                if (displayNameString != null && !displayNameString.isEmpty()) {
                    displayName.setText(displayNameString);
                } else {
                    displayName.setText("UNKNOWN USER");
                }
            } else {
                displayName.setText("UNKNOWN USER");
            }
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Error fetching user data", e);
            displayName.setText("UNKNOWN USER");
        });

        // Generate an avatar and set it
        TextDrawable avatarDrawable = generateAvatar(userId);
        avatar.setImageDrawable(avatarDrawable);
    }

    private TextDrawable generateAvatar(String userId) {
        String content = "\\O/";
        Random random = new Random(userId.hashCode());
        int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return TextDrawable.builder().buildRound(content, color);
    }
}

