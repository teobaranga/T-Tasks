package com.teo.ttasks.ui

/*
 * Copyright 2016 Juliane Lehmann <jl@lambdasoup.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import android.animation.ValueAnimator
import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View

import com.lambdasoup.appbarsyncedfab.FabOffsetter

import timber.log.Timber

/**
 * Behavior for FABs that does not support anchoring to AppBarLayout, but instead translates the FAB
 * out of the bottom in sync with the AppBarLayout collapsing towards the top.
 *
 *
 * Extends FloatingActionButton.Behavior to keep using the pre-Lollipop shadow padding offset.
 *
 *
 * Replaces inbuilt Snackbar displacement by a relative version that does not interfere with other
 * sources of translation for the FAB; in particular not translation from the sync to the scrolling AppBarLayout.
 */
class AppBarBoundFabBehavior(context: Context, attrs: AttributeSet) : FloatingActionButton.Behavior() {

    // Whether we already registered our OnOffsetChangedListener with the AppBarLayout
    // Does not get saved in instance state, because AppBarLayout does not save its listeners either
    private var listenerRegistered = false

    private var snackbarFabTranslationYAnimator: ValueAnimator? = null

    // respect that other code may also change y translation; keep track of the part coming from us
    private var snackbarFabTranslationYByThis: Float = 0.toFloat()

    private var offsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null

    override fun layoutDependsOn(parent: CoordinatorLayout?, child: FloatingActionButton?, dependency: View?): Boolean {
        if (dependency is AppBarLayout) {

            val appBarLayout = dependency as AppBarLayout?
            val layoutParams = appBarLayout!!.getChildAt(0).layoutParams as AppBarLayout.LayoutParams
            if (layoutParams.scrollFlags == 0) {
                if (listenerRegistered) {
                    Timber.d("removing offset listener")
                    appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
                    listenerRegistered = false
                }
            } else if (!listenerRegistered) {
                Timber.d("adding offset listener")
                if (offsetChangedListener == null)
                    offsetChangedListener = FabOffsetter(parent!!, child!!)
                appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
                listenerRegistered = true
            }
        }
        return dependency is AppBarLayout || super.layoutDependsOn(parent, child, dependency)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, fab: FloatingActionButton, dependency: View): Boolean {
        if (dependency is AppBarLayout) {
            // if the dependency is an AppBarLayout, do not allow super to react on that
            // we don't want that behavior
            return true
        } else if (dependency is Snackbar.SnackbarLayout) {
            updateFabTranslationForSnackbar(parent, fab, dependency)
            return true
        }
        return super.onDependentViewChanged(parent, fab, dependency)
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View?) {
        if (dependency is Snackbar.SnackbarLayout) {
            updateFabTranslationForSnackbar(parent, child, dependency)
        }
    }

    private fun updateFabTranslationForSnackbar(parent: CoordinatorLayout, fab: FloatingActionButton, snackbar: View) {

        // We want to introduce additional y-translation (with respect to what's already there),
        // by the current visible height of any snackbar
        val targetTransYByThis = getVisibleHeightOfOverlappingSnackbar(parent, fab)

        if (snackbarFabTranslationYByThis == targetTransYByThis) {
            // We're already at (or currently animating to) the target value, return...
            return
        }

        val currentTransY = fab.translationY

        // Calculate difference between what we want now and what we wanted earlier
        val stepTransYDelta = targetTransYByThis - snackbarFabTranslationYByThis

        // ... and we're going to change the current state just by the difference
        val targetTransY = currentTransY + stepTransYDelta

        // Make sure that any current animation is cancelled
        if (snackbarFabTranslationYAnimator != null && snackbarFabTranslationYAnimator!!.isRunning) {
            snackbarFabTranslationYAnimator!!.cancel()
        }

        if (fab.isShown && Math.abs(currentTransY - targetTransY) > fab.height * 0.667f) {
            // If the FAB will be travelling by more than 2/3 of it's height, let's animate
            // it instead
            if (snackbarFabTranslationYAnimator == null) {
                snackbarFabTranslationYAnimator = ValueAnimator.ofFloat(currentTransY, targetTransY)
                snackbarFabTranslationYAnimator!!.interpolator = FastOutSlowInInterpolator()
                snackbarFabTranslationYAnimator!!.addUpdateListener { animator -> fab.translationY = animator.animatedValue as Float }
            }
            snackbarFabTranslationYAnimator!!.start()
        } else {
            // Now update the translation Y
            fab.translationY = targetTransY
        }

        snackbarFabTranslationYByThis = targetTransYByThis
    }

    /**
     * returns visible height of snackbar, if snackbar is overlapping fab
     * 0 otherwise
     */
    private fun getVisibleHeightOfOverlappingSnackbar(parent: CoordinatorLayout, fab: FloatingActionButton): Float {
        var minOffset = 0f
        val dependencies = parent.getDependencies(fab)
        var i = 0
        val z = dependencies.size
        while (i < z) {
            val view = dependencies[i]
            if (view is Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset, view.translationY - view.getHeight())
            }
            i++
        }

        return minOffset
    }
}
