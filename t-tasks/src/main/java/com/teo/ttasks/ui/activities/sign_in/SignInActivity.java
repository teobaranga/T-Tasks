package com.teo.ttasks.ui.activities.sign_in;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.databinding.ActivitySignInBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.main.MainActivity;

import javax.inject.Inject;

import timber.log.Timber;

import static com.teo.ttasks.injection.module.ApplicationModule.SCOPE_TASKS;

public class SignInActivity extends AppCompatActivity implements SignInView, OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;
    private static final int RC_USER_RECOVERABLE = 1;

    @Inject SignInPresenter signInPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    private GoogleApiClient googleApiClient;

    /** Request code to use when launching the resolution activity */
    protected static final int RC_RESOLVE_ERROR = 1001;

    /** Bool to track whether the app is already resolving an error */
    protected boolean resolvingError = false;

    public static void start(Context context) {
        Intent starter = new Intent(context, SignInActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySignInBinding signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in);
        TTasksApp.get(this).signInComponent().inject(this);
        signInPresenter.bindView(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(SCOPE_TASKS), new Scope(Scopes.PLUS_ME))
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInBinding.signInButton.setOnClickListener(view -> {
            if (!networkInfoReceiver.isOnline(this)) {
                Toast.makeText(this, R.string.error_sign_in_offline, Toast.LENGTH_SHORT).show();
            } else {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

//        Auth.GoogleSignInApi.revokeAccess(googleApiClient).setResultCallback(status -> {
//            Timber.d("done revoking access");
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        switch (requestCode) {
            case RC_RESOLVE_ERROR:
                resolvingError = false;
                if (resultCode == RESULT_OK) {
                    // Make sure the app is not already connected or attempting to connect
                    if (!googleApiClient.isConnecting() && !googleApiClient.isConnected())
                        googleApiClient.connect();
                }
                return;
            case RC_SIGN_IN:
                final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                Timber.d("handleSignInResult: %s", result.isSuccess());
                if (result.isSuccess()) {
                    GoogleSignInAccount account = result.getSignInAccount();
                    signInPresenter.saveUser(account);
                    signInPresenter.signIn();
                } else {
                    Timber.e(result.getStatus().toString());
                    Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show();
                }
                return;
            case RC_USER_RECOVERABLE:
                if (resultCode == RESULT_OK) {
                    signInPresenter.signIn();
                    return;
                }
                Toast.makeText(this, R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSignInSuccess() {
        MainActivity.start(this);
        finish();
    }

    @Override
    public void onSignInError(@Nullable Intent resolveIntent) {
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE);
        } else {
            Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (!resolvingError) {
            if (result.hasResolution()) {
                try {
                    resolvingError = true;
                    result.startResolutionForResult(this, RC_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    googleApiClient.connect();
                }
            } else {
                // Show dialog using GoogleApiAvailability.getErrorDialog()
                showGooglePlayServicesAvailabilityErrorDialog(result.getErrorCode());
                resolvingError = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        signInPresenter.unbindView(this);
        super.onDestroy();
        TTasksApp.get(this).releaseSignInComponent();
    }

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
            dialog.setOnDismissListener(dialogInterface -> resolvingError = false);
            dialog.show();
        }
    }
}
