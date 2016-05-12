package com.teo.ttasks.ui.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.tasks.TasksScopes;
import com.greysonparrelli.permiso.Permiso;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.ui.activities.main.MainActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SignInActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;

    private GoogleApiClient mGoogleApiClient;

    public static void start(Context context) {
        Intent starter = new Intent(context, SignInActivity.class);
        context.startActivity(starter);
    }

    @OnClick(R.id.sign_in_button)
    public void onSignInClicked() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        Permiso.getInstance().setActivity(this);
        ButterKnife.bind(this);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope(TasksScopes.TASKS))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: 2016-01-04 implement this
        Timber.w("Connection failed");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Timber.d("handleSignInResult: %s", result.isSuccess());
            if (result.isSuccess()) {
                Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                    @Override
                    public void onPermissionResult(Permiso.ResultSet resultSet) {
                        if (resultSet.areAllPermissionsGranted()) {
                            // Email is not null as long as it was requested when building the request
                            GoogleSignInAccount account = result.getSignInAccount();

                            //noinspection ConstantConditions
                            PrefHelper.setUser(SignInActivity.this, account.getEmail(), account.getDisplayName(), account.getPhotoUrl());

                            TTasksApp.get(SignInActivity.this).tasksComponent();

                            // Signed in successfully, show authenticated UI.
                            MainActivity.start(SignInActivity.this);
                            finish();
                        }
                    }

                    @Override
                    public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                        Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
                    }
                }, Manifest.permission.GET_ACCOUNTS);
            } else {
                // Signed out, show unauthenticated UI.
                Toast.makeText(this, R.string.sign_in_error, Toast.LENGTH_LONG).show();
            }
        }
    }
}
