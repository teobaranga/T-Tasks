/*
 * Copyright (c) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.teo.sample;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.common.ConnectionResult;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSubheader;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialAccountListener;
import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

/**
 * Sample activity for Google Tasks API v1. It demonstrates how to use authorization to list tasks
 * with the user's permission.
 *
 * @author Yaniv Inbar
 */


public final class MainActivity extends MaterialNavigationDrawer implements MaterialAccountListener,
        MaterialAccount.OnAccountDataLoaded, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    MaterialAccount account;
    MaterialAccount[] accounts;
    MaterialSection tasksSection, section2, helpSection, aboutSection, settingsSection;

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;
    private static final int RC_ADD = 3;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;

    static final String TAG = "MainActivity";

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;
    static final int REQUEST_AUTHORIZATION = 2;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    SwipeRefreshLayout mSwipeRefreshLayout = null;
    ArrayList<Triplet<String,String,String>> tasksList;
    RecyclerAdapter adapter;
    Tasks service;
    TasksFragment frag;

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog =
                        GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, MainActivity.this,
                                REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    void refreshView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.getAdapter().notifyDataSetChanged();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_ADD:
                if(resultCode == RESULT_OK) {
                    MaterialAccount acc = new MaterialAccount(this.getResources(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),"", R.drawable.ic_photo, R.drawable.ic_cover);
                    this.addAccount(acc);
                    Toast.makeText(MainActivity.this, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), Toast.LENGTH_SHORT).show();
                }
                break;
            case RC_SIGN_IN:
                mIntentInProgress = false;

                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    private void chooseAccount() {
        // disconnects the account and the account picker
        // stays there until another account is chosen
        if (mGoogleApiClient.isConnected()) {
            // Prior to disconnecting, run clearDefaultAccount().
//            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
//                    .setResultCallback(new ResultCallback<Status>() {
//
//                        public void onResult(Status status) {
//                            // mGoogleApiClient is now disconnected and access has been revoked.
//                            // Trigger app logic to comply with the developer policies
//                        }
//
//                    });
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }
    }

    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            MaterialAccount acc = new MaterialAccount(MainActivity.this.getResources(), "",
                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME), R.drawable.ic_photo, R.drawable.ic_cover);
            MainActivity.this.addAccount(acc);

//            mAccount = new Account(
//                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
//                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
//            );
            // do more stuff
        }
    }

    @Override
    public void init(Bundle savedInstanceState) {

        tasksList = new ArrayList<>();
        Scope taskScope = new Scope(TasksScopes.TASKS);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addScope(taskScope)
                .build();

        // add placeholder account
        account = new MaterialAccount(this.getResources(), "","", R.drawable.ic_photo, R.drawable.ic_cover);
        this.addAccount(account);

        // set listener
        this.setAccountListener(this);

        // create sections
        adapter = new RecyclerAdapter(tasksList);
        frag = new TasksFragment();

        tasksSection = this.newSection("Tasks", this.getResources().getDrawable(R.drawable.ic_assignment_white_48dp), frag);
        section2 = this.newSection("Add an account", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                Toast.makeText(MainActivity.this, "Section 2 Clicked", Toast.LENGTH_SHORT).show();
                AccountManager acm = AccountManager.get(getApplicationContext());
                acm.addAccount("com.google", null, null, null, MainActivity.this, new OnAccountAddComplete(), null);
                //Toast.makeText(MainActivity.this, acm.getAccounts().toString(), Toast.LENGTH_SHORT).show();
//                Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null, new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, true, null, null, null, null);
//                startActivityForResult(googlePicker, RC_ADD);
                section.unSelect();
            }
        });

        settingsSection = this.newSection("Settings",this.getResources().getDrawable(R.drawable.ic_settings_white_48dp), new Intent(this,SettingsActivity.class));
        helpSection = this.newSection("Help & Feedback", this.getResources().getDrawable(R.drawable.ic_help_white_48dp), new Intent(this,SettingsActivity.class));
        aboutSection = this.newSection("About Tasks", this.getResources().getDrawable(R.drawable.ic_info_outline_white_48dp), new Intent(this,SettingsActivity.class));

        // add your sections to the drawer
        this.addSection(tasksSection);
        this.addSection(section2);
        this.addDivisor();
        this.addBottomSection(settingsSection);
        this.addBottomSection(helpSection);
        this.addBottomSection(aboutSection);
        this.disableLearningPattern();

        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_CUSTOM);
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(result.getResolution().getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    public void onConnected(Bundle connectionHint) {
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.
        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            String personName = currentPerson.getDisplayName();
            String cover = null;
            if(currentPerson.hasCover())
                cover = currentPerson.getCover().getCoverPhoto().getUrl();
            String pic = currentPerson.getImage().getUrl();
            final String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

            // Google Accounts
            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    String token = null;

                    try {
                        token = GoogleAuthUtil.getToken(MainActivity.this,
                                                        email,
                                                        "oauth2:" + TasksScopes.TASKS + " " + Plus.SCOPE_PLUS_PROFILE.toString());
                    } catch (IOException transientEx) {
                        // Network or server error, try later
                        Log.e(TAG, transientEx.toString());
                    } catch (UserRecoverableAuthException e) {
                        // Recover (with e.getIntent())
                        Log.e(TAG, e.toString());
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, RC_SIGN_IN);
                    } catch (GoogleAuthException authEx) {
                        // The call is not ever expected to succeed
                        // assuming you have already verified that
                        // Google Play services is installed.
                        Log.e(TAG, authEx.toString());
                    }
                    return token;
                }

                @Override
                protected void onPostExecute(String token) {
                    //Log.i(TAG, token);
                    GoogleCredential credential = new GoogleCredential();
                    credential.setAccessToken(token);
                    // Tasks client
                    service = new com.google.api.services.tasks.Tasks.Builder(httpTransport, jsonFactory, credential)
                            .setApplicationName("Google-TasksAndroidSample/1.0").build();

                    AsyncLoadTasks.run(MainActivity.this);
                }
            };
            task.execute();

            account.setTitle(personName);
            account.setSubTitle(email);

            new setPics().execute(pic, cover);
        }
    }

    @Override
    public void onAccountOpening(MaterialAccount account) {
        // open profile activity
    }

    @Override
    public void onChangeAccount(MaterialAccount newAccount) {
        // when another account is selected
    }

    class setPics extends AsyncTask<String, Void, List<Bitmap>> {

        protected List<Bitmap> doInBackground(String... url) {
            try {
                Bitmap pic, cover;
                InputStream input;
                List<Bitmap> array = new ArrayList<>();

                input = new java.net.URL(url[0]).openStream();
                pic = BitmapFactory.decodeStream(input);
                array.add(pic);

                if(url[1] != null) {
                    input = new java.net.URL(url[1]).openStream();
                    cover = BitmapFactory.decodeStream(input);
                    array.add(cover);
                }

                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected final void onPostExecute(List<Bitmap> pics) {
            super.onPostExecute(pics);

            account.setPhoto(pics.get(0));
            if(pics.size() == 2)
                account.setBackground(pics.get(1));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyAccountDataChanged();
                }
            });
        }
    }

}


class Triplet<T, U, V>
{
    final T taskName;
    final U completed;
    final V dateDue;

    Triplet(T taskName, U completed, V dateDue)
    {
        this.taskName = taskName;
        this.completed = completed;
        this.dateDue = dateDue;
    }

    T getTaskName(){ return taskName;}
    U getCompleted(){ return completed;}
    V getDateDue(){ return dateDue;}
}
