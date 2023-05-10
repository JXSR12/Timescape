package edu.bluejack22_2.timescape.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

public class Project {

    private String uid;
    private String title;
    private String description;
    private Timestamp created_date;
    private Timestamp last_modified_date;
    private Timestamp deadline_date;
    private Map<String, Map<String, Object>> members;
    private DocumentReference owner;
    private boolean isPrivate;

    public Project() {
    }

    public Project(String title, Timestamp created_date, Timestamp last_modified_date,
                   Timestamp deadline_date, Map<String, Map<String, Object>> members,
                   DocumentReference owner, boolean isPrivate, String description) {
        this.title = title;
        this.created_date = created_date;
        this.last_modified_date = last_modified_date;
        this.deadline_date = deadline_date;
        this.members = members;
        this.owner = owner;
        this.isPrivate = isPrivate;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Timestamp getCreated_date() {
        return created_date;
    }

    public void setCreated_date(Timestamp created_date) {
        this.created_date = created_date;
    }

    public Timestamp getLast_modified_date() {
        return last_modified_date;
    }

    public void setLast_modified_date(Timestamp last_modified_date) {
        this.last_modified_date = last_modified_date;
    }

    public Timestamp getDeadline_date() {
        return deadline_date;
    }

    public void setDeadline_date(Timestamp deadline_date) {
        this.deadline_date = deadline_date;
    }

    public Map<String, Map<String, Object>> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Map<String, Object>> members) {
        this.members = members;
    }

    public DocumentReference getOwner() {
        return owner;
    }

    public void setOwner(DocumentReference owner) {
        this.owner = owner;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }
}
