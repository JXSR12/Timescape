package edu.bluejack22_2.timescape.ui.inbox;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import edu.bluejack22_2.timescape.ui.inbox.InboxMessagesFragment;

public class InboxPagerAdapter extends FragmentStateAdapter {

    public InboxPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return InboxMessagesFragment.newInstance(position);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

