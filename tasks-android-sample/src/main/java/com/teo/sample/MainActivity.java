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

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.TasksScopes;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.neokree.materialnavigationdrawer.MaterialAccount;
import it.neokree.materialnavigationdrawer.MaterialAccountListener;
import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.MaterialSection;
import it.neokree.materialnavigationdrawer.MaterialSectionListener;

/**
 * Sample activity for Google Tasks API v1. It demonstrates how to use authorization to list tasks
 * with the user's permission.
 * 
 * @author Yaniv Inbar
 */


public final class MainActivity extends MaterialNavigationDrawer implements MaterialAccountListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    /**
    * Logging level for HTTP requests/responses.
    *
    * To turn on, set to {@link Level#CONFIG} or {@link Level#ALL} and run this from command line:
    *
    * adb shell setprop log.tag.HttpTransport DEBUG
    * </pre>
    */

    MaterialAccount account;
    MaterialSection section1, section2, recorder, night, last, settingsSection;

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 3;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;

    private static final Level LOGGING_LEVEL = Level.OFF;

    private static final String PREF_ACCOUNT_NAME = "accountName";

    static final String TAG = "MainActivity";

    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    static final int REQUEST_AUTHORIZATION = 1;
    static final int REQUEST_ACCOUNT_PICKER = 2;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    GoogleAccountCredential credential;

    List<String> tasksList;

    ArrayAdapter<String> adapter;

    com.google.api.services.tasks.Tasks service;

    int numAsyncTasks;

    FragmentIndex frag;

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
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tasksList);
        frag.setListAdapter(adapter);
    }

    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkGooglePlayServicesAvailable()) {
            haveGooglePlayServices();
        }
    }

    protected void onStop() {
        super.onStop();

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
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    AsyncLoadTasks.run(this);
                } else {
                    chooseAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        AsyncLoadTasks.run(this);
                    }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                AsyncLoadTasks.run(this);
                break;
            case R.id.menu_accounts:
                chooseAccount();
                return true;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential.getSelectedAccountName() == null) {
            // ask user to choose account
            chooseAccount();
        } else {
            // load calendars
            AsyncLoadTasks.run(this);
        }
    }

    private void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    public void init(Bundle savedInstanceState) {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .build();

        // add placeholder account
        account = new MaterialAccount("","",new ColorDrawable(R.color.cyan_500), this.getResources().getDrawable(R.drawable.bamboo));
        this.addAccount(account);

        // set listener
        this.setAccountListener(this);

        //this.replaceDrawerHeader(this.getResources().getDrawable(R.drawable.mat2));

        // create sections
        frag = new FragmentIndex();
        section1 = this.newSection("Section 1", frag);
        section2 = this.newSection("Section 2",new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                Toast.makeText(MainActivity.this, "Section 2 Clicked", Toast.LENGTH_SHORT).show();

                section.unSelect();
            }
        });
        // recorder section with icon and 10 notifications
        recorder = this.newSection("Recorder",this.getResources().getDrawable(R.drawable.ic_launcher), frag).setNotifications(10);
        // night section with icon, section color and notifications
        night = this.newSection("Night Section", this.getResources().getDrawable(R.drawable.ic_launcher), frag)
                .setSectionColor(Color.parseColor("#2196f3"),Color.parseColor("#1565c0")).setNotifications(150);
        // night section with section color
        last = this.newSection("Last Section", frag).setSectionColor(Color.parseColor("#ff9800"),Color.parseColor("#ef6c00"));

        Intent i = new Intent(this,SettingsActivity.class);
        settingsSection = this.newSection("Settings",this.getResources().getDrawable(R.drawable.ic_launcher),i);

        // add your sections to the drawer
        this.addSection(section1);
        this.addSection(section2);
        this.addSubheader("Subheader");
        this.addSection(recorder);
        this.addSection(night);
        this.addDivisor();
        this.addSection(last);
        this.addBottomSection(settingsSection);
        this.disableLearningPattern();

        this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_CUSTOM);

        // enable logging
        Logger.getLogger("com.google.api.client").setLevel(LOGGING_LEVEL);
        // view and menu
        //setContentView(R.layout.calendarlist);
        //listView = (ListView) findViewById(R.id.list);
        // Google Accounts
        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(TasksScopes.TASKS));
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        // Tasks client
        service = new com.google.api.services.tasks.Tasks.Builder(httpTransport, jsonFactory, credential)
                        .setApplicationName("Google-TasksAndroidSample/1.0").build();

        // start thread
        //t.start();

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
            String cover = currentPerson.getCover().getCoverPhoto().getUrl();
            String pic = currentPerson.getImage().getUrl();
            String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

            account.setTitle(personName);
            account.setSubTitle(email);

            new setPics().execute(cover, pic);

        }
    }

    @Override
    public void onAccountOpening(MaterialAccount account) {
        // open profile activity
        Intent i = new Intent(this,SettingsActivity.class);
        startActivity(i);
    }

    @Override
    public void onChangeAccount(MaterialAccount newAccount) {
        // when another account is selected
    }

    class setPics extends AsyncTask<String, Void, List<Drawable>> {

        protected List<Drawable> doInBackground(String... url) {
            try {
                Bitmap cover, pic;

                HttpURLConnection connection = (HttpURLConnection) new URL(url[0]).openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                cover = BitmapFactory.decodeStream(input);
                connection.disconnect();

                connection = (HttpURLConnection) new URL(url[1]).openConnection();
                connection.connect();
                input = connection.getInputStream();
                pic = BitmapFactory.decodeStream(input);
                connection.disconnect();

                Drawable c = new BitmapDrawable(getApplicationContext().getResources(), cover);
                Drawable p = new BitmapDrawable(getApplicationContext().getResources(), pic);

                List<Drawable> array = new ArrayList<Drawable>();
                array.add(c);
                array.add(p);

                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected final void onPostExecute(List<Drawable> pic) {
            super.onPostExecute(pic);

            account.setBackground(pic.get(0));
            account.setPhoto(pic.get(1));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyAccountDataChanged();
                }
            });
        }
    }

}
