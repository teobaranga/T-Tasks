package com.teo.ttasks.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.TaskItem;
import com.teo.ttasks.util.RxUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity.EXTRA_TASK_ID;

public class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    @Inject TasksHelper tasksHelper;

    private final String taskListId;

    private String packageName;
    private List<TaskItem> taskItems;

    TasksRemoteViewsFactory(Context context, Intent intent) {
        this.packageName = context.getPackageName();
        this.taskListId = intent.getStringExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID);
        TTasksApp.get(context).userComponent().inject(this);
    }

    @Override
    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
    }

    @Override
    public void onDestroy() {
        taskItems = null;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getCount() {
        return taskItems != null ? taskItems.size() : 0;
    }

    @Override
    public void onDataSetChanged() {
        //noinspection unchecked
        tasksHelper.getTasks(taskListId)
                .compose(RxUtils.getTaskItems(true))
                .cast((Class<List<TaskItem>>) (Class<?>) List.class)
                .subscribe(
                        taskItems -> this.taskItems = taskItems,
                        throwable -> Timber.e(throwable.toString()));
        Timber.d("Widget taskItems count %d", taskItems.size());
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        TaskItem task = taskItems.get(position);

        RemoteViews rv = new RemoteViews(packageName, R.layout.item_task_widget);

        // Title
        rv.setTextViewText(R.id.task_title, task.getTitle());

        // Set the click action
        Intent intent = new Intent();
        intent.putExtra(EXTRA_TASK_ID, task.getTaskId());
        rv.setOnClickFillInIntent(R.id.item_task_widget, intent);

        // Task description
        final String notes = task.getNotes();
        if (notes == null || notes.isEmpty()) {
            rv.setViewVisibility(R.id.task_description, GONE);
        } else {
            rv.setTextViewText(R.id.task_description, notes);
            rv.setViewVisibility(R.id.task_description, VISIBLE);
        }

        // Due date
        final Date dueDate = task.getDueDate();
        if (dueDate != null) {
            simpleDateFormat.applyLocalizedPattern("EEE");
            rv.setTextViewText(R.id.date_day_name, simpleDateFormat.format(dueDate));
            simpleDateFormat.applyLocalizedPattern("d");
            rv.setTextViewText(R.id.date_day_number, simpleDateFormat.format(dueDate));

            // Reminder
            Date reminder = task.getReminder();
            if (reminder != null) {
                simpleDateFormat.applyLocalizedPattern("hh:mma");
                rv.setTextViewText(R.id.reminder, simpleDateFormat.format(reminder));
                rv.setViewVisibility(R.id.reminder, VISIBLE);
            } else {
                rv.setViewVisibility(R.id.reminder, GONE);
            }
        } else {
            rv.setTextViewText(R.id.date_day_name, null);
            rv.setTextViewText(R.id.date_day_number, null);
        }

        // Return the remote views object.
        return rv;
    }

    @Override
    public long getItemId(int position) {
        return taskItems.get(position).getIdentifier();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
