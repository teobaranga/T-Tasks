package com.teo.ttasks.ui.base;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.performance.AsyncJobsObserver;
import com.teo.ttasks.ui.fragments.tasks.TasksPresenter;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;
import rx.schedulers.Schedulers;

public abstract class BaseFragment extends Fragment {

    // Due to bug (https://github.com/google/dagger/issues/214) in Dagger 2 we can not inject handler here, sorry.
    @NonNull
    private static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    protected void runOnUiThreadIfFragmentAlive(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper() && isFragmentAlive()) {
            runnable.run();
        } else {
            MAIN_THREAD_HANDLER.post(() -> {
                if (isFragmentAlive()) {
                    runnable.run();
                }
            });
        }
    }

    private boolean isFragmentAlive() {
        return getActivity() != null && isAdded() && !isDetached() && getView() != null && !isRemoving();
    }

    @Subcomponent(modules = TasksFragmentModule.class)
    public interface TasksFragmentComponent {
        void inject(TasksFragment orderFragment);
    }

    @Module
    public static class TasksFragmentModule {
        @Provides
        @NonNull
        public TasksPresenter provideTasksPresenter(@NonNull TasksHelper tasksHelper,
                                                    @NonNull RealmHelper realmHelper,
                                                    @NonNull AsyncJobsObserver asyncJobsObserver) {
            return new TasksPresenter(
                    Schedulers.io(),
                    tasksHelper,
                    realmHelper,
                    asyncJobsObserver
            );
        }
    }
}
