package com.teo.ttasks.ui.activities.sign_in;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.ui.activities.BaseGoogleApiClientActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SignInActivity extends BaseGoogleApiClientActivity implements SignInView, OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;

    @Inject SignInPresenter mSignInPresenter;
    @Inject GoogleApiClient mGoogleApiClient;

    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    public static void start(Context context) {
        Intent starter = new Intent(context, SignInActivity.class);
        context.startActivity(starter);
    }

    @OnClick(R.id.sign_in_button)
    void onSignInClicked() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        TTasksApp.get(this).signInComponent().inject(this);
        ButterKnife.bind(this);
        mSignInPresenter.bindView(this);

        mGoogleApiClient.registerConnectionFailedListener(this);

//        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(status -> {
//            Timber.d("done revoking access");
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        switch (requestCode) {
            case RC_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected())
                        mGoogleApiClient.connect();
                }
                break;
            case RC_SIGN_IN:
                final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                Timber.d("handleSignInResult: %s", result.isSuccess());
                if (result.isSuccess()) {
                    GoogleSignInAccount account = result.getSignInAccount();
                    mSignInPresenter.signIn(account);
                } else {
                    Timber.d(result.getStatus().toString());
                    // Signed out, show unauthenticated UI.
                    Toast.makeText(this, R.string.sign_in_error, Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onSignInSuccess() {
        MainActivity.start(this);
        finish();
    }

    @Override
    public void onSignInError() {
        Toast.makeText(this, "There was an error while signing you in, please try again", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!mResolvingError) {
            if (result.hasResolution()) {
                try {
                    mResolvingError = true;
                    result.startResolutionForResult(this, RC_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }
            } else {
                // Show dialog using GoogleApiAvailability.getErrorDialog()
                showGooglePlayServicesAvailabilityErrorDialog(result.getErrorCode());
                mResolvingError = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        mSignInPresenter.unbindView(this);
        super.onDestroy();
        TTasksApp.get(this).releaseSignInComponent();
    }
}
