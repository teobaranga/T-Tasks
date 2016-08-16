package com.teo.ttasks.ui.activities.main;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
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
import com.mikepenz.materialdrawer.model.ProfileSettingDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.data.TaskListsAdapter;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ActivityMainBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.receivers.TaskNotificationReceiver;
import com.teo.ttasks.ui.activities.AboutActivity;
import com.teo.ttasks.ui.activities.BaseActivity;
import com.teo.ttasks.ui.activities.sign_in.SignInActivity;
import com.teo.ttasks.ui.fragments.tasks.TasksFragment;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

// TODO: 2015-12-29 implement multiple accounts
public final class MainActivity extends BaseActivity implements MainView {

    //private static final int RC_ADD = 4;

    private static final int ID_TASKS = 0x10;
    private static final int ID_TASK_LISTS = 0x20;
    private static final int ID_ADD_ACCOUNT = 0x01;
    private static final int ID_MANAGE_ACCOUNT = 0x02;
    private static final int ID_ABOUT = 0xFF;

    @Inject MainActivityPresenter mainActivityPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    private ActivityMainBinding mainBinding;

    private TaskListsAdapter taskListsAdapter;

    /** The profile of the currently logged in user */
    private ProfileDrawerItem profile = null;
    private Drawer drawer = null;
    private AccountHeader accountHeader = null;
    private TasksFragment tasksFragment;

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

        //noinspection ConstantConditions
        taskListsAdapter = new TaskListsAdapter(getSupportActionBar().getThemedContext());
        mainBinding.spinnerTaskLists.setAdapter(taskListsAdapter);
        mainBinding.spinnerTaskLists.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                TaskList taskList = ((TaskList) adapterView.getItemAtPosition(position));
                mainActivityPresenter.setLastAccessedTaskList(taskList.getId());
                tasksFragment = TasksFragment.newInstance(taskList.getId());
                // TODO: 2016-07-24 how about one fragment with changing data?
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, tasksFragment)
                        .commitAllowingStateLoss();
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
                .addProfiles(
                        profile,
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

        accountHeader.getHeaderBackgroundView().setTag(new CoverPhotoTarget());

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
                                mainBinding.spinnerTaskLists.setVisibility(VISIBLE);
                                break;
                            case ID_TASK_LISTS:
                                getSupportActionBar().setDisplayShowTitleEnabled(true);
                                mainBinding.spinnerTaskLists.setVisibility(GONE);
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
            accountHeader.setActiveProfile(profile);
        }

        mainActivityPresenter.getTaskLists();
        mainActivityPresenter.loadUserPictures();
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
    public void onTaskListsLoaded(List<TaskList> taskLists, int currentTaskListIndex) {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//            case RC_ADD:
//                if(resultCode == RESULT_OK) {
//                    MaterialAccount acc = new MaterialAccount(this.getResources(), data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),"", R.drawable.ic_photo, R.drawable.ic_cover);
//                    this.addAccount(acc);
//                    Toast.makeText(MainActivity.this, data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME), Toast.LENGTH_SHORT).show();
//                }
//                break;
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.schedule_notification:
                scheduleNotification(getNotification("Testing"), new Date());
//                scheduleNotification(getTaskNotification("3 second delay"), 3000);
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scheduleNotification(Notification notification, Date date) {

        Intent notificationIntent = new Intent(this, TaskNotificationReceiver.class);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
    }

    private void scheduleNotification(Notification notification, int delay) {

        Intent notificationIntent = new Intent(this, TaskNotificationReceiver.class);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(TaskNotificationReceiver.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Scheduled Notification")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_assignment_turned_in_24dp);
        return builder.build();
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
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) drawer.closeDrawer();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainActivityPresenter.unbindView(this);
    }

    private class ProfileIconTarget implements Target {
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

    private class CoverPhotoTarget implements Target {
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
