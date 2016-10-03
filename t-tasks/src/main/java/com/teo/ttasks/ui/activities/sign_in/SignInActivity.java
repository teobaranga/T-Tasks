package com.teo.ttasks.ui.activities.sign_in;

import android.app.Dialog;
import android.app.ProgressDialog;
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
import com.google.android.gms.common.api.Scope;
import com.google.firebase.auth.FirebaseAuth;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.databinding.ActivitySignInBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.main.MainActivity;

import javax.inject.Inject;

import timber.log.Timber;

import static com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED;
import static com.teo.ttasks.injection.module.ApplicationModule.SCOPE_TASKS;

public class SignInActivity extends AppCompatActivity implements SignInView,
                                                                 GoogleApiClient.ConnectionCallbacks,
                                                                 GoogleApiClient.OnConnectionFailedListener {

    /** Request code to use when launching the resolution activity */
    private static final int RC_RESOLVE_ERROR = 1001;
    private static final int RC_SIGN_IN = 0;
    private static final int RC_USER_RECOVERABLE = 1;

    private static final String ARG_SIGN_OUT = "signOut";

    /** Bool to track whether the app is already resolving an error */
    protected boolean resolvingError = false;

    @Inject SignInPresenter signInPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    private GoogleApiClient googleApiClient;
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    public static void start(Context context, boolean signOut) {
        Intent starter = new Intent(context, SignInActivity.class);
        starter.putExtra(ARG_SIGN_OUT, signOut);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySignInBinding signInBinding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in);
        TTasksApp.get(this).signInComponent().inject(this);
        signInPresenter.bindView(this);

        firebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestScopes(new Scope(SCOPE_TASKS), new Scope(Scopes.PLUS_ME))
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
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
    }

    @Override
    protected void onDestroy() {
        signInPresenter.unbindView(this);
        super.onDestroy();
        TTasksApp.get(this).releaseSignInComponent();
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
                if (result.isSuccess()) {
                    progressDialog = ProgressDialog.show(this, null, getString(R.string.signing_in), true, false);
                    GoogleSignInAccount account = result.getSignInAccount();
                    signInPresenter.saveUser(account);
                    signInPresenter.signIn(firebaseAuth);
                } else if (result.getStatus().getStatusCode() != SIGN_IN_CANCELLED) {
                    Timber.e(result.getStatus().toString());
                    Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show();
                }
                return;
            case RC_USER_RECOVERABLE:
                if (resultCode == RESULT_OK) {
                    signInPresenter.signIn(firebaseAuth);
                    return;
                }
                Toast.makeText(this, R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLoadingTaskLists() {
        runOnUiThread(() -> {
            if (progressDialog != null)
                progressDialog.setMessage(getString(R.string.loading_task_lists));
        });
    }

    @Override
    public void onSignInSuccess() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.cancel();
        MainActivity.start(this);
        finish();
    }

    @Override
    public void onSignInError(@Nullable Intent resolveIntent) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.cancel();
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE);
        } else {
            Toast.makeText(this, R.string.error_sign_in, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (getIntent() != null) {
            if (getIntent().getBooleanExtra(ARG_SIGN_OUT, false)) {
                TTasksApp.get(this).releaseUserComponent();
                Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(status -> Timber.d(status.toString()));
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
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
