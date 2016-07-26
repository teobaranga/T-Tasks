package com.teo.ttasks.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.ui.items.TaskItem;
import com.teo.ttasks.util.RxUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.realm.Realm;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TasksRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE", Locale.getDefault());

    @Inject TasksHelper mTasksHelper;

    private List<TaskItem> mTasks;
    private Context mContext;
    private int mAppWidgetId;

    public TasksRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
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
        mTasks.clear();
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
        mTasksHelper.getTaskLists(realm)
                .flatMap(taskLists -> mTasksHelper.getTasks(taskLists.get(0).getId(), realm)) // TODO: 2016-05-01 load the right task list
                .compose(RxUtil.getTaskItems())
                .filter(taskItem -> taskItem instanceof TaskItem && ((TaskItem) taskItem).getCompleted() == null)
                .cast(TaskItem.class)
                .toList()
                .subscribe(
                        tasks -> {
                            mTasks.clear();
                            mTasks.addAll(tasks);
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
            rv.setViewVisibility(R.id.task_description, VISIBLE);
            rv.setTextViewText(R.id.date_day_name, null);
            rv.setTextViewText(R.id.date_day_number, null);
        }

        // Return the remote views object.
        return rv;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
