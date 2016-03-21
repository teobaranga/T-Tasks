package com.teo.ttasks.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.teo.ttasks.TTasksApp;

import javax.inject.Inject;
import javax.inject.Named;

import static com.teo.ttasks.injection.module.ApplicationModule.MAIN_THREAD_HANDLER;

public abstract class BaseFragment extends Fragment {

    @Inject @Named(MAIN_THREAD_HANDLER) Handler mMainThreadHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(getContext()).applicationComponent().inject(this);
    }

    protected void runOnUiThreadIfFragmentAlive(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper() && isFragmentAlive()) {
            runnable.run();
        } else {
            mMainThreadHandler.post(() -> {
                if (isFragmentAlive()) {
                    runnable.run();
                }
            });
        }
    }

    private boolean isFragmentAlive() {
        return getActivity() != null && isAdded() && !isDetached() && getView() != null && !isRemoving();
    }
}
