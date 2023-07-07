package edu.bluejack22_2.timescape2.model;

import com.google.firebase.Timestamp;

public class ProjectMember {
    private String userId;
    private String displayName;
    private Timestamp date_joined;
    private String role;

    public ProjectMember(){

    }

    public ProjectMember(String userId, String displayName, Timestamp date_joined, String role) {
        this.userId = userId;
        this.date_joined = date_joined;
        this.role = role;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getDate_joined() {
        if(date_joined == null) return Timestamp.now();
        return date_joined;
    }

    public void setDate_joined(Timestamp date_joined) {
        this.date_joined = date_joined;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
