package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.injection.ActivityScope;

import dagger.Module;
import dagger.Provides;

@Module
public class MainActivityModule {

    private MainActivity mMainActivity;

    public MainActivityModule(MainActivity mainActivity) {
        this.mMainActivity = mainActivity;
    }

    @Provides
    @ActivityScope
    MainActivity provideMainActivity() {
        return mMainActivity;
    }

    @Provides
    @ActivityScope
    MainActivityPresenter provideMainActivityPresenter(@NonNull RealmHelper realmHelper) {
        return new MainActivityPresenter(mMainActivity, realmHelper);
    }
}
