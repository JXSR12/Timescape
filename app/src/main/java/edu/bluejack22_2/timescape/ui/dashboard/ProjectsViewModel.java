package edu.bluejack22_2.timescape.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.ProjectAccess;

public class ProjectsViewModel extends ViewModel {
    private final MutableLiveData<List<Project>> recentProjects = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> allProjects = new MutableLiveData<>();

    public void fetchData(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        fetchRecentProjects(db, userId);
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
                                allProjects.setValue(projects);

                                // Sort the projects by their last_modified_date field
                                projects.sort((o1, o2) -> o2.getLast_modified_date().compareTo(o1.getLast_modified_date()));

                                allProjects.setValue(projects);
                            });
                });
    }

    public LiveData<List<Project>> getRecentProjects() {
        return recentProjects;
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }
}
