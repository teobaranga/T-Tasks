package com.teo.ttasks.ui.activities.sign_in;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.remote.TokenHelper;
import com.teo.ttasks.ui.base.Presenter;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SignInPresenter extends Presenter<SignInView> {

    private final TokenHelper mTokenHelper;
    private final TasksHelper mTasksHelper;
    private final PrefHelper mPrefHelper;

    public SignInPresenter(TokenHelper tokenHelper, TasksHelper tasksHelper, PrefHelper prefHelper) {
        mPrefHelper = prefHelper;
        mTokenHelper = tokenHelper;
        mTasksHelper = tasksHelper;
    }

    /**
     * Save some user info. Useful to detect when a user is signed in.
     *
     * @param account the current user's account
     */
    void saveUser(GoogleSignInAccount account) {
        mPrefHelper.setUser(account.getEmail(), account.getDisplayName());
    }

    void signIn() {
        final Subscription subscription = mTokenHelper.refreshAccessToken()
                .flatMap(accessToken -> mTasksHelper.refreshTaskLists())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        taskListsResponse -> {
                            final SignInView view = view();
                            if (view != null) view.onSignInSuccess();
                        },
                        throwable -> {
                            Timber.e("Error signing in: %s", throwable.toString());
                            final SignInView view = view();
                            if (view != null) {
                                if (throwable.getCause() instanceof UserRecoverableAuthException)
                                    view.onSignInError(((UserRecoverableAuthException) throwable.getCause()).getIntent());
                                else
                                    view.onSignInError(null);
                            }
                        }
                );
        unsubscribeOnUnbindView(subscription);
    }
}
