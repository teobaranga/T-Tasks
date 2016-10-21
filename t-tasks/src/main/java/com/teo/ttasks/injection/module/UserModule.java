package com.teo.ttasks.injection.module;

import android.os.Build;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService;
import com.birbit.android.jobqueue.scheduling.GcmJobSchedulerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.api.PeopleApi;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.local.WidgetHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.jobs.CreateTaskJob;
import com.teo.ttasks.jobs.DeleteTaskJob;
import com.teo.ttasks.services.MyGcmJobService;
import com.teo.ttasks.services.MyJobService;
import com.teo.ttasks.ui.activities.edit_task.EditTaskPresenter;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailPresenter;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;
import com.teo.ttasks.util.NotificationHelper;
import com.teo.ttasks.widget.configure.TasksWidgetConfigurePresenter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class UserModule {

    @Provides
    TasksPresenter provideTasksPresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksPresenter(tasksHelper, prefHelper);
    }

    @Provides
    TaskListsPresenter provideTaskListsPresenter(TasksHelper tasksHelper) {
        return new TaskListsPresenter(tasksHelper);
    }

    @Provides
    MainActivityPresenter provideMainActivityPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, PeopleApi peopleApi) {
        return new MainActivityPresenter(tasksHelper, prefHelper, peopleApi);
    }

    @Provides
    EditTaskPresenter provideEditTaskPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                                               NotificationHelper notificationHelper, JobManager jobManager) {
        return new EditTaskPresenter(tasksHelper, prefHelper, widgetHelper, notificationHelper, jobManager);
    }

    @Provides
    TaskDetailPresenter provideTaskDetailPresenter(TasksHelper tasksHelper, PrefHelper prefHelper, WidgetHelper widgetHelper,
                                                   NotificationHelper notificationHelper, JobManager jobManager) {
        return new TaskDetailPresenter(tasksHelper, prefHelper, widgetHelper, notificationHelper, jobManager);
    }

    // TODO: 2016-07-27 maybe this belongs to another component
    @Provides
    TasksWidgetConfigurePresenter provideTasksWidgetConfigurePresenter(TasksHelper tasksHelper, PrefHelper prefHelper) {
        return new TasksWidgetConfigurePresenter(tasksHelper, prefHelper);
    }

    @Provides @Singleton
    JobManager provideJobManager(TTasksApp app) {
        Configuration.Builder builder = new Configuration.Builder(app)
                .injector(job -> {
                    if (job instanceof CreateTaskJob)
                        app.userComponent().inject(((CreateTaskJob) job));
                    else if (job instanceof DeleteTaskJob)
                        app.userComponent().inject(((DeleteTaskJob) job));
                })
                .minConsumerCount(1)//always keep at least one consumer alive
                .maxConsumerCount(3)//up to 3 consumers at a time
                .loadFactor(3)//3 jobs per consumer
                .consumerKeepAlive(120);//wait 2 minute
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(app, MyJobService.class), true);
        } else {
            int enableGcm = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(app);
            if (enableGcm == ConnectionResult.SUCCESS) {
                builder.scheduler(GcmJobSchedulerService.createSchedulerFor(app, MyGcmJobService.class), true);
            }
        }
        return new JobManager(builder.build());
    }
}
