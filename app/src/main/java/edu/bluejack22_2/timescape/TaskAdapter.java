package edu.bluejack22_2.timescape;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import edu.bluejack22_2.timescape.model.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private List<Task> tasks;
    private OnTaskCheckedChangeListener onTaskCheckedChangeListener;

    public TaskAdapter(List<Task> tasks, OnTaskCheckedChangeListener onTaskCheckedChangeListener) {
        this.tasks = tasks;
        this.onTaskCheckedChangeListener = onTaskCheckedChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.taskTitle.setText(task.getTitle());
        holder.taskDescription.setText(task.getDescription());

        holder.taskCheckBox.setOnCheckedChangeListener(null);
        holder.taskCheckBox.setChecked(task.isCompleted());
        holder.taskCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (onTaskCheckedChangeListener != null) {
                onTaskCheckedChangeListener.onTaskCheckedChanged(task, isChecked);
            }
        });

        holder.editTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(v.getContext(), holder.editTask);
                popup.getMenuInflater().inflate(R.menu.task_options_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_edit) {
                        openEditTaskDialog(task, holder.itemView);
                        return true;
                    } else if (item.getItemId() == R.id.action_delete) {
                        deleteTask(task, holder.itemView, (ViewGroup) holder.itemView.getParent());
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
        task.setTitle(title);
        task.setDescription(description);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks").document(task.getUid())
                .set(task)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(itemView, "Task updated", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(itemView, "Error while updating task", Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteTask(Task task, View itemView, ViewGroup parentView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
        builder.setTitle("Delete this task?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("tasks").document(task.getUid())
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(parentView, "Task deleted", Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(parentView, "Error while deleting task", Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.dismiss();
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }


    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView taskTitle;
        public TextView taskDescription;
        public CheckBox taskCheckBox;
        public ImageButton editTask;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskCheckBox = itemView.findViewById(R.id.task_checkbox);
            editTask = itemView.findViewById(R.id.edit_task);
        }
    }

    public interface OnTaskCheckedChangeListener {
        void onTaskCheckedChanged(Task task, boolean isChecked);
    }
}