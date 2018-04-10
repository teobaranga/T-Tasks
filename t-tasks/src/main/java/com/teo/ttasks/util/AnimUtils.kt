package com.teo.ttasks.util

import android.annotation.TargetApi
import android.os.Build
import android.support.v4.view.ViewCompat
import android.support.v4.view.ViewPropertyAnimatorCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.transition.Transition
import android.view.View

object AnimUtils {

    object TaskDetailAnim {

        private val interpolator = FastOutSlowInInterpolator()
        private const val duration = 200
        private const val delay = 200

        fun animate(view: View): ViewPropertyAnimatorCompat {
            return ViewCompat.animate(view)
                    .alpha(1f)
                    .setDuration(duration.toLong())
                    .setStartDelay(delay.toLong())
                    .setInterpolator(interpolator)
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    open class TransitionListener : Transition.TransitionListener {
        override fun onTransitionStart(transition: Transition) {

        }

        override fun onTransitionEnd(transition: Transition) {

        }

        override fun onTransitionCancel(transition: Transition) {

        }

        override fun onTransitionPause(transition: Transition) {

        }

        override fun onTransitionResume(transition: Transition) {

        }
    }
}
