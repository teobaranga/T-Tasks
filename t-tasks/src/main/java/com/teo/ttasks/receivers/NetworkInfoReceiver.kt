package com.teo.ttasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class NetworkInfoReceiver : BroadcastReceiver() {

    companion object {
        fun Context?.isOnline(): Boolean = this?.let {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return@let connectivityManager.activeNetworkInfo?.isConnected ?: false
        } ?: false
    }

    private var mOnConnectionChangedListener: ((isOnline: Boolean) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        // Ignore the initial broadcast because CONNECTIVITY_ACTION seems to be sticky
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION && !isInitialStickyBroadcast) {
            mOnConnectionChangedListener?.let { it(context.isOnline()) }
        }
    }

    fun setOnConnectionChangedListener(onConnectionChangedListener: (isOnline: Boolean) -> Unit) {
        mOnConnectionChangedListener = onConnectionChangedListener
    }
}
