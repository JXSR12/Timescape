package edu.bluejack22_2.timescape;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.bluejack22_2.timescape.model.Project;
import edu.bluejack22_2.timescape.model.Task;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;


public class ProjectDetailActivity extends BaseActivity {
    private CardView projectInfoCard;
    private CardView projectTasksCard;
    private ConstraintLayout projectDetailLayout;
    private TextView projectTitle;
    private ImageView backButton;
    private TextView projectDesc;
    private TextView projectDates;
    private TextView projectTimeRemaining;
    private TextView projectMembersCount;
    private TextView projectCompletionPercentage;
    private TextView projectTaskCompletedCaption;
    private ProgressBar projectCompletionProgressBar;
    private TextView projectVisibility;
    private ImageView projectVisibilityIcon;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> tasks;
    private Project project;

    private String projectId;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser = mAuth.getCurrentUser();
    private CollectionReference tasksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        tasksRef = db.collection("tasks");

        projectInfoCard = findViewById(R.id.project_info_card);
        projectTasksCard = findViewById(R.id.project_tasks_card);
        projectTitle = findViewById(R.id.project_title);
        backButton = findViewById(R.id.back_button);
        projectInfoCard = findViewById(R.id.project_info_card);
        projectTasksCard = findViewById(R.id.project_tasks_card);
        projectTitle = findViewById(R.id.project_title);
        projectDesc = findViewById(R.id.project_description);
        projectDates = findViewById(R.id.project_dates);
        projectMembersCount = findViewById(R.id.members_count);
        projectTimeRemaining = findViewById(R.id.time_remaining);
        projectVisibility = findViewById(R.id.visibility);
        projectVisibilityIcon = findViewById(R.id.visibility_icon);
        projectCompletionPercentage = findViewById(R.id.completion_percentage);
        projectCompletionProgressBar = findViewById(R.id.completion_progress_bar);
        projectTaskCompletedCaption = findViewById(R.id.tasks_completed_caption);
        backButton = findViewById(R.id.back_button);
        tasksRecyclerView = findViewById(R.id.tasks_recyclerview);
        backButton.setOnClickListener(v -> finish());

        // Retrieve project data from Firestore and populate the views here
        tasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(tasks, new TaskAdapter.OnTaskCheckedChangeListener() {
            @Override
            public void onTaskCheckedChanged(Task task, boolean isChecked) {
                // Update the completed status of the task
                task.setCompleted(isChecked);

                // Update the task in the database
                tasksRef.document(task.getUid())
                        .update("completed", isChecked)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d("ProjectDetailActivity", "Task updated successfully");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("ProjectDetailActivity", "Error updating task", e);
                            }
                        });
            }
        });
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(ProjectDetailActivity.this));
        tasksRecyclerView.setAdapter(taskAdapter);

        // Retrieve project data from Firestore and populate the views here
        projectId = getIntent().getStringExtra("PROJECT_ID");

        db.collection("projects")
                .document(projectId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }

                    if (value != null && value.exists()) {
                        project = value.toObject(Project.class);

                        // Update project details UI
                        updateProjectDetailsUI();

                        updateRecentAccesses(projectId);
                    }
                });

            // Set up real-time listener for tasks updates
            DocumentReference projectRef = db.collection("projects").document(projectId);
                tasksRef
                .whereEqualTo("project", projectRef)
                .orderBy("created_date", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Handle error
                            return;
                        }

                        tasks = value.toObjects(Task.class);
                        taskAdapter.updateTasks(tasks);

                        // Update tasks count and completion status
                        updateTasksCountAndCompletionStatus();
                    }
                });

        ExtendedFloatingActionButton fabMain = findViewById(R.id.fab_main);
        ExtendedFloatingActionButton fabAddTask = findViewById(R.id.fab_add_task);
        ExtendedFloatingActionButton fabProjectChat = findViewById(R.id.fab_project_chat);
        ExtendedFloatingActionButton fabAllMembers = findViewById(R.id.fab_all_members);
        ExtendedFloatingActionButton fabEditProject = findViewById(R.id.fab_edit_project);

        fabAddTask.hide();
        fabProjectChat.hide();
        fabAllMembers.hide();
        fabEditProject.hide();

        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fabAddTask.getVisibility() == View.VISIBLE) {
                    fabAddTask.hide();
                    fabProjectChat.hide();
                    fabAllMembers.hide();
                    if(project.getOwner().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        fabEditProject.hide();
                    }
                } else {
                    fabAddTask.show();
                    fabProjectChat.show();
                    fabAllMembers.show();
                    if(project.getOwner().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        fabEditProject.show();
                    }
                }
            }
        });

        fabProjectChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProjectDetailActivity.this, ProjectChatActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
            }
        });

        fabAddTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog();
            }
        });

        fabAllMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProjectDetailActivity.this, ProjectMembersActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
            }
        });

        fabEditProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProjectDetailActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_edit_project, null);
                builder.setView(dialogView);

                EditText titleEditText = dialogView.findViewById(R.id.title_input);
                EditText descriptionEditText = dialogView.findViewById(R.id.description_input);
                EditText deadlineDateEditText = dialogView.findViewById(R.id.deadline_input);
                SwitchMaterial visibilitySwitch = dialogView.findViewById(R.id.visibility_switch);
                Button saveChangesButton = dialogView.findViewById(R.id.create_project_button);

                titleEditText.setText(project.getTitle());
                descriptionEditText.setText(project.getDescription());
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
                deadlineDateEditText.setText(sdf.format(project.getDeadline_date().toDate()));
                visibilitySwitch.setChecked(project.isPrivate());

                // Set up the DatePicker and TimePicker dialogs
                final Calendar calendar = Calendar.getInstance();
                deadlineDateEditText.setOnClickListener(v1 -> {
                    new DatePickerDialog(ProjectDetailActivity.this, (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        new TimePickerDialog(ProjectDetailActivity.this, (timeView, hourOfDay, minute) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minute);
                            deadlineDateEditText.setText(sdf.format(calendar.getTime()));
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                });

                AlertDialog dialog = builder.create();

                saveChangesButton.setOnClickListener(v1 -> {
                    String newTitle = titleEditText.getText().toString().trim();
                    String newDescription = descriptionEditText.getText().toString().trim();
                    Timestamp newDeadline = new Timestamp(calendar.getTime());
                    String newDeadlineText = deadlineDateEditText.getText().toString().trim();
                    boolean newVisibility = visibilitySwitch.isChecked();

                    if (newTitle.isEmpty() || newDescription.isEmpty() || newDeadlineText.isEmpty()) {
                        Snackbar.make(dialogView, "All fields must be filled", Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Update the project document in Firestore
                    db.collection("projects").document(projectId)
                            .update("title", newTitle,
                                    "description", newDescription,
                                    "deadline_date", newDeadline,
                                    "private", newVisibility)
                            .addOnSuccessListener(aVoid -> {
                                // Close the dialog and show success message
                                dialog.dismiss();
                                Snackbar.make(dialogView, "Project edited successfully!", Snackbar.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                // Show error message
                                Snackbar.make(dialogView, "Error editing project: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                });

                dialog.show();
            }
        });



    }

    private void updateRecentAccesses(String projectId) {
        // Get the current user's document reference
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        // Remove the project id from the array (if it exists) and add it to the beginning of the array
        userRef.update(
                        "project_accesses", FieldValue.arrayRemove(projectId),
                        "project_accesses", FieldValue.arrayUnion(projectId))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("ProjectDetailActivity", "Recent accesses updated successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("ProjectDetailActivity", "Error updating recent accesses", e);
                    }
                });
    }

    private void showAddTaskDialog() {
        // Inflate the layout and create the AlertDialog
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_add_task, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        // Find views
        TextInputLayout titleInputLayout = view.findViewById(R.id.title_input_layout);
        TextInputEditText titleInput = view.findViewById(R.id.title_input);
        TextInputLayout descriptionInputLayout = view.findViewById(R.id.description_input_layout);
        TextInputEditText descriptionInput = view.findViewById(R.id.description_input);
        MaterialButton addTaskButton = view.findViewById(R.id.add_task_button);

        // Set OnClickListener for the Add Task button
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
        // Get the input values
                String title = titleInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                // Validate the inputs
                if (title.isEmpty()) {
                    titleInputLayout.setError(getString(R.string.title_required));
                } else if (description.isEmpty()) {
                    descriptionInputLayout.setError(getString(R.string.description_required));
                } else {
                    // Clear any previous errors
                    titleInputLayout.setError(null);
                    descriptionInputLayout.setError(null);

                    // Create a new task
                    createNewTask(title, description);

                    // Dismiss the dialog
                    dialog.dismiss();
                }
            }
        });

    // Show the dialog
        dialog.show();
    }

    private void createNewTask(String title, String description) {
        // Create a new task object
        final Task newTask = new Task();
        newTask.setTitle(title);
        newTask.setDescription(description);
        newTask.setCompleted(false);
        newTask.setCreated_date(new Timestamp(new Date()));
        newTask.setProject(db.collection("projects").document(projectId));

        // Add the new task to Firestore
        db.collection("tasks").add(newTask)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Set the uid field to the created document ID
                        newTask.setUid(documentReference.getId());

                        // Update the Firestore document with the new uid field
                        documentReference.update("uid", newTask.getUid())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(findViewById(android.R.id.content), R.string.task_added_successfully, Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(findViewById(android.R.id.content), R.string.error_adding_task, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(findViewById(android.R.id.content), R.string.error_adding_task, Snackbar.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateProjectDetailsUI() {
        projectTitle.setText(project.getTitle());
        projectDesc.setText(project.getDescription());

        if(project.isPrivate()){
            projectVisibility.setText(R.string.private_str2);
            projectVisibilityIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.outline_lock_24));
        }else{
            projectVisibility.setText(R.string.public_str);
            projectVisibilityIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.outline_public_24));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy '('HH:mm')'", Locale.getDefault());
        String createdDate = sdf.format(project.getCreated_date().toDate());
        String deadlineDate = sdf.format(project.getDeadline_date().toDate());
        projectDates.setText(String.format("%s - %s", createdDate, deadlineDate));

        updateRemainingTime();

        // Update members count
        if (project.getMembers() != null) {
            int membersCount = project.getMembers().size();
            projectMembersCount.setText(String.format(Locale.getDefault(), "%d members", membersCount + 1));
        } else {
            projectMembersCount.setText("0 members");
        }
    }

    private void updateRemainingTime() {
        long remainingMillis = project.getDeadline_date().toDate().getTime() - new Date().getTime();

        if (remainingMillis <= 0) {
            projectTimeRemaining.setText("Deadline passed");
            return;
        }

        long days = TimeUnit.MILLISECONDS.toDays(remainingMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60;

        if (days > 0) {projectTimeRemaining.setText(String.format(Locale.getDefault(), "%d days left", days));
        } else if (hours > 0) {
            projectTimeRemaining.setText(String.format(Locale.getDefault(), "%d hours left", hours));
        } else if (minutes > 0) {
            projectTimeRemaining.setText(String.format(Locale.getDefault(), "%d minutes left", minutes));
        } else {
            projectTimeRemaining.setText("Under a minute left");
        }
    }

    private void updateTasksCountAndCompletionStatus() {
        int completedTasks = 0;
        int totalTasks = tasks.size();

        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks++;
            }
        }
        int completionPercentage;

        if(totalTasks == 0){
            completionPercentage = 100;
            projectCompletionPercentage.setText("No tasks yet");
        }else{
            completionPercentage = (int) (((float) completedTasks / totalTasks) * 100);
            projectCompletionPercentage.setText(String.format(Locale.getDefault(), "%d%%", completionPercentage));
        }
        projectCompletionProgressBar.setProgress(completionPercentage);

        projectTaskCompletedCaption.setText(String.format(Locale.getDefault(), "%d out of %d completed", completedTasks, totalTasks));
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseFirestore.getInstance().collection("projects").document(getIntent().getStringExtra("PROJECT_ID")).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(!documentSnapshot.exists()){
                    finish();
                }else{
                    DocumentReference ownerRef = documentSnapshot.getDocumentReference("owner");
                    HashMap<String, Map<String, Object>> membersMap = (HashMap<String, Map<String, Object>>) documentSnapshot.get("members");
                    if(!ownerRef.getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) && !membersMap.containsKey(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                        finish();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
