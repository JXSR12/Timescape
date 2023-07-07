package edu.bluejack22_2.timescape2.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class Task {
    private String uid;
    private String title;
    private String description;
    private Timestamp created_date;
    private DocumentReference project;
    private boolean isCompleted;

    public Task() {
    }

    public Task(String uid, String title, String description, Timestamp created_date, DocumentReference project, boolean isCompleted) {
        this.uid = uid;
        this.title = title;
        this.description = description;
        this.created_date = created_date;
        this.project = project;
        this.isCompleted = isCompleted;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreated_date() {
        return created_date;
    }

    public void setCreated_date(Timestamp created_date) {
        this.created_date = created_date;
    }

    public DocumentReference getProject() {
        return project;
    }

    public void setProject(DocumentReference project) {
        this.project = project;
    }
}
