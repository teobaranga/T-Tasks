package com.teo.ttasks.ui.base

import androidx.annotation.CallSuper
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Base presenter implementation.
 *
 * @param <V> view.
 */
open class Presenter<V : MvpView> {

    private val disposablesToDisposeOnUnbindView = CompositeDisposable()

    @Volatile private var view: V? = null

    @CallSuper
    open fun bindView(view: V) {
        val previousView = this.view

        if (previousView != null) {
            throw IllegalStateException("Previous view was not unbound! previousView = $previousView")
        }

        this.view = view
    }

    protected fun view(): V? = view

    /** Dispose the disposables held by this presenter to avoid memory leaks. */
    protected fun disposeOnUnbindView(disposable: Disposable, vararg disposables: Disposable) {
        disposablesToDisposeOnUnbindView.add(disposable)

        disposables.forEach { s -> disposablesToDisposeOnUnbindView.add(s) }
    }

    @CallSuper
    open fun unbindView(view: V) {
        val previousView = this.view

        if (previousView === view) {
            this.view = null
        } else {
            throw IllegalStateException("Unexpected view! previousView = $previousView, view to unbind = $view")
        }

        // Unsubscribe all subscriptions that need to be unsubscribed in this lifecycle state.
        disposablesToDisposeOnUnbindView.clear()
    }
}
