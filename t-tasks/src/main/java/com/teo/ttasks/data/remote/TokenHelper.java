package com.teo.ttasks.data.remote;

import android.accounts.Account;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.Scopes;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;

import java.io.IOException;

import io.reactivex.Flowable;
import timber.log.Timber;

import static com.google.android.gms.auth.GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE;
import static com.teo.ttasks.injection.module.ApplicationModule.SCOPE_TASKS;

public final class TokenHelper {

    public static final String EXC_IO = "io";
    public static final String EXC_GOOGLE_AUTH = "gae";
    private static final String APP_SCOPES = "oauth2:" + SCOPE_TASKS + " " + Scopes.PLUS_ME;

    private final PrefHelper mPrefHelper;
    private final TTasksApp mTTasksApp;

    /** The current user's Google account */
    private Account mAccount;

    public TokenHelper(PrefHelper prefHelper, TTasksApp tTasksApp) {
        mPrefHelper = prefHelper;
        mTTasksApp = tTasksApp;
        if (mPrefHelper.getUserEmail() != null)
            mAccount = new Account(mPrefHelper.getUserEmail(), GOOGLE_ACCOUNT_TYPE);
    }

    /**
     * Get the access token and save it
     *
     * @return an Flowable containing the access token
     */
    public Flowable<String> refreshAccessToken() {
        if (mAccount == null)
            mAccount = new Account(mPrefHelper.getUserEmail(), GOOGLE_ACCOUNT_TYPE);
        return Flowable.defer(() -> {
            try {
                if (mPrefHelper.getAccessToken() != null)
                    GoogleAuthUtil.clearToken(mTTasksApp, mPrefHelper.getAccessToken());
                return Flowable.just(GoogleAuthUtil.getToken(mTTasksApp, mAccount, APP_SCOPES))
                        .doOnNext(mPrefHelper::setAccessToken)
                        .doOnNext(Timber::d);
            } catch (IOException | GoogleAuthException e) {
                return Flowable.error(e);
            }
        });
    }

    @Nullable
    public Intent isTokenAvailable() {
        if (mAccount == null)
            mAccount = new Account(mPrefHelper.getUserEmail(), GOOGLE_ACCOUNT_TYPE);
        try {
            GoogleAuthUtil.getToken(mTTasksApp, mAccount, APP_SCOPES);
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
