package com.teo.ttasks.injection.component;

import com.teo.ttasks.injection.module.UserModule;
import com.teo.ttasks.jobs.CreateTaskJob;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.services.MyGcmJobService;
import com.teo.ttasks.services.MyJobService;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;
import com.teo.ttasks.widget.TasksRemoteViewsFactory;
import com.teo.ttasks.widget.TasksWidgetProvider;
import com.teo.ttasks.widget.configure.TasksWidgetConfigureActivity;

import javax.inject.Singleton;

import dagger.Subcomponent;

@Singleton
@Subcomponent(modules = UserModule.class)
public interface UserComponent {

    void inject(MainActivity mainActivity);

    void inject(TaskDetailActivity taskDetailActivity);

    void inject(EditTaskActivity editTaskActivity);

    void inject(TasksFragment tasksFragment);

    void inject(TaskListsFragment taskListsFragment);

    void inject(TasksWidgetConfigureActivity tasksWidgetConfigureActivity);

    void inject(TasksWidgetProvider tasksWidgetProvider);

    void inject(TasksRemoteViewsFactory tasksRemoteViewsFactory);

    void inject(TaskNotificationReceiver taskNotificationReceiver);

    void inject(MyJobService myJobService);

    void inject(MyGcmJobService myGcmJobService);

    void inject(CreateTaskJob createTaskJob);
}
