package com.teo.sample;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by skyfishjy on 10/31/14.
 * Source: https://gist.github.com/skyfishjy/443b7448f59be978bc59
 */

public class MyListCursorAdapter extends CursorRecyclerViewAdapter<MyListCursorAdapter.ViewHolder>{

    static OnItemClickListener mItemClickListener;
    static MultiSelector mMultiSelector = new MultiSelector();
    static ActionMode aMode;

    public MyListCursorAdapter(Context context,Cursor cursor){
        super(context,cursor);
    }

    public static class ViewHolder extends SwappingHolder implements View.OnClickListener, View.OnLongClickListener{
        // each data item is just a string in this case
        public MainActivity mainActivity;
        public CheckBox mCheckBox;
        public TextView mTaskTitle;
        public TextView mDay;
        public TextView mDayWeek;
        public ViewHolder(View v) {
            super(v, mMultiSelector);
            mCheckBox = (CheckBox) v.findViewById(R.id.checkBox);
            mDay = (TextView) v.findViewById(R.id.textViewDay);
            mDayWeek = (TextView) v.findViewById(R.id.textViewDayWeek);
            mTaskTitle = (TextView) v.findViewById(R.id.taskTitle);
            mainActivity = (MainActivity) v.getContext();
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            v.setLongClickable(true);
        }

        @Override
        public void onClick(View v) {
            if (mMultiSelector.tapSelection(this)) {
                if(mMultiSelector.getSelectedPositions().isEmpty() && aMode != null)
                    aMode.finish();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mMultiSelector.tapSelection(this)) {
                if (mMultiSelector.isSelected(this.getPosition(), this.getItemId()))
                    mMultiSelector.setSelected(this, true);
                else{
                    mMultiSelector.setSelected(this, false);
                    if(mMultiSelector.getSelectedPositions().isEmpty() && aMode != null)
                        aMode.finish();
                }
                return true;
            }
            aMode = mainActivity.startSupportActionMode(mActionMode);
            //mMultiSelector.setSelected(this, true);
            return true;
        }
    }

    private static ActionMode.Callback mActionMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
//            switch (menuItem.getItemId()) {
//                case R.id.menu_item_delete_crime:
//                    // Need to finish the action mode before doing the following,
//                    // not after. No idea why, but it crashes.
//                    actionMode.finish();
//
//                    for (int i = mCrimes.size(); i > 0; i--) {
//                        if (mMultiSelector.isSelected(i, 0)) {
//                            Crime crime = mCrimes.get(i);
//                            CrimeLab.get(getActivity()).deleteCrime(crime);
//                            mRecyclerView.getAdapter().notifyItemRemoved(i);
//                        }
//                    }
//
//                    mMultiSelector.clearSelections();
//                    return true;
//                default:
//                    break;
//            }
            return false;
        }
    };

    public interface OnItemClickListener {
        public void onItemClick(View view , int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener ItemClickListener) {
        mItemClickListener = ItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_task, parent, false);
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
            if (!dueDate.equals("")){
                Date date = format.parse(dueDate);
                viewHolder.mDayWeek.setText(dayWeekFormat.format(date));
                viewHolder.mDay.setText(dayFormat.format(date));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        viewHolder.mTaskTitle.setText(title);

        if(status.contains("completed"))
            viewHolder.mCheckBox.setChecked(true);
    }
}
