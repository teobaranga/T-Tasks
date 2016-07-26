package com.teo.ttasks.ui.activities;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.GoogleApiAvailability;

public class BaseGoogleApiClientActivity extends AppCompatActivity {

    /** Request code to use when launching the resolution activity */
    protected static final int RC_RESOLVE_ERROR = 1001;

    /** Bool to track whether the app is already resolving an error */
    protected boolean mResolvingError = false;

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    protected void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            Dialog dialog = googleAPI.getErrorDialog(this, connectionStatusCode, RC_RESOLVE_ERROR);
            dialog.setOnDismissListener(dialogInterface -> mResolvingError = false);
            dialog.show();
        }
    }
}
