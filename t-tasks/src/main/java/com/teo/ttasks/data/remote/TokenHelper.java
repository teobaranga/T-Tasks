package com.teo.ttasks.data.remote;

import android.accounts.Account;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;

import java.io.IOException;

import rx.Observable;
import timber.log.Timber;

import static com.google.android.gms.auth.GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE;
import static com.teo.ttasks.injection.module.ApplicationModule.SCOPE_GOOGLE_TASKS;

public final class TokenHelper {

    public static final String EXC_IO = "io";
    public static final String EXC_GOOGLE_AUTH = "gae";

    private final PrefHelper mPrefHelper;
    private final TTasksApp mTTasksApp;

    public TokenHelper(PrefHelper prefHelper, TTasksApp tTasksApp) {
        mPrefHelper = prefHelper;
        mTTasksApp = tTasksApp;
    }

    /**
     * Get the access token and save it
     *
     * @return an Observable containing the access token
     */
    public Observable<String> refreshAccessToken() {
        return Observable.defer(() -> {
            try {
                if (mPrefHelper.getAccessToken() != null)
                    GoogleAuthUtil.clearToken(mTTasksApp, mPrefHelper.getAccessToken());
                return Observable.just(GoogleAuthUtil.getToken(mTTasksApp, new Account(mPrefHelper.getUserEmail(), GOOGLE_ACCOUNT_TYPE), SCOPE_GOOGLE_TASKS))
                        .doOnNext(mPrefHelper::setAccessToken)
                        .doOnNext(Timber::d);
            } catch (IOException | GoogleAuthException e) {
                // TODO: 2016-07-15 handle user deauthorization
                return Observable.error(e);
            }
        });
    }

    @Nullable
    public Intent isTasksApiReady() {
        try {
            GoogleAuthUtil.getToken(mTTasksApp, new Account(mPrefHelper.getUserEmail(), GOOGLE_ACCOUNT_TYPE), SCOPE_GOOGLE_TASKS);
            return null;
        } catch (UserRecoverableAuthException e) {
            return e.getIntent();
        } catch (IOException e) {
            return new Intent(EXC_IO);
        } catch (GoogleAuthException e) {
            return new Intent(EXC_GOOGLE_AUTH);
        }
    }
}
