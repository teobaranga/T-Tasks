package com.teo.ttasks;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Based on:
 * Sample activity for Google Tasks API v1. It demonstrates how to use authorization to list tasks
 * with the user's permission.
 *
 * @author Yaniv Inbar
 * @author Teo Baranga
 */

// TODO: fix SwipeRefreshLayout after switching fragments
// TODO: implement background color depending on priority
// TODO: implement multiple accounts

public final class MainActivity extends AppCompatActivity
        implements OnConnectionFailedListener, ConnectionCallbacks {

    static final String TAG = "MainActivity";
    //private static final int RC_ADD = 4;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;
    static final int REQUEST_AUTHORIZATION = 2;
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 3;
    private static final int PROFILE_SETTING = 1;
    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
    public SwipeRefreshLayout mSwipeRefreshLayout = null;
    public MyListCursorAdapter adapter;
    public Tasks service;
    public ConnectivityManager cm;
    public NetworkInfo ni;
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    /* A flag indicating that a PendingIntent is in progress and prevents
     * us from starting further intents.
     */
    private boolean mIntentInProgress;
    //save our header or result
    private AccountHeader accountHeader = null;
    private Drawer drawer = null;
    private IProfile profile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get internet status
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        ni = cm.getActiveNetworkInfo();

        Scope taskScope = new Scope(TasksScopes.TASKS);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addScope(taskScope)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        // Handle Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        profile = new ProfileDrawerItem()
                .withNameShown(true)
                .withName("Hey")
                .withEmail("")
                .withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_photo, null))
                .withIdentifier(0);

        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.ic_cover)
                .addProfiles(
                        profile,
                        new ProfileSettingDrawerItem().withName("Add Account").withDescription("Add a new Google account").withIcon(GoogleMaterial.Icon.gmd_add).withIdentifier(PROFILE_SETTING),
                        new ProfileSettingDrawerItem().withName("Manage Account").withIcon(GoogleMaterial.Icon.gmd_settings)
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        // ttasks usage of the onProfileChanged listener
                        // if the clicked item has the identifier 1 add a new profile
                        // TODO: Create new account
//                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_SETTING) {
//                        }

                        Log.d("iProfile", profile + "");
                        return true;
                        //false if you have not consumed the event and it should close the drawer
                        //return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        //Create the drawer
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader) //set the AccountHeader we created earlier for the header
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Tasks").withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_assignment_white_48dp, null)).withIdentifier(1).withCheckable(false),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withName("Settings").withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_settings_grey_48dp, null)).withIdentifier(2).withCheckable(false),
                        new SecondaryDrawerItem().withName("Help & Feedback").withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_help_grey_48dp, null)).withIdentifier(3).withCheckable(false),
                        new SecondaryDrawerItem().withName("About Tasks").withIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_info_outline_grey_48dp, null)).withIdentifier(4).withCheckable(false)
                ) // add the items we want to use with our Drawer
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        //check if the drawerItem is set.
                        //there are different reasons for the drawerItem to be null
                        //--> click on the header
                        //--> click on the footer
                        //those items don't contain a drawerItem

                        if (drawerItem != null) {
                            // Check that the activity is using the layout version with
                            // the fragment_container FrameLayout
                            if (findViewById(R.id.fragment_container) != null) {

                                // However, if we're being restored from a previous state,
                                // then we don't need to do anything and should return or else
                                // we could end up with overlapping fragments.
//                                if (savedInstanceState != null) {
//                                    return;
//                                }

                                // Create a new Fragment to be placed in the activity layout
                                TasksFragment tasksFragment = new TasksFragment();

                                // Add the fragment to the 'fragment_container' FrameLayout
                                getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.fragment_container, tasksFragment).commit();
                            }
                        }

                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        //only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the first item
            drawer.setSelectionByIdentifier(1, true);
            //set the active profile
            accountHeader.setActiveProfile(profile);
        }
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

    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed!");
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

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connection succeeded!");
        // We've resolved any connection errors.  mGoogleApiClient can be used to
        // access Google APIs on behalf of the user.
        if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            String personName = currentPerson.getDisplayName();
            String cover = null;
            if (currentPerson.hasCover())
                cover = currentPerson.getCover().getCoverPhoto().getUrl();
            // by default the profile url gives 50x50 px image only
            // we can replace the value with whatever dimension we want by
            // replacing sz=X
            String pic = currentPerson.getImage().getUrl();
            pic = pic.substring(0, pic.length() - 2) + 400;
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

            profile.setName(personName);
            profile.setEmail(email);
            accountHeader.updateProfileByIdentifier(profile);

            // get pictures only if connected to the internet
            if (ni != null) {
                new AsyncTask<String, Void, Drawable[]>() {
                    @Override
                    protected Drawable[] doInBackground(String... url) {
                        try {
                            Bitmap pic, cover;
                            Drawable[] pics = new Drawable[2];
                            InputStream input;

                            input = new java.net.URL(url[0]).openStream();
                            pic = BitmapFactory.decodeStream(input);
                            pics[0] = new BitmapDrawable(getResources(), pic);

                            if (url[1] != null) {
                                input = new java.net.URL(url[1]).openStream();
                                cover = BitmapFactory.decodeStream(input);
                                pics[1] = new BitmapDrawable(getResources(), cover);
                            }

                            return pics;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    protected final void onPostExecute(Drawable[] pics) {
                        super.onPostExecute(pics);

                        profile.setIcon(pics[0]);
                        accountHeader.updateProfileByIdentifier(profile);

                        if (pics[1] != null)
                            accountHeader.setBackground(pics[1]);
                    }
                }.execute(pic, cover);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
//            case RC_ADD:
//                if(resultCode == RESULT_OK) {
//                    MaterialAccount acc = new MaterialAccount(this.getResources(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),"", R.drawable.ic_photo, R.drawable.ic_cover);
//                    this.addAccount(acc);
//                    Toast.makeText(MainActivity.this, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), Toast.LENGTH_SHORT).show();
//                }
//                break;
            case RC_SIGN_IN:
                mIntentInProgress = false;

                if (!mGoogleApiClient.isConnecting()) {
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    // TODO: implement chooseAccount
//    private void chooseAccount() {
//        // disconnects the account and the account picker
//        // stays there until another account is chosen
//        if (mGoogleApiClient.isConnected()) {
//            // Prior to disconnecting, run clearDefaultAccount().
////            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
////            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
////                    .setResultCallback(new ResultCallback<Status>() {
////
////                        public void onResult(Status status) {
////                            // mGoogleApiClient is now disconnected and access has been revoked.
////                            // Trigger app logic to comply with the developer policies
////                        }
////
////                    });
//            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//            mGoogleApiClient.disconnect();
//            mGoogleApiClient.connect();
//        }
//    }

    // TODO: implement OnAccountAddComplete
//    private class OnAccountAddComplete implements AccountManagerCallback<Bundle> {
//        @Override
//        public void run(AccountManagerFuture<Bundle> result) {
//            Bundle bundle;
//            try {
//                bundle = result.getResult();
//            } catch (OperationCanceledException e) {
//                e.printStackTrace();
//                return;
//            } catch (AuthenticatorException e) {
//                e.printStackTrace();
//                return;
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
////            MaterialAccount acc = new MaterialAccount(MainActivity.this.getResources(), "",
////                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME), R.drawable.ic_photo, R.drawable.ic_cover);
////            MainActivity.this.addAccount(acc);
//
////            mAccount = new Account(
////                    bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
////                    bundle.getString(AccountManager.KEY_ACCOUNT_TYPE)
////            );
//            // do more stuff
//        }
//    }

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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = accountHeader.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}