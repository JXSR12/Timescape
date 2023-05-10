package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.List;
import java.util.Random;

import edu.bluejack22_2.timescape.ProjectDetailActivity;
import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Project;

public class RecentProjectAdapter extends BaseAdapter {

    private final Context context;
    private final List<Project> recentProjects;

    public RecentProjectAdapter(Context context, List<Project> recentProjects) {
        this.context = context;
        this.recentProjects = recentProjects;
    }

    @Override
    public int getCount() {
        return recentProjects.size();
    }

    @Override
    public Object getItem(int position) {
        return recentProjects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.recent_project_item, parent, false);
        }

        Project project = recentProjects.get(position);

        CardView recentProjectCard = convertView.findViewById(R.id.recentProjectCard);
        TextView recentProjectTitle = convertView.findViewById(R.id.recentProjectTitle);

        recentProjectTitle.setText(project.getTitle());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Project selectedProject = (Project) getItem(position);
                if (selectedProject != null) {
                    openProjectDetailActivity(selectedProject.getUid());
                }
            }
        });

        // Set a deterministic background color for the CardView based on the project ID
        Random random = new Random(project.getUid().hashCode());
        int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        recentProjectCard.setCardBackgroundColor(color);

        return convertView;
    }

    private void openProjectDetailActivity(String projectId) {
        Intent intent = new Intent(context, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        context.startActivity(intent);
    }
}
