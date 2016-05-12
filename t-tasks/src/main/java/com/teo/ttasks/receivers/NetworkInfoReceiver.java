package com.teo.ttasks.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

/**
 * @author Teo
 */
public class NetworkInfoReceiver extends BroadcastReceiver {

    private boolean isOnline;
    private OnConnectionChangedListener mOnConnectionChangedListener;

    @SuppressWarnings("unused")
    public NetworkInfoReceiver() {
        super();
    }

    /**
     * Constructor used in order to determine the Internet connectivity
     * in a certain context
     */
    public NetworkInfoReceiver(Context context, @Nullable OnConnectionChangedListener onConnectionChangedListener) {
        isOnline = isOnline(context);
        mOnConnectionChangedListener = onConnectionChangedListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            isOnline = isOnline(context);
            if (mOnConnectionChangedListener != null)
                mOnConnectionChangedListener.onConnectionChanged(isOnline);
        }
    }

    public boolean isOnline() {
        return isOnline;
    }

    private boolean isOnline(Context context) {
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
