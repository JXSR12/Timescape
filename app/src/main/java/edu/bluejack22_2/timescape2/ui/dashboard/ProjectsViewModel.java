package edu.bluejack22_2.timescape2.ui.dashboard;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.bluejack22_2.timescape2.R;
import edu.bluejack22_2.timescape2.model.Project;
import edu.bluejack22_2.timescape2.model.ProjectAccess;

public class ProjectsViewModel extends ViewModel {
    private final MutableLiveData<List<Project>> recentProjects = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();

    private String deadlineFilter = "All Deadline Status";
    private String completionFilter = "All Completion Status";

    private Context context = null;

    public void setContext(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void fetchData(String userId, boolean allProjectsOnly) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if(!allProjectsOnly){
            fetchRecentProjects(db, userId);
        }
        fetchAllProjects(db, userId);
    }


    private void fetchRecentProjects(FirebaseFirestore db, String userId) {
        CollectionReference projectAccessesRef = db.collection("users").document(userId)
                .collection("projectAccesses");

        // Fetch all the documents, ordered by lastAccessTimestamp descending
        projectAccessesRef.orderBy("lastAccessTimestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ProjectAccess> projectAccesses = querySnapshot.toObjects(ProjectAccess.class);

                    List<String> recentProjectIds = projectAccesses.stream()
                            .limit(4)
                            .map(ProjectAccess::getProjectId)
                            .collect(Collectors.toList());

                    List<Project> recentProjectsList = new ArrayList<>();
                    AtomicInteger counter = new AtomicInteger();

                    // Iterate through the project access IDs and fetch the projects in the order they appear in the projectAccesses list
                    for (String projectId : recentProjectIds) {
                        db.collection("projects").document(projectId)
                                .get()
                                .addOnSuccessListener(projectSnapshot -> {
                                    Project project = projectSnapshot.toObject(Project.class);
                                    if (project != null) {
                                        project.setUid(projectSnapshot.getId());
                                        recentProjectsList.add(project);
                                    }
                                    if (counter.incrementAndGet() == recentProjectIds.size()) {
                                        recentProjects.setValue(recentProjectsList);
                                    }
                                });
                    }
                });
    }

    private void fetchAllProjects(FirebaseFirestore db, String userId) {
        DocumentReference userRef = db.collection("users").document(userId);

        // Combine queries for projects where user is a member and projects where user is the owner
        List<Project> projects = new ArrayList<>();
        db.collection("projects")
                .whereNotEqualTo("members." + userId + ".role", "not_member")
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        List<Task<Void>> tasks = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            Project project = document.toObject(Project.class);
                            if (project != null) {
                                project.setUid(document.getId());
                                // Filter based on the selected deadline filter
                                if (matchesDeadlineFilter(project, deadlineFilter)) {
                                    tasks.add(matchesCompletionFilter(db, project, completionFilter)
                                            .continueWith(task1 -> {
                                                if (task1.isSuccessful() && task1.getResult()) {
                                                    projects.add(project);
                                                }
                                                return null;
                                            }));
                                }
                            }
                        }
                        return Tasks.whenAll(tasks);
                    } else {
                        // handle error
                        return null;
                    }
                }).continueWithTask(task -> db.collection("projects")
                        .whereEqualTo("owner", userRef)
                        .get()
                        .continueWithTask(task1 -> {
                            if (task1.isSuccessful()) {
                                List<Task<Void>> tasks = new ArrayList<>();
                                for (DocumentSnapshot document : task1.getResult().getDocuments()) {
                                    Project project = document.toObject(Project.class);
                                    if (project != null) {
                                        project.setUid(document.getId());
                                        if (matchesDeadlineFilter(project, deadlineFilter)) {
                                            tasks.add(matchesCompletionFilter(db, project, completionFilter)
                                                    .continueWith(task2 -> {
                                                        if (task2.isSuccessful() && task2.getResult()) {
                                                            projects.add(project);
                                                        }
                                                        return null;
                                                    }));
                                        }
                                    }
                                }
                                return Tasks.whenAll(tasks);
                            } else {
                                // handle error
                                return null;
                            }
                        })).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sort the projects by their last_modified_date field
                        projects.sort((o1, o2) -> o2.getLast_modified_date().compareTo(o1.getLast_modified_date()));
                        allProjects.setValue(projects);
                    } else {
                        // handle error
                    }
                });
    }



    private boolean matchesDeadlineFilter(Project project, String deadlineFilter) {
        String[] deadlineFilterOptions = context.getResources().getStringArray(R.array.project_deadline_filter_options);
        Date currentTime = new Date();
        Date deadlineDate = project.getDeadline_date().toDate();
        if (deadlineFilter.equals(deadlineFilterOptions[1])) {
            return deadlineDate.after(currentTime);
        } else if (deadlineFilter.equals(deadlineFilterOptions[2])) {
            return deadlineDate.before(currentTime);
        } else {
            return true;
        }
    }

    private Task<Boolean> matchesCompletionFilter(FirebaseFirestore db, Project project, String completionFilter) {
        String[] completionFilterOptions = context.getResources().getStringArray(R.array.project_completed_filter_options);
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        if(completionFilter.equals(completionFilterOptions[1])) {
            db.collection("tasks").whereEqualTo("project", db.document("projects/" + project.getUid())).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        edu.bluejack22_2.timescape2.model.Task taskObj = document.toObject(edu.bluejack22_2.timescape2.model.Task.class);
                        if (!taskObj.isCompleted()) {
                            taskCompletionSource.setResult(false);
                            return;
                        }
                    }
                    taskCompletionSource.setResult(true);
                } else {
                    taskCompletionSource.setResult(false);
                }
            });
        }else if(completionFilter.equals(completionFilterOptions[2])) {
            db.collection("tasks").whereEqualTo("project", db.document("projects/" + project.getUid())).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        edu.bluejack22_2.timescape2.model.Task taskObj = document.toObject(edu.bluejack22_2.timescape2.model.Task.class);
                        if (taskObj.isCompleted()) {
                            taskCompletionSource.setResult(false);
                            return;
                        }
                    }
                    taskCompletionSource.setResult(true);
                } else {
                    taskCompletionSource.setResult(false);
                }
            });
        }else{
                taskCompletionSource.setResult(true);
        }
        return taskCompletionSource.getTask();
    }


    public LiveData<List<Project>> getRecentProjects() {
        return recentProjects;
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public void setDeadlineFilter(String deadlineFilter) {
        this.deadlineFilter = deadlineFilter;
    }

    public void setCompletionFilter(String completionFilter) {
        this.completionFilter = completionFilter;
    }
}
