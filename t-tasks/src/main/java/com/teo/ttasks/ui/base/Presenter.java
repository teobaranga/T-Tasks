package com.teo.ttasks.ui.base;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Base presenter implementation.
 *
 * @param <V> view.
 */
public class Presenter<V extends MvpView> {

    @NonNull
    private final CompositeDisposable subscriptionsToUnsubscribeOnUnbindView = new CompositeDisposable();

    @Nullable
    private volatile V view;

    @CallSuper
    public void bindView(@NonNull V view) {
        final V previousView = this.view;

        if (previousView != null) {
            throw new IllegalStateException("Previous view is not unbounded! previousView = " + previousView);
        }

        this.view = view;
    }

    @Nullable
    protected V view() {
        return view;
    }

    /**
     * Unsubscribe the subscriptions held by this presenter to avoid memory leaks.
     */
    protected final void unsubscribeOnUnbindView(@NonNull Disposable subscription, @NonNull Disposable... subscriptions) {
        subscriptionsToUnsubscribeOnUnbindView.add(subscription);

        for (Disposable s : subscriptions) {
            subscriptionsToUnsubscribeOnUnbindView.add(s);
        }
    }

    @CallSuper
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    public void unbindView(@NonNull V view) {
        final V previousView = this.view;

        if (previousView == view) {
            this.view = null;
        } else {
            throw new IllegalStateException("Unexpected view! previousView = " + previousView + ", view to unbind = " + view);
        }

        // Unsubscribe all subscriptions that need to be unsubscribed in this lifecycle state.
        subscriptionsToUnsubscribeOnUnbindView.clear();
    }
}
