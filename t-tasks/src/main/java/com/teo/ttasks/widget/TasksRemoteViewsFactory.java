package com.teo.ttasks.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.api.entities.TasksResponse;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.TaskItem;
import com.teo.ttasks.util.RxUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import rx.Observable;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity.EXTRA_TASK_ID;

public class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    private final String taskListId;

    private Context mContext;
    private List<TaskItem> mTasks;

    TasksRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        taskListId = intent.getStringExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID);
    }

    @Override
    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
        TTasksApp.get(mContext).applicationComponent().inject(this);
        mTasks = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        mTasks = null;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getCount() {
        return mTasks.size();
    }

    @Override
    public void onDataSetChanged() {
        Realm realm = Realm.getDefaultInstance();
        TasksResponse tasksResponse = realm.where(TasksResponse.class).equalTo("id", taskListId).findFirst();
        if (tasksResponse == null) {
            // Empty task list
            return;
        }
        List<Task> tasks = tasksResponse.items;
        Observable.just(tasks)
                .compose(RxUtil.getTaskItems())
                .map(iItems -> {
                    // Get only tasks that are in progress
                    List<TaskItem> taskItems = new ArrayList<>();
                    for (IItem iItem : iItems) {
                        if (iItem instanceof TaskItem && ((TaskItem) iItem).getCompleted() == null)
                            taskItems.add((TaskItem) iItem);
                    }
                    return taskItems;
                })
                .subscribe(
                        taskItems -> {
                            Timber.d("Widget tasks count %d", taskItems.size());
                            mTasks.clear();
                            mTasks.addAll(taskItems);
                            realm.close();
                        },
                        throwable -> {
                            Timber.e(throwable.toString());
                            realm.close();
                        });
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        TaskItem task = mTasks.get(position);

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.item_task_widget);
        rv.setTextViewText(R.id.task_title, task.getTitle());

        // Set the click action
        Intent intent = new Intent();
        intent.putExtra(EXTRA_TASK_ID, task.getTaskId());
        rv.setOnClickFillInIntent(R.id.item_task_widget, intent);

        // Task description
        if (task.getNotes() == null) {
            rv.setViewVisibility(R.id.task_description, GONE);
        } else {
            rv.setTextViewText(R.id.task_description, task.getNotes());
            rv.setViewVisibility(R.id.task_description, VISIBLE);
        }

        if (task.getDueDate() != null) {
            Date dueDate = task.getDueDate();
            simpleDateFormat.applyLocalizedPattern("EEE");
            rv.setTextViewText(R.id.date_day_name, simpleDateFormat.format(dueDate));
            simpleDateFormat.applyLocalizedPattern("d");
            rv.setTextViewText(R.id.date_day_number, simpleDateFormat.format(dueDate));

            Date reminder = task.getReminderDate();
            if (reminder != null) {
                simpleDateFormat.applyLocalizedPattern("hh:mma");
                rv.setTextViewText(R.id.task_reminder, simpleDateFormat.format(reminder));
                rv.setViewVisibility(R.id.task_reminder, VISIBLE);
            } else {
                rv.setViewVisibility(R.id.task_reminder, GONE);
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
        return mTasks.get(position).getIdentifier();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
