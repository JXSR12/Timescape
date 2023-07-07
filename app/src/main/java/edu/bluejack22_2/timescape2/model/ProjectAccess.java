package edu.bluejack22_2.timescape2.model;

import com.google.firebase.Timestamp;

public class ProjectAccess {
    private String projectId;
    private Timestamp lastAccessTimestamp;

    public ProjectAccess(){

    }

    public ProjectAccess(String projectId, Timestamp lastAccessTimestamp) {
        this.projectId = projectId;
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public String getProjectId() {
        return projectId;
    }

    public Timestamp getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }
}

