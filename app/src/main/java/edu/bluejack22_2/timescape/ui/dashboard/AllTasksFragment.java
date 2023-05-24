package edu.bluejack22_2.timescape.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

import edu.bluejack22_2.timescape.R;
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
        TextView emptyView = binding.allTasksPlaceholder;
        if (groupedTasksAdapter.getItemCount() == 0) {
            emptyView.setVisibility(View.VISIBLE);
            binding.tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            binding.tasksRecyclerView.setVisibility(View.VISIBLE);
        }

        allTasksViewModel.getAllProjectsWithTasks().observe(getViewLifecycleOwner(), projects -> {
            List<Object> items = new ArrayList<>();
            for (ProjectWithTasks project : projects) {
                if (!project.getTasks().isEmpty()) {
                    items.add(getString(R.string.project) + project.getProject().getTitle());
                    items.addAll(project.getTasks());
                }
            }
            groupedTasksAdapter.updateItems(items);

            if (groupedTasksAdapter.getItemCount() == 0) {
                emptyView.setVisibility(View.VISIBLE);
                binding.tasksRecyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                binding.tasksRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Set up the SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            String selectedFilter = binding.filterSpinner.getSelectedItem().toString();
            if (selectedFilter.equals("Nearest Upcoming Deadline")) {
                allTasksViewModel.fetchAndSortProjectsAndTasks(true);
            } else if (selectedFilter.equals("Passed Deadline")) {
                allTasksViewModel.fetchAndSortProjectsAndTasks(false);
            } else {
                allTasksViewModel.fetchProjectsAndTasks();
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        });


        // Fetch data
        allTasksViewModel.fetchProjectsAndTasks();

        // Set up Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.filter_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.filterSpinner.setAdapter(adapter);

        binding.filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:  // Default
                        allTasksViewModel.fetchProjectsAndTasks();
                        break;
                    case 1:  // Nearest Upcoming Deadline
                        allTasksViewModel.fetchAndSortProjectsAndTasks(true);
                        break;
                    case 2:  // Passed Deadline
                        allTasksViewModel.fetchAndSortProjectsAndTasks(false);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


