package com.teo.ttasks.ui.activities.sign_in;

import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.remote.TasksHelper;
import com.teo.ttasks.data.remote.TokenHelper;
import com.teo.ttasks.ui.base.Presenter;

import java.util.concurrent.ExecutionException;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
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

    void signIn(FirebaseAuth firebaseAuth) {
        final Disposable subscription = mTokenHelper.refreshAccessToken()
                .flatMap(accessToken -> {
                    final AuthCredential credential = GoogleAuthProvider.getCredential(null, accessToken);
                    final Task<AuthResult> authResultTask = firebaseAuth.signInWithCredential(credential);
                    try {
                        final AuthResult await = Tasks.await(authResultTask);
                        Timber.d("%s %s", await.getUser().getDisplayName(), await.getUser().getEmail());
                        return Flowable.just(accessToken);
                    } catch (ExecutionException|InterruptedException e) {
                        return Flowable.error(e);
                    }
                })
                .doOnNext(ignored -> {
                    // Indicate that we're loading the task lists next
                    final SignInView view = view();
                    if (view != null) view.onLoadingTaskLists();
                })
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
