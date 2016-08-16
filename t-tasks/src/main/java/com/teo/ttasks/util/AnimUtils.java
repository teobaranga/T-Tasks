package com.teo.ttasks.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.transition.Transition;
import android.view.View;
import android.view.animation.Interpolator;

public class AnimUtils {

    private AnimUtils() { }

    public static class TaskDetailAnim {

        private static final Interpolator interpolator = new FastOutSlowInInterpolator();
        private static final int duration = 200;
        private static final int delay = 200;

        public static ViewPropertyAnimatorCompat animate(View view) {
            return ViewCompat.animate(view)
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static class TransitionListener implements Transition.TransitionListener {
        @Override public void onTransitionStart(Transition transition) {

        }

        @Override public void onTransitionEnd(Transition transition) {

        }

        @Override public void onTransitionCancel(Transition transition) {

        }

        @Override public void onTransitionPause(Transition transition) {

        }

        @Override public void onTransitionResume(Transition transition) {

        }
    }
}
