package com.teo.ttasks.injection.component;

import android.support.annotation.NonNull;

import com.teo.ttasks.injection.module.TasksApiModule;
import com.teo.ttasks.injection.UserScope;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;
import com.teo.ttasks.ui.base.BaseFragment;

import dagger.Subcomponent;

@UserScope
@Subcomponent(modules = TasksApiModule.class)
public interface TasksApiComponent {

    void inject(@NonNull MainActivityPresenter mainActivityPresenter);

    @NonNull
    BaseFragment.TasksFragmentComponent plus(@NonNull BaseFragment.TasksFragmentModule ordersFragmentModule);
}
