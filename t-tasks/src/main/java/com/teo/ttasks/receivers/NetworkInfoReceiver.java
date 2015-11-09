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

    private Context mContext;

    @SuppressWarnings("unused")
    public NetworkInfoReceiver() {
        super();
    }

    /**
     * Constructor used in order to determine the Internet connectivity
     * in a certain context
     */
    public NetworkInfoReceiver(Context context) {
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }

    public boolean isOnline() {
        if (mContext != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting());
        } else return false;
    }

}
