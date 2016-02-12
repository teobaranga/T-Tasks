package com.teo.ttasks.ui.base;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.teo.ttasks.R;
import com.teo.ttasks.data.local.RealmHelper;
import com.teo.ttasks.injection.ActivityScope;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.activities.main.MainActivityPresenter;

import dagger.Module;
import dagger.Provides;
import dagger.Subcomponent;

public abstract class BaseActivity extends AppCompatActivity {

    @Nullable
    private Toolbar toolbar;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupToolbar();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setupToolbar();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        setupToolbar();
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null)
            setSupportActionBar(toolbar);
    }

    @Nullable
    public Toolbar toolbar() {
        return toolbar;
    }

    protected void runOnUiThreadIfAlive(Runnable action) {
        if (isFinishing() || isDestroyed())
            return;
        runOnUiThread(action);
    }

    @ActivityScope
    @Subcomponent(modules = MainActivityModule.class)
    public interface MainActivityComponent {
        void inject(MainActivity mainActivity);

    }

    @Module
    public class MainActivityModule {

        @NonNull
        @Provides
        @ActivityScope
        MainActivityPresenter provideMainActivityPresenter(@NonNull RealmHelper realmHelper) {
            return new MainActivityPresenter(realmHelper);
        }
    }
}
