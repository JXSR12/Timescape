package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Task;

public class GroupedTasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private static AllTasksViewModel allTasksViewModel;
    private final Context context;
    private final List<Object> items;

    public GroupedTasksAdapter(Context context, List<Object> items, AllTasksViewModel atvm) {
        this.context = context;
        this.items = items;
        allTasksViewModel = atvm;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.project_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((ItemViewHolder) holder).bind((Task) items.get(position));
        }
    }

    public void updateItems(List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        TextView projectTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            projectTitle = itemView.findViewById(R.id.project_title);
        }

        void bind(String title) {
            projectTitle.setText(title);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        // Add your views for the task item
        TextView taskTitle;
        TextView taskDescription;
        CheckBox taskCompleted;
        ImageButton editTask;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize your views for the task item
            taskTitle = itemView.findViewById(R.id.task_title);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskCompleted = itemView.findViewById(R.id.task_checkbox);
            editTask = itemView.findViewById(R.id.edit_task);
        }

        void bind(Task task) {
            // Bind the task data to the views
            taskTitle.setText(task.getTitle());
            taskDescription.setText(task.getDescription());

            // Remove the listener before setting the CheckBox state to prevent it from triggering
            taskCompleted.setOnCheckedChangeListener(null);
            taskCompleted.setChecked(task.isCompleted());

            // Add the listener back after setting the CheckBox state
            taskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                allTasksViewModel.updateTaskStatus(task, itemView.getContext());
            });

            editTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(v.getContext(), editTask);
                    popup.getMenuInflater().inflate(R.menu.task_options_menu, popup.getMenu());

                    popup.setOnMenuItemClickListener(item -> {
                        if (item.getItemId() == R.id.action_edit) {
                            openEditTaskDialog(task, itemView);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            deleteTask(task, itemView, (ViewGroup) itemView.getParent());
                            return true;
                        } else {
                            return false;
                        }
                    });

                    popup.show();
                }
            });
        }

        private void openEditTaskDialog(Task task, View itemView) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            View view = inflater.inflate(R.layout.dialog_edit_task, null);

            TextInputLayout titleInputLayout = view.findViewById(R.id.title_input_layout);
            TextInputEditText titleInput = view.findViewById(R.id.title_input);
            TextInputLayout descriptionInputLayout = view.findViewById(R.id.description_input_layout);
            TextInputEditText descriptionInput = view.findViewById(R.id.description_input);
            MaterialButton saveChangesButton = view.findViewById(R.id.add_task_button);

            // Set initial values
            titleInput.setText(task.getTitle());
            descriptionInput.setText(task.getDescription());

            builder.setView(view);
            AlertDialog dialog = builder.create();

            saveChangesButton.setOnClickListener(v -> {
                String title = titleInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                updateTask(title, description, itemView, task);
                dialog.dismiss();
            });

            dialog.show();
        }

        private void updateTask(String title, String description, View itemView, Task task) {
            String projectId = task.getProject().getId();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("projects").document(projectId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if ((documentSnapshot.exists() && documentSnapshot.get("members." + FirebaseAuth.getInstance().getCurrentUser().getUid()) != null) || documentSnapshot.getDocumentReference("owner").getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        task.setTitle(title);
                        task.setDescription(description);

                        db.collection("tasks").document(task.getUid())
                                .set(task)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(itemView, R.string.task_updated, Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(itemView, R.string.error_while_updating_task, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                        builder.setTitle(R.string.no_access)
                                .setMessage(R.string.it_seems_that_you_are_not_allowed_to_perform_this_action_please_refresh_or_check_if_you_are_still_part_of_the_project)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            });
        }

        private void deleteTask(Task task, View itemView, ViewGroup parentView) {
            String projectId = task.getProject().getId();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("projects").document(projectId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if ((documentSnapshot.exists() && documentSnapshot.get("members." + FirebaseAuth.getInstance().getCurrentUser().getUid()) != null) || documentSnapshot.getDocumentReference("owner").getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                        builder.setTitle(R.string.delete_this_task)
                                .setMessage(R.string.this_action_cannot_be_undone)
                                .setPositiveButton(R.string.delete_b, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        db.collection("tasks").document(task.getUid())
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Snackbar.make(itemView, R.string.task_deleted, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Snackbar.make(itemView, R.string.error_while_deleting_task, Snackbar.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.create().show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
                        builder.setTitle(R.string.no_access)
                                .setMessage(R.string.it_seems_that_you_are_not_allowed_to_perform_this_action_please_refresh_or_check_if_you_are_still_part_of_the_project)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                }
            });
        }



    }

}
