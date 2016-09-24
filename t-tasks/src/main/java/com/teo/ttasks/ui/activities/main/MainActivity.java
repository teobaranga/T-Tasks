package com.teo.ttasks.ui.activities.main;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.databinding.ActivityMainBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.AboutActivity;
import com.teo.ttasks.ui.activities.BaseActivity;
import com.teo.ttasks.ui.activities.SettingsActivity;
import com.teo.ttasks.ui.activities.sign_in.SignInActivity;
import com.teo.ttasks.ui.fragments.task_lists.TaskListsFragment;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

// TODO: 2015-12-29 implement multiple accounts
public final class MainActivity extends BaseActivity implements MainView {

    //private static final int RC_ADD = 4;
    private static final int RC_NIGHT_MODE = 415;

    private static final int ID_TASKS = 0x01;
    private static final int ID_TASK_LISTS = 0x02;
    //    private static final int ID_ADD_ACCOUNT = 0x01;
//    private static final int ID_MANAGE_ACCOUNT = 0x02;
    private static final int ID_SETTINGS = 0xF0;
    private static final int ID_HELP_AND_FEEDBACK = 0xF1;
    private static final int ID_ABOUT = 0xF2;

    @Inject MainActivityPresenter mainActivityPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    ProfileDrawerItem profile = null;
    AccountHeader accountHeader = null;
    TasksFragment tasksFragment;
    TaskListsFragment taskListsFragment;

    private ActivityMainBinding mainBinding;

    private TaskListsAdapter taskListsAdapter;

    /** The profile of the currently logged in user */
    private Drawer drawer = null;

    private boolean recreate;

    public static void start(Context context) {
        Intent starter = new Intent(context, MainActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(this).userComponent().inject(this);
        mainActivityPresenter.bindView(this);

        // Show the SignIn activity if there's no user connected
        if (!mainActivityPresenter.isUserPresent()) {
            SignInActivity.start(this);
            finish();
            return;
        }

        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (savedInstanceState == null) {
            // Create the tasks fragment
            tasksFragment = TasksFragment.newInstance();
            taskListsFragment = TaskListsFragment.newInstance();
        } else {
            // Get the tasks fragment
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("tasks");
            if (fragment instanceof TasksFragment)
                tasksFragment = (TasksFragment) fragment;
            else
                tasksFragment = TasksFragment.newInstance();
        }

        //noinspection ConstantConditions
        taskListsAdapter = new TaskListsAdapter(getSupportActionBar().getThemedContext());
        mainBinding.spinnerTaskLists.setAdapter(taskListsAdapter);
        mainBinding.spinnerTaskLists.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                final TTaskList taskList = ((TTaskList) adapterView.getItemAtPosition(position));
                final String taskListId = taskList.getId();
                mainActivityPresenter.setLastAccessedTaskList(taskListId);
                tasksFragment.setTaskList(taskListId);
            }

            @Override public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        profile = new ProfileDrawerItem()
                .withName(mainActivityPresenter.getUserName())
                .withEmail(mainActivityPresenter.getUserEmail())
                .withNameShown(true)
                .withTag(new ProfileIconTarget());

        // Create the AccountHeader
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .withCurrentProfileHiddenInList(true)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        profile
//                        new ProfileSettingDrawerItem()
//                                .withName(getResources().getString(R.string.drawer_add_account))
//                                .withIcon(GoogleMaterial.Icon.gmd_account_add)
//                                .withIdentifier(ID_ADD_ACCOUNT),
//                        new ProfileSettingDrawerItem()
//                                .withName(getResources().getString(R.string.drawer_manage_account))
//                                .withIcon(GoogleMaterial.Icon.gmd_settings)
//                                .withIdentifier(ID_MANAGE_ACCOUNT)
                )
                .withOnAccountHeaderListener((View view, IProfile profile, boolean current) -> {
                    Timber.d(profile.toString());
                    return true;
                })
                .withSavedInstance(savedInstanceState)
                .build();

        accountHeader.getHeaderBackgroundView().setTag(new CoverPhotoTarget());

        // Create the drawer
        //noinspection ConstantConditions
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar())
                .withAccountHeader(accountHeader)
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.tasks)
                                .withIcon(R.drawable.ic_tasks_24dp)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_TASKS),
                        new PrimaryDrawerItem()
                                .withName(R.string.task_lists)
                                .withIcon(R.drawable.ic_task_lists_24dp)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_TASK_LISTS),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem()
                                .withName(getResources().getString(R.string.drawer_settings))
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withIdentifier(ID_SETTINGS)
                                .withSelectable(false),
                        new SecondaryDrawerItem()
                                .withName(R.string.drawer_help_and_feedback)
                                .withIcon(GoogleMaterial.Icon.gmd_help)
                                .withIdentifier(ID_HELP_AND_FEEDBACK)
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
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        Fragment fragment = null;
                        String tag = null;
                        final ActionBar supportActionBar = getSupportActionBar();
                        switch ((int) drawerItem.getIdentifier()) {
                            case ID_TASKS:
                                if (currentFragment instanceof TasksFragment)
                                    return false;
                                supportActionBar.setDisplayShowTitleEnabled(false);
                                mainBinding.spinnerTaskLists.setVisibility(VISIBLE);
                                fragment = tasksFragment;
                                tag = "tasks";
                                break;
                            case ID_TASK_LISTS:
                                if (currentFragment instanceof TaskListsFragment)
                                    return false;
                                supportActionBar.setTitle(R.string.task_lists);
                                supportActionBar.setDisplayShowTitleEnabled(true);
                                mainBinding.spinnerTaskLists.setVisibility(GONE);
                                fragment = TaskListsFragment.newInstance();
                                tag = "taskLists";
                                break;
                            case ID_SETTINGS:
                                SettingsActivity.startForResult(this, RC_NIGHT_MODE);
                                break;
                            case ID_ABOUT:
                                AboutActivity.start(this);
                                break;
                            default:
                                // If we're being restored from a previous state,
                                // then we don't need to do anything and should return or else
                                // we could end up with overlapping fragments.
                                if (savedInstanceState != null)
                                    break;
                        }
                        Timber.d("fragment is %s", fragment);
                        if (fragment != null) {
                            mainBinding.appbar.setExpanded(true);
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, fragment, tag)
                                    .commitAllowingStateLoss();
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
            accountHeader.setActiveProfile(profile);
        } else {
            // Restore state
            switch ((int) drawer.getCurrentSelection()) {
                case ID_TASKS:
                    //noinspection ConstantConditions
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                    mainBinding.spinnerTaskLists.setVisibility(VISIBLE);
                    break;
                case ID_TASK_LISTS:
                    // Restore visibility of the task lists spinner & toolbar title
                    mainBinding.spinnerTaskLists.setVisibility(GONE);
                    //noinspection ConstantConditions
                    getSupportActionBar().setTitle(R.string.task_lists);
                    getSupportActionBar().setDisplayShowTitleEnabled(true);
                    break;
            }
        }

        mainActivityPresenter.getTaskLists();
        mainActivityPresenter.loadUserPictures();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (recreate) {
            // Recreate the activity after a delay that is equal to the drawer close delay
            // otherwise, the drawer will open unexpectedly after a night mode change
            new Handler().postDelayed(this::recreate, 50);
            recreate = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainActivityPresenter.unbindView(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = drawer.saveInstanceState(outState);
        // add the values which need to be saved from the accountHeader to the bundle
        outState = accountHeader.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_NIGHT_MODE:
                if (resultCode == RESULT_OK)
                    recreate = true;
                break;
//            case RC_ADD:
//                if(resultCode == RESULT_OK) {
//                    MaterialAccount acc = new MaterialAccount(this.getResources(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),"", R.drawable.ic_photo, R.drawable.ic_cover);
//                    this.addAccount(acc);
//                    Toast.makeText(MainActivity.this, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), Toast.LENGTH_SHORT).show();
//                }
//                break;
        }
    }

    @Override
    protected void onApiReady() {
        if (networkInfoReceiver.isOnline(this)) {
            mainActivityPresenter.refreshTaskLists();
            mainActivityPresenter.loadCurrentUser();
        }
    }

    /**
     * Load the profile picture into the current profile
     * and display it in the Navigation drawer header
     */
    @Override
    public void onUserPicture(@NonNull String pictureUrl) {
        Picasso.with(this)
                .load(pictureUrl)
                .placeholder(DrawerUIUtils.getPlaceHolder(this))
                .into(((Target) profile.getTag()));
    }

    @Override
    public void onUserCover(@NonNull String coverUrl) {
        Picasso.with(this)
                .load(coverUrl)
                .placeholder(new IconicsDrawable(this).iconText(" ").backgroundColorRes(com.mikepenz.materialdrawer.R.color.primary).sizeDp(56))
                .into(((Target) accountHeader.getHeaderBackgroundView().getTag()));
    }

    /**
     * Load the task lists and prepare them to be displayed
     * Select the last accessed task list
     */
    @Override
    public void onTaskListsLoaded(List<TTaskList> taskLists, int currentTaskListIndex) {
        taskListsAdapter.clear();
        taskListsAdapter.addAll(taskLists);
        // Restore previously selected task list
        mainBinding.spinnerTaskLists.setSelection(currentTaskListIndex);
    }

    @Override
    public void onTaskListsLoadError() {
        // TODO: 2016-07-24 implement
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) drawer.closeDrawer();
        else super.onBackPressed();
    }

    /**
     * Enable the scrolling system, which moves the toolbar and the FAB
     * out of the way when scrolling down and brings them back when scrolling up.
     */
    public void enableScrolling() {
        final AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) mainBinding.toolbar.getLayoutParams();
        final int flags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS;
        if (layoutParams.getScrollFlags() != flags) {
            layoutParams.setScrollFlags(flags);
            mainBinding.toolbar.setLayoutParams(layoutParams);
        }
    }

    /**
     * Disable the whole scrolling system, which pins the toolbar and the FAB in place.
     * This is used when the content of the fragment is not big enough to require scrolling,
     * such is the case when a short list or an empty view is displayed.
     */
    public void disableScrolling(boolean delay) {
        // Delay the behavior change by 300 milliseconds to give time to the FAB to restore its default position
        mainBinding.fab.postDelayed(() -> {
            final AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) mainBinding.toolbar.getLayoutParams();
            if (layoutParams.getScrollFlags() != 0) {
                layoutParams.setScrollFlags(0);
                mainBinding.toolbar.setLayoutParams(layoutParams);
            }
        }, delay ? 300 : 0);
    }

    public FloatingActionButton fab() {
        return mainBinding.fab;
    }

    class ProfileIconTarget implements Target {
        @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Bitmap imageWithBG = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());  // Create another image the same size
            imageWithBG.eraseColor(Color.WHITE);  // set its background to white, or whatever color you want
            Canvas canvas = new Canvas(imageWithBG);  // create a canvas to draw on the new image
            canvas.drawBitmap(bitmap, 0f, 0f, null); // draw old image on the background
            profile.withIcon(imageWithBG);
            accountHeader.updateProfile(profile);
        }

        @Override public void onBitmapFailed(Drawable errorDrawable) { }

        @Override public void onPrepareLoad(Drawable placeHolderDrawable) { }
    }

    class CoverPhotoTarget implements Target {
        @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            accountHeader.setBackground(new BitmapDrawable(getResources(), bitmap));
        }

        @Override public void onBitmapFailed(Drawable errorDrawable) {
            Timber.e("Error fetching cover pic");
        }

        @Override public void onPrepareLoad(Drawable placeHolderDrawable) { }
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
