package edu.bluejack22_2.timescape.ui.dashboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import edu.bluejack22_2.timescape.R;
import edu.bluejack22_2.timescape.model.Project;
import com.amulyakhare.textdrawable.TextDrawable;
import com.google.firebase.Timestamp;


public class ProjectListAdapter extends BaseAdapter {

    private final Context context;
    private final List<Project> projects;

    public ProjectListAdapter(Context context, List<Project> projects) {
        this.context = context;
        this.projects = projects;
    }

    @Override
    public int getCount() {
        return projects.size();
    }

    @Override
    public Object getItem(int position) {
        return projects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.project_list_item, parent, false);
        }

        Project project = projects.get(position);

        ImageView projectAvatar = convertView.findViewById(R.id.projectAvatar);
        TextView projectName = convertView.findViewById(R.id.projectName);
        TextView projectLastModifiedDate = convertView.findViewById(R.id.projectLastModifiedDate);

        // Set avatar with the first letter of the project title
        projectAvatar.setImageDrawable(generateAvatar(project.getUid(), project.getTitle()));

        projectName.setText(project.getTitle());
        projectLastModifiedDate.setText(String.format("%s %s", context.getString(R.string.last_edit), getTimeAgo(project.getLast_modified_date())));
        return convertView;
    }
    private String getTimeAgo(Timestamp timestamp) {
        long timeInMillis = timestamp.toDate().getTime();
        long now = System.currentTimeMillis();
        long diff = now - timeInMillis;
        Resources res = context.getResources();

        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return res.getString(R.string.time_less_than_minute);
        } else if (diff < DateUtils.HOUR_IN_MILLIS) {
            return res.getString(R.string.time_minutes_ago, diff / DateUtils.MINUTE_IN_MILLIS);
        } else if (diff < DateUtils.DAY_IN_MILLIS) {
            return res.getString(R.string.time_hours_ago, diff / DateUtils.HOUR_IN_MILLIS);
        } else if (diff < DateUtils.WEEK_IN_MILLIS) {
            return res.getString(R.string.time_days_ago, diff / DateUtils.DAY_IN_MILLIS);
        } else if (diff < DateUtils.WEEK_IN_MILLIS * 4) {
            return res.getString(R.string.time_weeks_ago, diff / DateUtils.WEEK_IN_MILLIS);
        } else if (diff < DateUtils.YEAR_IN_MILLIS) {
            return res.getString(R.string.time_months_ago, diff / (DateUtils.WEEK_IN_MILLIS * 4));
        } else if (diff < DateUtils.YEAR_IN_MILLIS * 10) {
            return res.getString(R.string.time_years_ago, diff / DateUtils.YEAR_IN_MILLIS);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        }
    }



    private TextDrawable generateAvatar(String projectId, String title) {
        String firstLetter = String.valueOf(title.charAt(0)).toUpperCase();
        Random random = new Random(projectId.hashCode());
        int color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        return TextDrawable.builder().buildRound(firstLetter, color);
    }

}
