package com.teo.ttasks.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teo.ttasks.R;
import com.teo.ttasks.model.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.ViewHolder> {

    // Store a member variable for the contacts
    private List<Task> mTasks;
    private SimpleDateFormat simpleDateFormat;

    // Pass in the contact array into the constructor
    public TasksAdapter(List<Task> tasks) {
        mTasks = tasks;
        simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    }

    public static Date parseRFC3339Date(String datestring) {
        Date d;
        try {
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()); //spec for RFC3339
            d = s.parse(datestring);
        } catch (ParseException pe) {
            //try again with optional decimals
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());//spec for RFC3339 (with fractional seconds)
            s.setLenient(true);
            try {
                d = s.parse(datestring);
            } catch (ParseException e) {
                return null;
            }
        }
        return d;
    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public TasksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.layout_item_task, parent, false);

        // Return a new holder instance
        return new ViewHolder(contactView);
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(TasksAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        Task task = mTasks.get(position);

        if (task.getDue() != null) {
            Date dueDate = parseRFC3339Date(task.getDue());
            // Mon
            simpleDateFormat.applyLocalizedPattern("EEE");
            viewHolder.dateDayName.setText(simpleDateFormat.format(dueDate));
            // 1
            simpleDateFormat.applyLocalizedPattern("d");
            viewHolder.dateDayNumber.setText(simpleDateFormat.format(dueDate));
            // 12:00PM
            Date reminder = task.getReminder();
            if (reminder != null) {
                simpleDateFormat.applyLocalizedPattern("hh:mma");
                viewHolder.reminderTime.setText(simpleDateFormat.format(reminder));
            }
        } else
            viewHolder.dateDayName.setText("");

        // Set item views based on the data model
        viewHolder.taskTitle.setText(task.getTitle());

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public void add(Task task) {
        mTasks.add(task);
        notifyItemInserted(mTasks.size() - 1);
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        @Bind(R.id.task_title) TextView taskTitle;
        @Bind(R.id.task_description) TextView taskDescription;
        @Bind(R.id.date_day_number) TextView dateDayNumber;
        @Bind(R.id.date_day_name) TextView dateDayName;
        @Bind(R.id.task_reminder) TextView reminderTime;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);
            ButterKnife.bind(this, itemView);

            // Attach a click listener to the entire row view
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getLayoutPosition(); // gets item position
        }
    }
}
