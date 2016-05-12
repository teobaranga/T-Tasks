package com.teo.ttasks.ui.base;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Base presenter implementation.
 *
 * @param <V> view.
 */
public class Presenter<V extends MvpView> {

    @NonNull
    private final CompositeSubscription subscriptionsToUnsubscribeOnUnbindView = new CompositeSubscription();

    @Nullable
    private volatile V view;

    @CallSuper
    public void bindView(@NonNull V view) {
        this.view = view;
    }

    @Nullable
    protected V view() {
        return view;
    }

    protected final void unsubscribeOnUnbindView(@NonNull Subscription subscription, @NonNull Subscription... subscriptions) {
        subscriptionsToUnsubscribeOnUnbindView.add(subscription);

        for (Subscription s : subscriptions) {
            subscriptionsToUnsubscribeOnUnbindView.add(s);
        }
    }

    @CallSuper
    public void unbindView() {
        this.view = null;
        // Unsubscribe all subscriptions that need to be unsubscribed in this lifecycle state.
        subscriptionsToUnsubscribeOnUnbindView.clear();
    }
}
