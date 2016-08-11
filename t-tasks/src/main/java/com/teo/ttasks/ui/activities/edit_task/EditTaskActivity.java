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
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityEditTaskBinding;
import com.teo.ttasks.util.DateUtils;
import com.teo.ttasks.util.NotificationUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnTextChanged;

public class EditTaskActivity extends AppCompatActivity implements EditTaskView {

    private static final String EXTRA_TASK_ID = "taskId";
    private static final String EXTRA_TASK_LIST_ID = "taskListId";

    @BindView(R.id.due_date) TextView dueDate;
    @BindView(R.id.due_time) TextView dueTime;
    @BindView(R.id.reminder) TextView reminder;
    @BindView(R.id.notes) EditText notes;

    @Inject EditTaskPresenter editTaskPresenter;

    private DatePickerFragment datePickerFragment;
    private TimePickerFragment timePickerFragment;

    private ActivityEditTaskBinding editTaskBinding;

    private TaskListsAdapter taskListsAdapter;

    private String taskId;
    private String taskListId;

    /** Flag indicating that the reminder time has been clicked */
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

    @OnClick(R.id.due_date)
    void onDueDateClicked() {
        if (datePickerFragment == null)
            datePickerFragment = new DatePickerFragment();
        datePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    /**
     * Reset the due date. This also resets the reminder date/time.
     *
     * @return true if the due date was reset, false otherwise
     */
    @OnLongClick(R.id.due_date)
    boolean onDueDateLongClicked() {
        if (!editTaskPresenter.hasDueDate())
            return false;
        dueDate.setText(R.string.due_date_missing);
        reminder.setText(null);
        editTaskPresenter.removeDueDate();
        return true;
    }

    @OnClick(R.id.due_time)
    void onDueTimeClicked() {
        if (timePickerFragment == null)
            timePickerFragment = new TimePickerFragment();
        timePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    /**
     * Reset the due time
     *
     * @return true if the due time was reset, false otherwise
     */
    @OnLongClick(R.id.due_time)
    boolean onDueTimeLongClicked() {
        // TODO: 2016-05-18 return false if the due time is already reset
        dueTime.setText(R.string.due_time_all_day);
        return true;
    }

    @OnTextChanged(R.id.task_title)
    void onTitleChanged(CharSequence title) {
        editTaskPresenter.setTaskTitle(title.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editTaskBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_task);
        TTasksApp.get(this).userComponent().inject(this);
        ButterKnife.bind(this);
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
            getSupportActionBar().setTitle("New Task");
        } else {
            editTaskPresenter.loadTaskInfo(taskId);
        }

        // Load the available task lists
        editTaskPresenter.loadTaskLists(taskListId);
    }

    @Override
    public void onTaskLoaded(TTask task) {
        editTaskBinding.setTask(task);
    }

    @Override
    public void onTaskListsLoaded(List<TaskList> taskLists, int selectedPosition) {
        taskListsAdapter.addAll(taskLists);
        editTaskBinding.taskLists.setSelection(selectedPosition);
    }

    @Override
    public void onTaskInfoError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskSaved(TTask task) {
        // Schedule the notification if it exists
        NotificationUtils.scheduleTaskNotification(this, task);
        onBackPressed();
    }

    @Override
    public void onTaskSaveError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthOfYear, dayOfMonth);
        Date time = c.getTime();
        dueDate.setText(DateUtils.formatDate(this, time));
        editTaskPresenter.setDueDate(time);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        Date time = c.getTime();
        if (reminderTimeClicked) {
            reminder.setText(DateUtils.formatTime(this, time));
            editTaskPresenter.setReminderTime(time);
            reminderTimeClicked = false;
        } else {
            dueDate.setText(DateUtils.formatDate(this, time));
            dueTime.setText(DateUtils.formatTime(this, time));
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
                if (taskId == null)
                    editTaskPresenter.newTask(taskListId);
                else
//                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    public void onReminderClicked(View v) {
        if (!editTaskPresenter.hasDueDate()) {
            Toast.makeText(this, "You need to set a due date before adding a reminder", Toast.LENGTH_SHORT).show();
            return;
        }
        reminderTimeClicked = true;
        if (timePickerFragment == null)
            timePickerFragment = new TimePickerFragment();
        timePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editTaskPresenter.unbindView(this);
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
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
