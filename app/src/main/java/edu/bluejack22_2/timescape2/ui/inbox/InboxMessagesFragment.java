package edu.bluejack22_2.timescape2.ui.inbox;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import edu.bluejack22_2.timescape2.R;
import edu.bluejack22_2.timescape2.model.InboxMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InboxMessagesFragment extends Fragment {
    private static final String ARG_TAB_POSITION = "tab_position";
    private InboxViewModel inboxViewModel;
    private InboxAdapter inboxAdapter;
    private int tabPosition;

    public InboxMessagesFragment() {
    }

    public static InboxMessagesFragment newInstance(int position) {
        InboxMessagesFragment fragment = new InboxMessagesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabPosition = getArguments().getInt(ARG_TAB_POSITION);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inbox_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inboxViewModel = new ViewModelProvider(requireActivity()).get(InboxViewModel.class);
        RecyclerView recyclerView = view.findViewById(R.id.inbox_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        inboxAdapter = new InboxAdapter(new ArrayList<>(), getContext(), inboxViewModel);
        recyclerView.setAdapter(inboxAdapter);

        TextView emptyView = view.findViewById(R.id.all_inbox_placeholder);

        if (inboxAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        // Fetch inbox messages based on the current tab position
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Replace the following with the appropriate user ID
        String currentUserId = FirebaseAuth.getInstance().getUid();
        db.collection("inbox_messages")
                .document(currentUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        // Handle error
                        return;
                    }

                    List<InboxMessage> messages = new ArrayList<>();
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<Map<String, Object>> messagesData = (List<Map<String, Object>>) documentSnapshot.get("messages");

                        if (messagesData != null) {
                            for (Map<String, Object> messageData : messagesData) {
                                InboxMessage inboxMessage = new InboxMessage();
                                inboxMessage.setReceiverUserId((String) messageData.get("receiverUserId"));
                                inboxMessage.setTitle((String) messageData.get("title"));
                                inboxMessage.setContent((String) messageData.get("content"));
                                inboxMessage.setSentTime((Timestamp) messageData.get("sentTime"));
                                inboxMessage.setRead((Boolean) messageData.get("read"));

                                switch (tabPosition) {
                                    case 0: // All
                                        messages.add(inboxMessage);
                                        break;
                                    case 1: // Unread
                                        if (!inboxMessage.isRead()) {
                                            messages.add(inboxMessage);
                                        }
                                        break;
                                    case 2: // Read
                                        if (inboxMessage.isRead()) {
                                            messages.add(inboxMessage);
                                        }
                                        break;
                                }
                            }

                            // Sort the messages list based on the sentTime field
                            Collections.sort(messages, (m1, m2) -> m2.getSentTime().compareTo(m1.getSentTime()));
                        }
                    }
                    inboxAdapter.submitList(messages);

                    if (inboxAdapter.getItemCount() == 0) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });


    }
}

