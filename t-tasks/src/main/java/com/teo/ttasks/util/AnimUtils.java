package com.teo.ttasks.util;

import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.animation.Interpolator;

public class AnimUtils {

    public static class TaskDetailAnim {

        private static final Interpolator interpolator = new FastOutSlowInInterpolator();
        private static final int duration = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 200 : 0;
        private static final int delay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 200 : 0;

        public static ViewPropertyAnimatorCompat animate(View view) {
            return ViewCompat.animate(view)
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator);
        }
    }
}
