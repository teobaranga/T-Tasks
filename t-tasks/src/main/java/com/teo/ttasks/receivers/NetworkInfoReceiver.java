package com.teo.ttasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * @author Teo
 */
public class NetworkInfoReceiver extends BroadcastReceiver {

    private OnConnectionChangedListener mOnConnectionChangedListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (mOnConnectionChangedListener != null)
                mOnConnectionChangedListener.onConnectionChanged(isOnline(context));
        }
    }

    public void setOnConnectionChangedListener(OnConnectionChangedListener onConnectionChangedListener) {
        mOnConnectionChangedListener = onConnectionChangedListener;
    }

    public boolean isOnline(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting());
        } else return false;
    }

    public interface OnConnectionChangedListener {
        void onConnectionChanged(boolean isOnline);
    }
}
