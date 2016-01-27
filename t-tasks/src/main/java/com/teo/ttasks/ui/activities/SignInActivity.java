package com.teo.ttasks.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.tasks.TasksScopes;
import com.teo.ttasks.R;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.base.BaseActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SignInActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;

    private GoogleApiClient mGoogleApiClient;

    @OnClick(R.id.sign_in_button)
    public void onSignInClicked() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: 2016-01-04 implement this
        Timber.d("Connection failed");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Timber.d("handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            PrefHelper.setUser(this, result.getSignInAccount().getEmail());
            // Signed in successfully, show authenticated UI.
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // Signed out, show unauthenticated UI.
            Toast.makeText(SignInActivity.this, "There was an error signing in, please try again", Toast.LENGTH_SHORT).show();
        }
    }
}
