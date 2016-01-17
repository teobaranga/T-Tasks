package com.teo.ttasks.ui.activities.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.View;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.TasksScopes;
import com.google.api.services.tasks.model.TaskList;
import com.greysonparrelli.permiso.Permiso;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.AboutActivity;
import com.teo.ttasks.ui.activities.SignInActivity;
import com.teo.ttasks.ui.base.BaseActivity;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import timber.log.Timber;

// TODO: 2015-12-29 implement logic for inserting, updating and removing tasks
// TODO: 2015-12-29 implement multiple accounts
public final class MainActivity extends BaseActivity implements OnConnectionFailedListener {

    //private static final int RC_ADD = 4;

    private static final int ID_ADD_ACCOUNT = 1000;
    private static final int ID_MANAGE_ACCOUNT = 1001;
    private static final int ID_ADD_TASK_LIST = 2000;
    private static final int ID_ABOUT = 2;

    // Request code to use when launching the resolution activity
    private static final int RC_RESOLVE_ERROR = 1001;

    /** Unique tag for the error dialog fragment */
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    @Inject MainActivityPresenter mMainActivityPresenter;

    /** The profile of the currently logged in user */
    private ProfileDrawerItem mProfile = null;

    /** Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private AccountHeader accountHeader = null;
    private Drawer drawer = null;

    /** Bool to track whether the app is already resolving an error */
    private boolean mResolvingError = false;
    private ArrayList<IDrawerItem> taskLists = new ArrayList<>();
    private NetworkInfoReceiver mNetworkInfoReceiver;
    private TasksFragment tasksFragment;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TTasksApp.get(this).applicationComponent().plus(new MainActivityModule(this)).inject(this);
        Permiso.getInstance().setActivity(this);

        // Initialize Realm
        Realm.setDefaultConfiguration(new RealmConfiguration.Builder(this).build());
        realm = Realm.getDefaultInstance();

        // Create the network info receiver
        mNetworkInfoReceiver = new NetworkInfoReceiver(this) {
            @Override
            public void onReceive(@NotNull Context context, @NotNull Intent intent) {
                // Internet connection changed
                // TODO: 2015-12-29 Display/Hide message
            }
        };

        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

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
                .addApi(Plus.API)
                .build();

        mProfile = new ProfileDrawerItem()
                .withNameShown(true);

        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.ic_cover)
                .withCurrentProfileHiddenInList(true)
                .addProfiles(
                        mProfile,
                        new ProfileSettingDrawerItem()
                                .withName(getResources().getString(R.string.addaccount))
                                .withIcon(GoogleMaterial.Icon.gmd_person_add)
                                .withIdentifier(ID_ADD_ACCOUNT),
                        new ProfileSettingDrawerItem()
                                .withName(getResources().getString(R.string.manageaccount))
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withIdentifier(ID_MANAGE_ACCOUNT)
                )
                .withOnAccountHeaderListener((View view, IProfile profile, boolean current) -> {
                    Timber.d(profile.toString());
                    return true;
                })
                .withSavedInstance(savedInstanceState)
                .build();

        accountHeader.getHeaderBackgroundView().setTag(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                accountHeader.setBackground(new BitmapDrawable(getResources(), bitmap));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Timber.e("Error fetching cover pic");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });

        // Create the drawer
        //noinspection ConstantConditions
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar())
                .withAccountHeader(accountHeader)
                .withDrawerItems(taskLists)
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.drawer_addtasklist)
                                .withIcon(GoogleMaterial.Icon.gmd_add)
                                .withIdentifier(ID_ADD_TASK_LIST)
                                .withSelectable(false),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem()
                                .withName(getResources().getString(R.string.settings))
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.helpandfeedback)
                                .withIcon(GoogleMaterial.Icon.gmd_help)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.about)
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withIdentifier(ID_ABOUT)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener((View view, int position, IDrawerItem drawerItem) -> {
                    // The header and footer items don't contain a drawerItem
                    if (drawerItem != null) {
                        switch (drawerItem.getIdentifier()) {
                            case ID_ADD_TASK_LIST:
                                break;
                            case ID_ABOUT:
                                startActivity(new Intent(this, AboutActivity.class));
                                break;
                            default:
                                if (drawerItem.getTag() != null) {
                                    // Must be a task list
                                    // However, if we're being restored from a previous state,
                                    // then we don't need to do anything and should return or else
                                    // we could end up with overlapping fragments.
                                    if (savedInstanceState != null)
                                        break;

                                    // Create a new Fragment to be placed in the activity layout
                                    // Avoid recreating the same fragment
                                    if (tasksFragment != null && tasksFragment.getTaskListId().equals(drawerItem.getTag()))
                                        break;
                                    tasksFragment = TasksFragment.newInstance((String) drawerItem.getTag(), ((PrimaryDrawerItem) drawerItem).getName().getText());
                                    getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, tasksFragment)
                                            .commit();
                                }
                        }
                    }
                    return false;
                })
                .withSavedInstance(savedInstanceState)
                .build();

        // Only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the first item
            drawer.setSelectionAtPosition(-1);
            //set the active profile
            accountHeader.setActiveProfile(mProfile);
        }

        mMainActivityPresenter.loadTaskLists();
    }

    public void onCachedTaskListsLoaded(RealmResults<com.teo.ttasks.data.model.TaskList> taskLists) {
        runOnUiThreadIfAlive(() -> {
            Timber.d("Found %d offline task lists", taskLists.size());
            for (com.teo.ttasks.data.model.TaskList taskList : taskLists)
                addOrUpdateTaskList(taskList);
//            drawer.setSelectionAtPosition(1);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Timber.d("Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
//            showProgressDialog();
            opr.setResultCallback(googleSignInResult -> {
//                    hideProgressDialog();
                handleSignInResult(googleSignInResult);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mNetworkInfoReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkInfoReceiver);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }


    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            String email = acct.getEmail();

            // Set name
            mProfile.withName(acct.getDisplayName())
                    .withIcon(acct.getPhotoUrl())
                    .withEmail(email);
            accountHeader.updateProfile(mProfile);

            mMainActivityPresenter.loadCurrentUser(mGoogleApiClient);

            Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
                @Override
                public void onPermissionResult(Permiso.ResultSet resultSet) {
                    if (resultSet.areAllPermissionsGranted()) {
                        // Initialize credentials and service object.
                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(MainActivity.this, Collections.singleton(TasksScopes.TASKS))
                                .setBackOff(new ExponentialBackOff())
                                .setSelectedAccountName(email);

                        TTasksApp.get(MainActivity.this).createTasksApiComponent(credential).inject(mMainActivityPresenter);

                        mMainActivityPresenter.reloadTaskLists();
                    }
                }

                @Override
                public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                    Permiso.getInstance().showRationaleInDialog("Title", "Message", null, callback);
                }
            }, Manifest.permission.GET_ACCOUNTS);

        } else {
            // Signed out, show unauthenticated UI.
            TTasksApp.get(this).releaseTasksApiComponent();
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
    }

    /**
     * Called when all the information about the currently signed in user
     * has been retrieved
     */
    public void onUserLoaded(@NonNull Person currentPerson) {
        runOnUiThreadIfAlive(() -> {
            // Set profile picture
            // by default the profile url gives 50x50 px image only
            // we can replace the value with whatever dimension we want by
            // replacing sz=X
            String pic = currentPerson.getImage().getUrl();
            // Requesting a size of 400x400
            Timber.d(pic);
            pic = pic.substring(0, pic.length() - 2) + "400";
            mProfile.withIcon(pic);
            accountHeader.updateProfile(mProfile);

            // Set cover picture
            if (currentPerson.hasCover()) {
                String cover = currentPerson.getCover().getCoverPhoto().getUrl();
                if (cover != null) {
                    Timber.d("Loading cover picture");
                    Picasso.with(this).load(cover).into(((Target) accountHeader.getHeaderBackgroundView().getTag()));
                }
            }
        });
    }

    /**
     * Called once all the tasks lists have been fetched
     */
    public void onTaskListsLoaded(@NonNull List<TaskList> taskLists) {
        Timber.d("Found %s task lists", taskLists.size());
        // Insert task lists into Navigation Drawer
        realm.executeTransaction((Realm realm) -> {
            for (TaskList taskList : taskLists)
                realm.createOrUpdateObjectFromJson(com.teo.ttasks.data.model.TaskList.class, taskList.toString());
        }, new Realm.Transaction.Callback() {
            @Override
            public void onSuccess() {
                Timber.d("Saved all task lists to Realm");
                List<com.teo.ttasks.data.model.TaskList> taskListList = realm.allObjects(com.teo.ttasks.data.model.TaskList.class);
                if (taskListList.size() >= 1) {
                    for (com.teo.ttasks.data.model.TaskList taskList : taskListList)
                        addOrUpdateTaskList(taskList);
                    if (drawer.getCurrentSelectedPosition() == -1)
                        drawer.setSelectionAtPosition(1);
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e.toString());
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        Timber.w("Connection failed!");
        if (!mResolvingError) {
            // Not already attempting to resolve an error.
            if (result.hasResolution()) {
                try {
                    mResolvingError = true;
                    result.startResolutionForResult(this, RC_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }

            } else {
                showGooglePlayServicesAvailabilityErrorDialog(result.getErrorCode());
                mResolvingError = true;
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
            case RC_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode == RESULT_OK) {
                    if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.connect();
                    }
                }
                break;
        }
    }

    // TODO: implement chooseAccount
//    private void chooseAccount() {
//        // disconnects the account and the account picker
//        // stays there until another account is chosen
//        if (mGoogleApiClient.isOnline()) {
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = accountHeader.saveInstanceState(outState);
        // Keep track of the mResolvingError boolean across activity restarts
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen())
            drawer.closeDrawer();
        else
            super.onBackPressed();
    }

    /**
     * Add a task list to the drawer
     * TODO: implement some kind of sorting
     */
    private void addOrUpdateTaskList(com.teo.ttasks.data.model.TaskList taskList) {
        // Update task list name if it already exists
        for (IDrawerItem drawerItem : drawer.getDrawerItems())
            if (drawerItem.getTag() != null && drawerItem.getTag().equals(taskList.getId())) {
                if (!((PrimaryDrawerItem) drawerItem).getName().getText().equals(taskList.getTitle())) {
                    ((PrimaryDrawerItem) drawerItem).withName(taskList.getTitle());
                    drawer.updateItem(drawerItem);
                }
                return;
            }
        // Add task list
        drawer.addItemAtPosition(new PrimaryDrawerItem()
                .withName(taskList.getTitle())
                .withTag(taskList.getId())
                .withIcon(GoogleMaterial.Icon.gmd_assignment), 1);
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, this, RC_RESOLVE_ERROR);
        dialog.setOnDismissListener((DialogInterface dialogInterface) -> mResolvingError = false);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }
}