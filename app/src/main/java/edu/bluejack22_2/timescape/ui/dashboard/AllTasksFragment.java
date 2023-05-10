package edu.bluejack22_2.timescape.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import edu.bluejack22_2.timescape.databinding.FragmentAllTasksBinding;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectWithTasks;

public class AllTasksFragment extends Fragment {
    private FragmentAllTasksBinding binding;
    private AllTasksViewModel allTasksViewModel;
    private GroupedTasksAdapter groupedTasksAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        allTasksViewModel = new ViewModelProvider(this).get(AllTasksViewModel.class);

        binding = FragmentAllTasksBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Set up the RecyclerView
        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        groupedTasksAdapter = new GroupedTasksAdapter(requireContext(), new ArrayList<>(), allTasksViewModel);
        binding.tasksRecyclerView.setAdapter(groupedTasksAdapter);

        // Observe allProjects LiveData
        allTasksViewModel.getAllProjectsWithTasks().observe(getViewLifecycleOwner(), projects -> {
            List<Object> items = new ArrayList<>();
            for (ProjectWithTasks project : projects) {
                if (!project.getTasks().isEmpty()) {
                    items.add("Project: " + project.getProject().getTitle());
                    items.addAll(project.getTasks());
                }
            }
            groupedTasksAdapter.updateItems(items);
        });

        // Set up the SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            allTasksViewModel.fetchProjectsAndTasks();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        // Fetch data
        allTasksViewModel.fetchProjectsAndTasks();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


