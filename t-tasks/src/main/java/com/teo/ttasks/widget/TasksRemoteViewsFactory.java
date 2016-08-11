package com.teo.ttasks.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.mikepenz.fastadapter.IItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTask;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.TaskItem;
import com.teo.ttasks.util.RxUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;
import rx.Observable;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity.EXTRA_TASK_ID;

public class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    private final String taskListId;

    private Context context;
    private List<TaskItem> taskItems;
    private Realm realm;

    TasksRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.taskListId = intent.getStringExtra(TaskDetailActivity.EXTRA_TASK_LIST_ID);
    }

    @Override
    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.
        taskItems = new ArrayList<>();
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        context = null;
        taskItems = null;
        realm.close();
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getCount() {
        return taskItems.size();
    }

    @Override
    public void onDataSetChanged() {
        RealmResults<TTask> tasks = realm.where(TTask.class).equalTo("taskListId", taskListId).findAll();
        if (tasks.isEmpty())
            return;
        Observable.just(tasks)
                .compose(RxUtils.getTaskItems())
                .map(iItems -> {
                    // Get only taskItems that are in progress
                    List<TaskItem> taskItems = new ArrayList<>();
                    for (IItem iItem : iItems) {
                        if (iItem instanceof TaskItem && ((TaskItem) iItem).getCompleted() == null)
                            taskItems.add((TaskItem) iItem);
                    }
                    return taskItems;
                })
                .subscribe(
                        taskItems -> {
                            this.taskItems = taskItems;
                            Timber.d("Widget taskItems count %d", taskItems.size());
                        },
                        throwable -> Timber.e(throwable.toString()));
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        TaskItem task = taskItems.get(position);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.item_task_widget);
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

            Date reminder = task.getReminder();
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
        return taskItems.get(position).getIdentifier();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
