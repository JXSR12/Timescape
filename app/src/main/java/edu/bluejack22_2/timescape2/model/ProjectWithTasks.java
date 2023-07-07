package edu.bluejack22_2.timescape2.model;

import java.util.List;

public class ProjectWithTasks {
    private Project project;
    private List<Task> tasks;

    public ProjectWithTasks(Project project, List<Task> tasks) {
        this.project = project;
        this.tasks = tasks;
    }

    public Project getProject() {
        return project;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
