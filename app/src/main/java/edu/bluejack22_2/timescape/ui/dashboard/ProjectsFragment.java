package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.bluejack22_2.timescape.ProjectDetailActivity;
import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.databinding.FragmentProjectsBinding;
import edu.bluejack22_2.timescape.model.Project;

public class ProjectsFragment extends Fragment {

    private FragmentProjectsBinding binding;
    private ProjectsViewModel projectsViewModel;
    private FirebaseAuth firebaseAuth;

    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        projectsViewModel = new ViewModelProvider(this).get(ProjectsViewModel.class);

        dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
        dashboardViewModel.getDialogDismissed().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isDismissed) {
                if (isDismissed) {
                    // Call fetchData() when the dialog is dismissed
                    onResume();
                    // Reset the value of dialogDismissed to false
                    dashboardViewModel.setDialogDismissed(false);
                }
            }
        });

        binding = FragmentProjectsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Set up the SwipeRefreshLayout
            projectsViewModel.setContext(getContext());

            SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
            swipeRefreshLayout.setOnRefreshListener(() -> {
                projectsViewModel.fetchData(currentUser.getUid(), false);
                swipeRefreshLayout.setRefreshing(false);
            });

            projectsViewModel.getRecentProjects().observe(getViewLifecycleOwner(), projects -> {
                GridView recentProjectsGridView = binding.recentProjectsGridView;
                RecentProjectAdapter recentProjectAdapter = new RecentProjectAdapter(requireContext(), projects);
                recentProjectsGridView.setAdapter(recentProjectAdapter);

                recentProjectsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Project selectedProject = projects.get(position);
                        Log.d("DEBUG", "Click grid view item on position " + position);
                        openProjectDetailActivity(selectedProject.getUid());
                    }
                });
            });

            ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.project_completed_filter_options, android.R.layout.simple_spinner_item);
            filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.filterCompletionSpinner.setAdapter(filterAdapter);

            ArrayAdapter<CharSequence> filterAdapter2 = ArrayAdapter.createFromResource(requireContext(), R.array.project_deadline_filter_options, android.R.layout.simple_spinner_item);
            filterAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.filterDeadlineSpinner.setAdapter(filterAdapter2);

            projectsViewModel.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
                ListView listView = binding.allProjectsListView;
                ProjectListAdapter adapter = new ProjectListAdapter(getContext(), projects);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Project selectedProject = projects.get(position);
                        openProjectDetailActivity(selectedProject.getUid());
                    }
                });
            });

            projectsViewModel.fetchData(currentUser.getUid(), false);
        }

        ListView allProjectsListView = binding.allProjectsListView;
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;

        allProjectsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                View firstChild = view.getChildAt(0);
                boolean shouldEnableSwipeToRefresh = firstVisibleItem == 0 && (firstChild == null || firstChild.getTop() == 0);
                swipeRefreshLayout.setEnabled(shouldEnableSwipeToRefresh);
            }
        });

        Spinner filterDeadlineSpinner = binding.filterDeadlineSpinner;
        filterDeadlineSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                projectsViewModel.setDeadlineFilter((String) parent.getItemAtPosition(position));
                projectsViewModel.fetchData(firebaseAuth.getCurrentUser().getUid(), true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Spinner filterCompletionSpinner = binding.filterCompletionSpinner;
        filterCompletionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                projectsViewModel.setCompletionFilter((String) parent.getItemAtPosition(position));
                projectsViewModel.fetchData(firebaseAuth.getCurrentUser().getUid(), true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });



        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the projects data
        if (firebaseAuth.getCurrentUser() != null) {
            projectsViewModel.fetchData(firebaseAuth.getCurrentUser().getUid(), false);
        }
    }


    private void openProjectDetailActivity(String projectId) {
        Intent intent = new Intent(getActivity(), ProjectDetailActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        startActivity(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            projectsViewModel.fetchData(currentUser.getUid(), false);
        }
    }

}
