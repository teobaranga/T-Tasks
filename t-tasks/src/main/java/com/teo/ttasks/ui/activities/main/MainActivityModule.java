package com.teo.ttasks.ui.activities.main;

import android.support.annotation.NonNull;

import com.teo.ttasks.data.RealmModel;
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
    MainActivityPresenter provideMainActivityPresenter(@NonNull RealmModel realmModel) {
        return new MainActivityPresenter(mMainActivity, realmModel);
    }
}
