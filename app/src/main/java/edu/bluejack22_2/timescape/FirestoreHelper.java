package edu.bluejack22_2.timescape;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.bluejack22_2.timescape.model.InboxMessage;

public class FirestoreHelper {

    public static void sendInboxMessage(InboxMessage inboxMessage) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String receiverUserId = inboxMessage.getReceiverUserId();

        DocumentReference inboxMessageRef = db.collection("inbox_messages").document(receiverUserId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    DocumentSnapshot snapshot = transaction.get(inboxMessageRef);

                    if (!snapshot.exists()) {
                        // Create a new document with an empty list of messages
                        Map<String, Object> data = new HashMap<>();
                        data.put("messages", new ArrayList<>());
                        transaction.set(inboxMessageRef, data);
                    }

                    // Update the messages field
                    transaction.update(inboxMessageRef, "messages", FieldValue.arrayUnion(inboxMessage));
                    return null;
                })
                .addOnSuccessListener(aVoid -> {
                    Log.d("SendInboxMessage", "Inbox message sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.d("SendInboxMessage", "Error sending inbox message: " + e.getMessage());
                });

    }
}