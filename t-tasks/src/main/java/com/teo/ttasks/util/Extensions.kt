package com.teo.ttasks.util

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Display a short-lived Toast message.
 */
fun Context.toastShort(@StringRes resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

/**
 * Display a short-lived Toast message.
 */
fun Context.toastShort(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
