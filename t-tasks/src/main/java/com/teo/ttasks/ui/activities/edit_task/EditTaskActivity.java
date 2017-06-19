package com.teo.ttasks.ui.activities.edit_task;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.mikepenz.materialdrawer.util.KeyboardUtil;
import com.teo.ttasks.R;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.databinding.ActivityEditTaskBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.util.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

public class EditTaskActivity extends DaggerAppCompatActivity implements EditTaskView {

    private static final String EXTRA_TASK_ID = "taskId";
    private static final String EXTRA_TASK_LIST_ID = "taskListId";

    @Inject EditTaskPresenter editTaskPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    private DatePickerFragment datePickerFragment;
    private TimePickerFragment timePickerFragment;

    private ActivityEditTaskBinding editTaskBinding;

    private TaskListsAdapter taskListsAdapter;

    private String taskId;
    private String taskListId;

    /**
     * Flag indicating that the reminder time has been clicked.
     * Used to differentiate between the reminder time and the due time.
     */
    private boolean reminderTimeClicked;

    public static void startEdit(Context context, String taskId, String taskListId, Bundle bundle) {
        Intent starter = new Intent(context, EditTaskActivity.class);
        starter.putExtra(EXTRA_TASK_ID, taskId);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        context.startActivity(starter, bundle);
    }

    public static void startCreate(Fragment fragment, String taskListId, Bundle bundle) {
        Intent starter = new Intent(fragment.getContext(), EditTaskActivity.class);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        fragment.startActivity(starter, bundle);
    }

    /**
     * Used when starting this activity from the widget
     */
    public static Intent getTaskCreateIntent(Context context, String taskListId) {
        Intent starter = new Intent(context, EditTaskActivity.class);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editTaskBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_task);
        editTaskBinding.setView(this);
        editTaskPresenter.bindView(this);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskListId = getIntent().getStringExtra(EXTRA_TASK_LIST_ID);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        taskListsAdapter = new TaskListsAdapter(this);
        taskListsAdapter.setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown);
        editTaskBinding.taskLists.setAdapter(taskListsAdapter);

        // Handle a new task or an existing task
        if (taskId == null) {
            // Update the toolbar title
            getSupportActionBar().setTitle(R.string.title_activity_new_task);

            // Show the keyboard
            editTaskBinding.taskTitle.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editTaskBinding.taskTitle, InputMethodManager.SHOW_IMPLICIT);
        } else {
            editTaskPresenter.loadTaskInfo(taskId);
        }

        // Load the available task lists
        editTaskPresenter.loadTaskLists(taskListId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editTaskPresenter.unbindView(this);
    }

    @Override
    public void onTaskLoaded(TTask task) {
        editTaskBinding.setTask(task);
    }

    @Override
    public void onTaskListsLoaded(List<TTaskList> taskLists, int selectedPosition) {
        taskListsAdapter.addAll(taskLists);
        editTaskBinding.taskLists.setSelection(selectedPosition);
    }

    @Override
    public void onTaskLoadError() {
        Toast.makeText(this, R.string.error_task_loading, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onTaskSaved(TTask tTask) {
        onBackPressed();
    }

    @Override
    public void onTaskSaveError() {
        // TODO: 2016-07-24 implement
    }

    /**
     * Reset the due time
     *
     * @return true if the due time was reset, false otherwise
     */
    public boolean onDueTimeLongClicked(View v) {
        // TODO: 2016-05-18 return false if the due time is already reset
        editTaskBinding.dueTime.setText(R.string.due_time_all_day);
        return true;
    }

    public void onTitleChanged(CharSequence title, int start, int before, int count) {
        editTaskPresenter.setTaskTitle(title.toString());
        // Clear the error
        if (editTaskBinding.taskTitle.getError() != null)
            editTaskBinding.taskTitle.setError(null);
    }

    public void onNotesChanged(CharSequence notes, int start, int before, int count) {
        editTaskPresenter.setTaskNotes(notes.toString());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthOfYear, dayOfMonth);
        Date time = c.getTime();
        editTaskPresenter.setDueDate(time);
        // Display the date after being processed by the presenter
        editTaskBinding.dueDate.setText(DateUtils.formatDate(this, editTaskPresenter.getDueDate()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        Date time = c.getTime();
        if (reminderTimeClicked) {
            editTaskBinding.reminder.setText(DateUtils.formatTime(this, time));
            editTaskPresenter.setReminderTime(time);
            reminderTimeClicked = false;
        } else {
            editTaskBinding.dueDate.setText(DateUtils.formatDate(this, time));
            editTaskBinding.dueTime.setText(DateUtils.formatTime(this, time));
            editTaskPresenter.setDueTime(time);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.done:
                if (editTaskBinding.taskTitle.length() == 0) {
                    editTaskBinding.taskTitle.setError(getString(R.string.error_no_title));
                    editTaskBinding.taskTitle.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(editTaskBinding.taskTitle, InputMethodManager.SHOW_IMPLICIT);
                    return true;
                }
                if (taskId == null)
                    editTaskPresenter.newTask(taskListId);
                else
                    editTaskPresenter.updateTask(taskListId, taskId, networkInfoReceiver.isOnline(this));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onDueDateClicked(View v) {
        KeyboardUtil.hideKeyboard(this);
        if (editTaskPresenter.hasDueDate()) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_remove_change)
                    .show();

            //noinspection ConstantConditions
            dialog.findViewById(R.id.remove).setOnClickListener(v1 -> {
                // Reset the due date & reminder
                editTaskPresenter.removeDueDate();
                editTaskPresenter.removeReminder();
                editTaskBinding.dueDate.setText(null);
                editTaskBinding.reminder.setText(null);
                dialog.dismiss();
            });
            //noinspection ConstantConditions
            dialog.findViewById(R.id.change).setOnClickListener(v1 -> {
                if (datePickerFragment == null)
                    datePickerFragment = new DatePickerFragment();
                datePickerFragment.show(getSupportFragmentManager(), "datePicker");
                dialog.dismiss();
            });
        } else {
            if (datePickerFragment == null)
                datePickerFragment = new DatePickerFragment();
            datePickerFragment.show(getSupportFragmentManager(), "datePicker");
        }
    }

    public void onDueTimeClicked(View v) {
        KeyboardUtil.hideKeyboard(this);
        if (timePickerFragment == null)
            timePickerFragment = new TimePickerFragment();
        timePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void onReminderClicked(View v) {
        if (!editTaskPresenter.hasDueDate()) {
            Toast.makeText(this, "You need to set a due date before adding a reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editTaskPresenter.hasReminder()) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_remove_change)
                    .show();

            //noinspection ConstantConditions
            dialog.findViewById(R.id.remove).setOnClickListener(v1 -> {
                editTaskPresenter.removeReminder();
                editTaskBinding.reminder.setText(null);
                dialog.dismiss();
            });
            //noinspection ConstantConditions
            dialog.findViewById(R.id.change).setOnClickListener(v1 -> {
                reminderTimeClicked = true;
                if (timePickerFragment == null)
                    timePickerFragment = new TimePickerFragment();
                timePickerFragment.show(getSupportFragmentManager(), "timePicker");
                dialog.dismiss();
            });
        } else {
            reminderTimeClicked = true;
            if (timePickerFragment == null)
                timePickerFragment = new TimePickerFragment();
            timePickerFragment.show(getSupportFragmentManager(), "timePicker");
        }
    }

    public static class DatePickerFragment extends DialogFragment {
        @Override @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getContext(), ((EditTaskActivity) getActivity()), year, month, day);
        }
    }

    public static class TimePickerFragment extends DialogFragment {
        @Override @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getContext(), ((EditTaskActivity) getActivity()), hour, minute, DateFormat.is24HourFormat(getContext()));
        }
    }
}
