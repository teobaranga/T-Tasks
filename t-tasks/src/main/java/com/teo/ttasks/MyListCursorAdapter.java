package com.teo.ttasks;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.teo.ttasks.activities.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by skyfishjy on 10/31/14.
 * Source: https://gist.github.com/skyfishjy/443b7448f59be978bc59
 */

public class MyListCursorAdapter extends CursorRecyclerViewAdapter<MyListCursorAdapter.ViewHolder> {

    public MyListCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_task, parent, false);
        // set the view's size, margins, paddings and layout parameters
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor) {
        viewHolder.mCheckBox.setChecked(false);
        viewHolder.mDayWeek.setText("");
        viewHolder.mDay.setText("");
        viewHolder.mTaskTitle.setText("");

        String title = cursor.getString(cursor.getColumnIndex("title"));
        String status = cursor.getString(cursor.getColumnIndex("status"));
        String dueDate = cursor.getString(cursor.getColumnIndex("due"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.CANADA);
        SimpleDateFormat dayWeekFormat = new SimpleDateFormat("E", Locale.CANADA);
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.CANADA);
        try {
            if (!dueDate.equals("")) {
                Date date = format.parse(dueDate);
                viewHolder.mDayWeek.setText(dayWeekFormat.format(date));
                viewHolder.mDay.setText(dayFormat.format(date));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        viewHolder.mTaskTitle.setText(title);

        if (status.contains("completed"))
            viewHolder.mCheckBox.setChecked(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        // each data item is just a string in this case
        public MainActivity mainActivity;
        public CheckBox mCheckBox;
        public TextView mTaskTitle;
        public TextView mDay;
        public TextView mDayWeek;

        public ViewHolder(View v) {
            super(v);
            mCheckBox = (CheckBox) v.findViewById(R.id.checkBox);
            mDay = (TextView) v.findViewById(R.id.date_day_number);
            mDayWeek = (TextView) v.findViewById(R.id.date_day_name);
            mTaskTitle = (TextView) v.findViewById(R.id.title_task);
            mainActivity = (MainActivity) v.getContext();
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            v.setLongClickable(true);
        }

        @Override
        public void onClick(View v) {

        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }
}
