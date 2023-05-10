package edu.bluejack22_2.timescape;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import edu.bluejack22_2.timescape.model.User;

public class MemberAdapter extends ArrayAdapter<User> implements Filterable {

    private List<User> originalMembers;
    private List<User> filteredMembers;

    public MemberAdapter(@NonNull Context context, ArrayList<User> members) {
        super(context, 0, members);
        this.originalMembers = members;
        this.filteredMembers = new ArrayList<>(members);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_member, parent, false);
        }

        User member = getItem(position);
        TextView memberName = convertView.findViewById(R.id.member_name);
        memberName.setText(member.getDisplayName());

        return convertView;
    }

    @Override
    public int getCount() {
        return filteredMembers.size();
    }

    @Nullable
    @Override
    public User getItem(int position) {
        return filteredMembers.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String filterString = constraint.toString().toLowerCase().trim();

                List<User> filteredList = new ArrayList<>();
                for (User user : originalMembers) {
                    if (user.getDisplayName().toLowerCase().contains(filterString)) {
                        filteredList.add(user);
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMembers.clear();
                filteredMembers.addAll((List<User>) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
