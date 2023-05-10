package edu.bluejack22_2.timescape.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DashboardTabsAdapter extends FragmentStateAdapter {

    public DashboardTabsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ProjectsFragment();
            case 1:
                return new AllTasksFragment();
            case 2:
                return new TasksForTodayFragment();
            default:
                return new ProjectsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Number of tabs
    }
}
