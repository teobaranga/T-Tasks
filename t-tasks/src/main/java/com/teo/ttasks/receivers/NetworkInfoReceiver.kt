package com.teo.ttasks.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class NetworkInfoReceiver : BroadcastReceiver() {

    private var mOnConnectionChangedListener: ((isOnline: Boolean) -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        // Ignore the initial broadcast because CONNECTIVITY_ACTION seems to be sticky
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION && !isInitialStickyBroadcast) {
            mOnConnectionChangedListener?.let { it(isOnline(context)) }
        }
    }

    fun setOnConnectionChangedListener(onConnectionChangedListener: (isOnline: Boolean) -> Unit) {
        mOnConnectionChangedListener = onConnectionChangedListener
    }

    fun isOnline(context: Context?): Boolean = context?.let {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.activeNetworkInfo?.isConnected ?: false
    } ?: false
}
