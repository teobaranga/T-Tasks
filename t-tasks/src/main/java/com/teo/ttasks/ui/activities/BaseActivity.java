package com.teo.ttasks.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.teo.ttasks.R;
import com.teo.ttasks.data.remote.TokenHelper;
import com.teo.ttasks.ui.activities.sign_in.SignInActivity;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.teo.ttasks.data.remote.TokenHelper.EXC_GOOGLE_AUTH;
import static com.teo.ttasks.data.remote.TokenHelper.EXC_IO;

public abstract class BaseActivity extends BaseGoogleApiClientActivity {

    private static final int RC_USER_RECOVERABLE = 1002;

    @Inject TokenHelper mTokenHelper;

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

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // TODO: 2016-07-25 is this necessary?
        Observable.defer(() -> Observable.just(mTokenHelper.isTasksApiReady()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        intent -> {
                            if (intent == null) {
                                onTaskApiReady();
                            } else {
                                switch (intent.getAction()) {
                                    case EXC_IO:
                                        // TODO: 2016-07-25 probably an error cause by a missing internet connection
                                        break;
                                    case EXC_GOOGLE_AUTH:
                                        // TODO: 2016-07-25 not sure what to do in this case
                                        break;
                                    default:
                                        // User recoverable action
                                        startActivityForResult(intent, RC_USER_RECOVERABLE);
                                        break;
                                }
                            }
                        },
                        throwable -> {}
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_USER_RECOVERABLE) {
            if (resultCode == RESULT_OK)
                onTaskApiReady();
            else {
                // User denied permission to his tasks, redirect to the sign in screen :(
                SignInActivity.start(this);
                finish();
            }
        }
    }

    protected abstract void onTaskApiReady();
}
