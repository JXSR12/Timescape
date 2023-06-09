package edu.bluejack22_2.timescape2.ui.chats;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import edu.bluejack22_2.timescape2.ProjectChatActivity;
import edu.bluejack22_2.timescape2.databinding.FragmentChatsBinding;
import edu.bluejack22_2.timescape2.model.Chat;

public class ChatsFragment extends Fragment implements ChatListAdapter.OnChatItemClickListener{

    private FragmentChatsBinding binding;
    private RecyclerView chatsRecyclerView;
    private ChatListAdapter chatListAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChatsViewModel chatsViewModel =
                new ViewModelProvider(this).get(ChatsViewModel.class);

        binding = FragmentChatsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        chatsRecyclerView = binding.chatsRecyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        chatsRecyclerView.setLayoutManager(layoutManager);


        List<Chat> chatItemList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getContext(), chatItemList, this);
        chatsRecyclerView.setAdapter(chatListAdapter);

        TextView emptyView = binding.allChatsPlaceholder;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatsViewModel.getChatItems(userId, chatListAdapter).observe(getViewLifecycleOwner(), chatItems -> {
            chatListAdapter.setChatItems(chatItems);
            chatListAdapter.notifyDataSetChanged();

            if (chatListAdapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                chatsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                chatsRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        if (chatListAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            chatsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            chatsRecyclerView.setVisibility(View.VISIBLE);
        }

        return root;
    }

    @Override
    public void onChatItemClick(String projectId) {
        Intent intent = new Intent(getContext(), ProjectChatActivity.class);
        intent.putExtra("projectId", projectId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        chatListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
