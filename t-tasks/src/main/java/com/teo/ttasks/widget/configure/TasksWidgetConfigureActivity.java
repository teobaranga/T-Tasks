package com.teo.ttasks.widget.configure;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.TasksWidgetConfigureBinding;
import com.teo.ttasks.widget.TasksWidgetProvider;

import java.util.List;

import javax.inject.Inject;

/**
 * The configuration screen for the {@link TasksWidgetProvider TasksWidgetProvider} AppWidget.
 */
public class TasksWidgetConfigureActivity extends AppCompatActivity implements TasksWidgetConfigureView {

    @Inject TasksWidgetConfigurePresenter presenter;

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private TaskListsAdapter taskListsAdapter;

    private TasksWidgetConfigureBinding binding;

    public void onAddClicked(View v) {
        // Store the task list ID associated with this widget locally
        presenter.saveWidgetTaskListId(mAppWidgetId, taskListsAdapter.getItem(binding.taskLists.getSelectedItemPosition()).getId());

        // It is the responsibility of the configuration activity to update the app widget
        // Trigger the AppWidgetProvider in order to update the widget
        Intent intent = new Intent(this, TasksWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        int[] ids = {mAppWidgetId};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    public void onCancelClicked(View v) {
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.tasks_widget_configure);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TTasksApp.get(this).userComponent().inject(this);
        presenter.bindView(this);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Set the task list adapter
        taskListsAdapter = new TaskListsAdapter(this);
        taskListsAdapter.setDropDownViewResource(R.layout.spinner_item_task_list_edit_dropdown);
        binding.taskLists.setAdapter(taskListsAdapter);

        presenter.loadTaskLists();
    }

    @Override
    public void onTaskListsLoaded(List<TaskList> taskLists) {
        taskListsAdapter.addAll(taskLists);
    }

    @Override
    public void onTaskListsLoadError() {
        // TODO: 2016-07-27 implement
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.unbindView(this);
    }
}
