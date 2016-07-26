package com.teo.ttasks.data;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TaskList;

public class TaskListsAdapter extends ArrayAdapter<TaskList> {

    private static int layoutResId = R.layout.spinner_item_task_list;
    private static int layoutResDropDownId = R.layout.spinner_item_task_list_dropdown;

    public TaskListsAdapter(Context context) {
        super(context, layoutResId);
    }

    @Override
    public void setDropDownViewResource(@LayoutRes int resource) {
        layoutResDropDownId = resource;
        super.setDropDownViewResource(resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        TaskList taskList = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layoutResId, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.taskListTitle);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // Populate the data into the template view using the data object
        viewHolder.name.setText(taskList.getTitle());
        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TaskList taskList = getItem(position);
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layoutResDropDownId, parent, false);
            viewHolder.name = (TextView) convertView.findViewById(R.id.taskListTitle);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.name.setText(taskList.getTitle());
        return convertView;
    }

    // View lookup cache
    private static class ViewHolder {
        TextView name;
    }
}
