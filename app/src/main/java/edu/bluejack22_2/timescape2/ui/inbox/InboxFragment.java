package edu.bluejack22_2.timescape2.ui.inbox;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import edu.bluejack22_2.timescape2.R;

public class InboxFragment extends Fragment {

    private InboxViewModel inboxViewModel;
    private ViewPager2 viewPager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        inboxViewModel = new ViewModelProvider(this).get(InboxViewModel.class);
        View root = inflater.inflate(R.layout.fragment_inbox, container, false);

        viewPager = root.findViewById(R.id.view_pager);
        viewPager.setAdapter(new InboxPagerAdapter(this.getActivity()));

        // Fetch messages from Firestore
        inboxViewModel.fetchInboxMessages(FirebaseAuth.getInstance().getCurrentUser().getUid());

        inboxViewModel.getAllInboxMessagesLiveData().observe(getViewLifecycleOwner(), messages -> {
            viewPager.getAdapter().notifyDataSetChanged();
        });
        inboxViewModel.getUnreadInboxMessagesLiveData().observe(getViewLifecycleOwner(), messages -> {
            viewPager.getAdapter().notifyDataSetChanged();
        });
        inboxViewModel.getReadInboxMessagesLiveData().observe(getViewLifecycleOwner(), messages -> {
            viewPager.getAdapter().notifyDataSetChanged();
        });

        TabLayout tabLayout = root.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.all);
                    break;
                case 1:
                    tab.setText(R.string.unread);
                    break;
                case 2:
                    tab.setText(R.string.read_2);
                    break;
            }
        }).attach();

        return root;
    }
}

