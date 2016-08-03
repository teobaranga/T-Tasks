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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityEditTaskBinding;
import com.teo.ttasks.util.DateUtil;

import java.util.Calendar;
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

    @BindView(R.id.due_date) TextView mDueDate;
    @BindView(R.id.due_time) TextView mDueTime;
    @BindView(R.id.task_notes) EditText mTaskNotes;

    @Inject EditTaskPresenter mEditTaskPresenter;

    private DatePickerFragment mDatePickerFragment;
    private TimePickerFragment mTimePickerFragment;

    private ActivityEditTaskBinding mBinding;

    private TaskListsAdapter mTaskListsAdapter;

    private String taskId;
    private String taskListId;

    public static void startEdit(Context context, String taskId, String taskListId, Bundle bundle) {
        Intent starter = new Intent(context, EditTaskActivity.class);
        starter.putExtra(EXTRA_TASK_ID, taskId);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        context.startActivity(starter, bundle);
    }

    public static void startCreate(Fragment fragment, String taskListId, int requestCode, Bundle bundle) {
        Intent starter = new Intent(fragment.getContext(), EditTaskActivity.class);
        starter.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        fragment.startActivityForResult(starter, requestCode, bundle);
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
        if (mDatePickerFragment == null)
            mDatePickerFragment = new DatePickerFragment();
        mDatePickerFragment.show(getSupportFragmentManager(), "datePicker");
    }

    /**
     * Reset the due date
     *
     * @return true if the due date was reset, false otherwise
     */
    @OnLongClick(R.id.due_date)
    boolean onDueDateLongClicked() {
        // TODO: 2016-05-18 return false if the due date is already reset
        mDueDate.setText(R.string.due_date_missing);
        return true;
    }

    @OnClick(R.id.due_time)
    void onDueTimeClicked() {
        if (mTimePickerFragment == null)
            mTimePickerFragment = new TimePickerFragment();
        mTimePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    /**
     * Reset the due time
     *
     * @return true if the due time was reset, false otherwise
     */
    @OnLongClick(R.id.due_time)
    boolean onDueTimeLongClicked() {
        // TODO: 2016-05-18 return false if the due time is already reset
        mDueTime.setText(R.string.due_time_all_day);
        return true;
    }

    @OnTextChanged(R.id.task_title)
    void onTitleChanged(CharSequence title) {
        mEditTaskPresenter.setTaskTitle(title.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_task);
        TTasksApp.get(this).userComponent().inject(this);
        ButterKnife.bind(this);
        mEditTaskPresenter.bindView(this);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        taskListId = getIntent().getStringExtra(EXTRA_TASK_LIST_ID);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTaskListsAdapter = new TaskListsAdapter(this);
        mTaskListsAdapter.setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown);
        mBinding.taskLists.setAdapter(mTaskListsAdapter);

        // Handle a new task or an existing task
        if (taskId == null) {
            getSupportActionBar().setTitle("New Task");
        } else {
            mEditTaskPresenter.loadTaskInfo(taskId);
        }

        // Load the available task lists
        mEditTaskPresenter.loadTaskLists(taskListId);
    }

    @Override
    public void onTaskLoaded(Task task) {
        mBinding.setTask(task);
    }

    @Override
    public void onTaskListsLoaded(List<TaskList> taskLists, int selectedPosition) {
        mTaskListsAdapter.addAll(taskLists);
        mBinding.taskLists.setSelection(selectedPosition);
    }

    @Override
    public void onTaskInfoError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onTaskSaved() {
        setResult(RESULT_OK);
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
        mDueDate.setText(DateUtil.formatDate(this, c.getTime()));
        mDueTime.setText(DateUtil.formatTime(this, c.getTime()));
        mEditTaskPresenter.setDueDate(c.getTime());
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        mDueDate.setText(DateUtil.formatDate(this, c.getTime()));
        mDueTime.setText(DateUtil.formatTime(this, c.getTime()));
        mEditTaskPresenter.setDueTime(c.getTime());
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
                    mEditTaskPresenter.newTask(taskListId);
                else
//                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEditTaskPresenter.unbindView(this);
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
            return new DatePickerDialog(getActivity(), ((EditTaskActivity) getActivity()), year, month, day);
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
            return new TimePickerDialog(getActivity(), ((EditTaskActivity) getActivity()), hour, minute, DateFormat.is24HourFormat(getActivity()));
        }
    }
}
