package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.bluejack22_2.timescape.MainActivity;
import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectWithTasks;
import edu.bluejack22_2.timescape.model.Task;

public class AllTasksViewModel extends ViewModel {
    private MutableLiveData<List<ProjectWithTasks>> allProjectsWithTasks;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    public AllTasksViewModel() {
        allProjectsWithTasks = new MutableLiveData<>(new ArrayList<>());
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    public LiveData<List<ProjectWithTasks>> getAllProjectsWithTasks() {
        return allProjectsWithTasks;
    }

    public void fetchProjectsAndTasks() {
        if (currentUser != null) {
            fetchAllProjects(db, currentUser.getUid(), projects -> {
                List<ProjectWithTasks> projectWithTasksList = new ArrayList<>();

                for (Project project : projects) {
                    // Create a ProjectWithTasks object with an empty tasks list for each project
                    ProjectWithTasks projectWithTasks = new ProjectWithTasks(project, new ArrayList<>());
                    projectWithTasksList.add(projectWithTasks);

                    // Real-time listener for tasks updates for each project
                    DocumentReference projectRef = db.collection("projects").document(project.getUid());
                    db.collection("tasks")
                            .whereEqualTo("project", projectRef)
                            .orderBy("created_date", Query.Direction.ASCENDING) // Sort tasks by created_date in ascending order
                            .addSnapshotListener((value, error) -> {
                                if (error != null) {
                                    // Handle error
                                    return;
                                }

                                List<Task> updatedTasks = value.toObjects(Task.class);
                                projectWithTasks.getTasks().clear();
                                projectWithTasks.getTasks().addAll(updatedTasks);

                                allProjectsWithTasks.setValue(projectWithTasksList);
                            });
                }
            });
        }
    }

    private void fetchAllProjects(FirebaseFirestore db, String userId, final OnProjectsLoadedCallback callback) {
        DocumentReference userRef = db.collection("users").document(userId);

        // Combine queries for projects where user is a member and projects where user is the owner
        db.collection("projects")
                .whereLessThanOrEqualTo("members." + userId + ".date_joined", Timestamp.now())
                .get()
                .continueWithTask(task -> {
                    List<Project> projects = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (int i = 0; i < task.getResult().size(); i++) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(i);
                            Project project = document.toObject(Project.class);
                            if (project != null) {
                                project.setUid(document.getId());
                                projects.add(project);
                            }
                        }
                    }
                    return db.collection("projects")
                            .whereEqualTo("owner", userRef)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                                    Project project = queryDocumentSnapshots.getDocuments().get(i).toObject(Project.class);
                                    if (project != null) {
                                        project.setUid(queryDocumentSnapshots.getDocuments().get(i).getId());
                                        projects.add(project);
                                    }
                                }
                                callback.onProjectsLoaded(projects);
                            });
                });
    }


    public void updateTaskStatus(Task task, Context context) {
        if (currentUser != null) {
            String projectId = task.getProject().getId();
            db.collection("projects").document(projectId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if ((documentSnapshot.exists() && documentSnapshot.get("members." + currentUser.getUid()) != null) || documentSnapshot.getDocumentReference("owner").getId().equals(currentUser.getUid())) {
                        DocumentReference taskRef = db.collection("tasks").document(task.getUid());
                        taskRef.update("completed", task.isCompleted())
                                .addOnSuccessListener(aVoid -> Log.d("AllTasksViewModel", "Task status updated successfully"))
                                .addOnFailureListener(e -> Log.e("AllTasksViewModel", "Failed to update task status", e));
                    } else {
                        // Show the alert dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(context); // Replace 'context' with actual context
                        builder.setTitle(R.string.no_access)
                                .setMessage(R.string.it_seems_that_you_are_not_allowed_to_perform_this_action_please_refresh_or_check_if_you_are_still_part_of_the_project)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            });
        }
    }



    private ProjectWithTasks findProjectWithTasksByTaskId(String taskId) {
        for (ProjectWithTasks projectWithTasks : allProjectsWithTasks.getValue()) {
            for (Task task : projectWithTasks.getTasks()) {
                if (task.getUid().equals(taskId)) {
                    return projectWithTasks;
                }
            }
        }
        return null;
    }


    interface OnProjectsLoadedCallback {
        void onProjectsLoaded(List<Project> projects);
    }
}
