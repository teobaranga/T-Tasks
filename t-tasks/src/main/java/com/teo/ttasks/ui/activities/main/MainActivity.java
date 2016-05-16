package com.teo.ttasks.ui.activities.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
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
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.local.PrefHelper;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.AboutActivity;
import com.teo.ttasks.ui.activities.BaseActivity;
import com.teo.ttasks.ui.activities.SignInActivity;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

// TODO: 2015-12-29 implement multiple accounts
public final class MainActivity extends BaseActivity implements MainActivityView, AdapterView.OnItemSelectedListener, OnConnectionFailedListener {

    //private static final int RC_ADD = 4;

    private static final int ID_TASKS = 0x10;
    private static final int ID_TASK_LISTS = 0x20;
    private static final int ID_ADD_ACCOUNT = 0x01;
    private static final int ID_MANAGE_ACCOUNT = 0x02;
    private static final int ID_ABOUT = 0xFF;

    // Request code to use when launching the resolution activity
    private static final int RC_RESOLVE_ERROR = 1001;

    /** Unique tag for the error dialog fragment */
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    /** The spinner displaying the user's task lists in the toolbar **/
    @BindView(R.id.spinner_task_lists) Spinner mTaskLists;

    @Inject PrefHelper mPrefHelper;
    @Inject MainActivityPresenter mMainActivityPresenter;

    private TaskListsAdapter mTaskListsAdapter;

    /** The profile of the currently logged in user */
    private ProfileDrawerItem mProfile = null;

    /** Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    private AccountHeader accountHeader = null;
    private Drawer drawer = null;

    /** Bool to track whether the app is already resolving an error */
    private boolean mResolvingError = false;

    private NetworkInfoReceiver mNetworkInfoReceiver;
    private TasksFragment tasksFragment;
    private Permiso mPermiso;

    public static void start(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(this).applicationComponent().inject(this);

        // Show the SignIn activity if there's no user connected
        if (!mPrefHelper.isUserPresent()) {
            SignInActivity.start(this);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mPermiso = Permiso.getInstance();
        mPermiso.setActivity(this);
        mPermiso.requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted())
                    TTasksApp.get(MainActivity.this).tasksComponent().inject(mMainActivityPresenter);
                else {
                    // User revoked permissions, return to Sign In screen
                    SignInActivity.start(MainActivity.this);
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                // TODO: 2016-05-01 create rationale text
                mPermiso.showRationaleInDialog("Title", "Message", null, callback);
            }
        }, Manifest.permission.GET_ACCOUNTS);

        mMainActivityPresenter.bindView(this);

        mTaskListsAdapter = new TaskListsAdapter(getSupportActionBar().getThemedContext());
        mTaskListsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTaskLists.setAdapter(mTaskListsAdapter);
        mTaskLists.setOnItemSelectedListener(this);

        // Create the network info receiver
        mNetworkInfoReceiver = new NetworkInfoReceiver(this, isOnline -> {
            // Internet connection changed
            // TODO: 2015-12-29 Display/Hide info
            Toast.makeText(MainActivity.this, isOnline ? "Online" : "Offline", Toast.LENGTH_SHORT).show();
        });

        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addScope(Plus.SCOPE_PLUS_PROFILE)
                .addApi(Plus.API)
                .build();

        mProfile = new ProfileDrawerItem()
                .withIcon(mPrefHelper.getUserPhoto())
                .withName(mPrefHelper.getUserName())
                .withEmail(mPrefHelper.getUserEmail())
                .withNameShown(true);

        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .withCurrentProfileHiddenInList(true)
                .addProfiles(
                        mProfile,
                        new ProfileSettingDrawerItem()
                                .withName(getResources().getString(R.string.drawer_add_account))
                                .withIcon(GoogleMaterial.Icon.gmd_account_add)
                                .withIdentifier(ID_ADD_ACCOUNT),
                        new ProfileSettingDrawerItem()
                                .withName(getResources().getString(R.string.drawer_manage_account))
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
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName("Tasks")
                                .withIdentifier(ID_TASKS),
                        new PrimaryDrawerItem()
                                .withName("Task Lists")
                                .withIdentifier(ID_TASK_LISTS),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem()
                                .withName(getResources().getString(R.string.drawer_settings))
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_help_and_feedback)
                                .withIcon(GoogleMaterial.Icon.gmd_help)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_about)
                                .withIcon(GoogleMaterial.Icon.gmd_info_outline)
                                .withIdentifier(ID_ABOUT)
                                .withSelectable(false)
                )
                .withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    // The header and footer items don't contain a drawerItem
                    if (drawerItem != null) {
                        switch ((int) drawerItem.getIdentifier()) {
                            case ID_TASKS:
                                getSupportActionBar().setDisplayShowTitleEnabled(false);
                                mTaskLists.setVisibility(VISIBLE);
                                break;
                            case ID_TASK_LISTS:
                                getSupportActionBar().setDisplayShowTitleEnabled(true);
                                mTaskLists.setVisibility(GONE);
                                break;
                            case ID_ABOUT:
                                startActivity(new Intent(this, AboutActivity.class));
                                break;
                            default:
                                // If we're being restored from a previous state,
                                // then we don't need to do anything and should return or else
                                // we could end up with overlapping fragments.
                                if (savedInstanceState != null)
                                    break;

                                // Create a new Fragment to be placed in the activity layout
                                // Avoid recreating the same fragment
                                if (tasksFragment != null && tasksFragment.getTaskListId().equals(drawerItem.getTag()))
                                    break;
                        }
                    }
                    return false;
                })
                .withSavedInstance(savedInstanceState)
                .build();

        // Only set the active selection or active profile if we do not recreate the activity
        if (savedInstanceState == null) {
            // set the selection to the first item
            drawer.setSelectionAtPosition(1);
            //set the active profile
            accountHeader.setActiveProfile(mProfile);
        }

        mMainActivityPresenter.loadCurrentUser(mGoogleApiClient);
        mMainActivityPresenter.getTaskLists(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mNetworkInfoReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mMainActivityPresenter.getTaskLists(true);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mNetworkInfoReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMainActivityPresenter != null)
            mMainActivityPresenter.unbindView();
    }

    /**
     * Load the profile picture into the current profile
     * and display it in the Navigation drawer header
     */
    @Override
    public void onUserPicture(@NonNull String pictureUrl) {
        mProfile.withIcon(pictureUrl);
        accountHeader.updateProfile(mProfile);
    }

    @Override
    public void onUserCover(@NonNull String coverUrl) {
        Picasso.with(this).load(coverUrl).into(((Target) accountHeader.getHeaderBackgroundView().getTag()));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        com.teo.ttasks.data.model.TaskList taskList = ((com.teo.ttasks.data.model.TaskList) parent.getItemAtPosition(position));
        mPrefHelper.updateCurrentTaskList(taskList.getId());
        tasksFragment = TasksFragment.newInstance(taskList.getId());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, tasksFragment)
                .commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    /**
     * Load the task lists and prepare them to be displayed
     * Select the last accessed task list
     */
    @Override
    public void onTaskListsLoaded(List<TaskList> taskLists) {
        mTaskListsAdapter.clear();
        mTaskListsAdapter.addAll(taskLists);

        // Restore previously selected task list
        String currentTaskListId = mPrefHelper.getCurrentTaskListId();
        for (int i = 0, size = mTaskListsAdapter.getCount(); i < size; i++) {
            com.teo.ttasks.data.model.TaskList taskList = mTaskListsAdapter.getItem(i);
            if (taskList.getId().equals(currentTaskListId)) {
                mTaskLists.setSelection(i);
                break;
            }
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        // add the values which need to be saved from the accountHeader to the bundle
        outState = accountHeader.saveInstanceState(outState);
        // Keep track of the mResolvingError boolean across activity restarts
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) drawer.closeDrawer();
        else super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermiso.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            Dialog dialog = googleAPI.getErrorDialog(this, connectionStatusCode, RC_RESOLVE_ERROR);
            dialog.setOnDismissListener(dialogInterface -> mResolvingError = false);
            dialog.show();
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
}