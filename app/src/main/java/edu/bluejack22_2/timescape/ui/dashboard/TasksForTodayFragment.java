package edu.bluejack22_2.timescape.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import edu.bluejack22_2.timescape.databinding.FragmentTasksForTodayBinding;

public class TasksForTodayFragment extends Fragment {

    private FragmentTasksForTodayBinding binding;
    private TasksForTodayViewModel tasksForTodayViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        tasksForTodayViewModel = new ViewModelProvider(this).get(TasksForTodayViewModel.class);

        binding = FragmentTasksForTodayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up your views and observe data from tasksForTodayViewModel

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
