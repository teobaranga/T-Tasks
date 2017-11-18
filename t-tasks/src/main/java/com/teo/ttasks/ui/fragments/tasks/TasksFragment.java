package com.teo.ttasks.ui.fragments.tasks;

import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.expandable.ExpandableExtension;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.teo.ttasks.R;
import com.teo.ttasks.databinding.FragmentTasksBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.CategoryItem;
import com.teo.ttasks.ui.items.TaskItem;
import com.teo.ttasks.util.RxUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME;
import static com.mikepenz.fastadapter.adapters.ItemAdapter.items;

public class TasksFragment extends DaggerFragment implements TasksView,
                                                             SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";

    private static final int RC_USER_RECOVERABLE = 1;

    private static final long completedHeaderId = Long.MAX_VALUE;

    /**
     * Array holding the 3 shared elements used during the transition to the {@link TaskDetailActivity}.<br>
     * Indexes:<br>
     * 0 - task header layout, always present<br>
     * 1 - Navigation bar, can be missing<br>
     * 2 - FAB, could be hidden<br>
     */
    @SuppressWarnings("unchecked")
    final Pair<View, String>[] pairs = new Pair[3];

    @Inject TasksPresenter tasksPresenter;

    @Nullable String taskListId;

    FragmentTasksBinding tasksBinding;

    /**
     * The navigation bar view along with its associated transition name, used as a shared
     * element when selecting a task to prevent the task item from overlapping
     * it during the animation<br><br>
     * This is cached to avoid the {@code findViewById} every time a task is selected.<br>
     * <b>Note:</b> The view can be null if the activity is re-created after getting killed and
     * if that's the case {@link #createNavBarPair()} must be called to recreate it.
     *
     * @see <a href="http://stackoverflow.com/q/32501024/5606622">Shared elements overflow navigation bar in transition animation</a>
     */
    Pair<View, String> navBar;

    FloatingActionButton fab;
    ItemAdapter<CategoryItem> completedHeaderAdapter;
    ItemAdapter<IItem> itemAdapter;
    private FastAdapter<IItem> fastAdapter;
    private ExpandableExtension<IItem> expandableExtension;
    private final OnClickListener<IItem> taskItemClickListener = new OnClickListener<IItem>() {
        // Reject quick, successive clicks because they break the app
        private static final long MIN_CLICK_INTERVAL = 1000;
        private long lastClickTime = 0;

        @Override
        public boolean onClick(View v, IAdapter adapter, IItem item, int position) {
            if (item instanceof TaskItem) {
                // Handle click on a task item
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime;

                    // Make sure the navigation bar view isn't null
                    if (navBar.first == null)
                        createNavBarPair();

                    // Add the task header layout to the shared elements
                    final TaskItem taskItem = (TaskItem) item;
                    pairs[0] = Pair.create(taskItem.getViewHolder(v).binding.layoutTask, getString(R.string.transition_task_header));

                    // Find the shared elements to be used in the transition
                    Pair<View, String>[] sharedElements;

                    if (!fab.isShown()) {
                        // Check the navigation bar view
                        if (pairs[1].first == null) {
                            // Get only the task header layout element
                            //noinspection unchecked
                            sharedElements = new Pair[]{pairs[0]};
                        } else {
                            // Get the task header layout and the navigation bar
                            //noinspection unchecked
                            sharedElements = new Pair[]{pairs[0], pairs[1]};
                        }
                    } else {
                        if (pairs[1].first == null) {
                            // Get only the task header and the FAB
                            //noinspection unchecked
                            sharedElements = new Pair[]{pairs[0], pairs[2]};
                        } else {
                            // Get all the 3 elements
                            sharedElements = pairs;
                        }
                    }

                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), sharedElements);
                    TaskDetailActivity.start(getContext(), taskItem.getTaskId(), taskListId, options.toBundle());
                }
            } else if (item instanceof CategoryItem) {
                // Handle click on the "Completed" section
                final CategoryItem categoryItem = (CategoryItem) item;
                final List<TaskItem> subItems = categoryItem.getSubItems();
                if (subItems != null && !subItems.isEmpty()) {
                    final boolean showCompleted = !categoryItem.isExpanded();
                    if (showCompleted) {
                        categoryItem.withTitle(String.format(getString(R.string.completed_count), subItems.size()));
                    } else {
                        categoryItem.withTitle(R.string.completed);
                    }
                    categoryItem.toggleArrow(true);
                    tasksPresenter.setShowCompleted(!showCompleted);
                    fastAdapter.notifyAdapterItemChanged(completedHeaderAdapter.getGlobalPosition(0));
                }
            }
            return true;
        }
    };
    private NetworkInfoReceiver networkInfoReceiver;

    /**
     * Create a new instance of this fragment
     */
    public static TasksFragment newInstance() {
        return new TasksFragment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_USER_RECOVERABLE:
                if (resultCode == RESULT_OK) {
                    // Re-authorization successful, sync & refresh the tasks
                    tasksPresenter.syncTasks(taskListId);
                    return;
                }
                Toast.makeText(getContext(), R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        itemAdapter = items();
        completedHeaderAdapter = new ItemAdapter<>();

        if (savedInstanceState != null) {
            taskListId = savedInstanceState.getString(ARG_TASK_LIST_ID);
        }

        createNavBarPair();

        expandableExtension = new ExpandableExtension<>();
        fastAdapter = FastAdapter.with(Arrays.asList(itemAdapter, completedHeaderAdapter), Collections.singletonList(expandableExtension));
        fastAdapter.withOnClickListener(taskItemClickListener);

        networkInfoReceiver = new NetworkInfoReceiver();
        networkInfoReceiver.setOnConnectionChangedListener(isOnline -> {
            if (isOnline) {
                Timber.d("isOnline");
                // Sync tasks
                tasksPresenter.syncTasks(taskListId);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        tasksBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_tasks, container, false);
        View view = tasksBinding.getRoot();

        getContext().registerReceiver(networkInfoReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tasksPresenter.bindView(this);

        tasksBinding.tasksList.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksBinding.tasksList.setAdapter(fastAdapter);
        ((SimpleItemAnimator) tasksBinding.tasksList.getItemAnimator()).setSupportsChangeAnimations(false);

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(this);

        tasksPresenter.getTasks(taskListId);

        // Synchronize tasks and then refresh this task list
        refreshTasks();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fab = ((MainActivity) getActivity()).fab();
        fab.setOnClickListener(view1 -> EditTaskActivity.startCreate(this, taskListId, null));
        pairs[2] = Pair.create(fab, getString(R.string.transition_fab));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TASK_LIST_ID, taskListId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tasksPresenter.unbindView(this);
        getContext().unregisterReceiver(networkInfoReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_tasks, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_due_date:
                if (tasksPresenter.switchSortMode(RxUtils.SORT_DATE))
                    tasksPresenter.getTasks(taskListId);
                break;
            case R.id.menu_sort_alphabetical:
                if (tasksPresenter.switchSortMode(RxUtils.SORT_ALPHA))
                    tasksPresenter.getTasks(taskListId);
                break;
            case R.id.menu_sort_my_order:
                if (tasksPresenter.switchSortMode(RxUtils.SORT_MY_ORDER))
                    tasksPresenter.getTasks(taskListId);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTasksLoading() {
        tasksBinding.tasksLoading.setVisibility(VISIBLE);
        tasksBinding.tasksEmpty.setVisibility(GONE);
    }

    @Override
    public void onActiveTasksLoaded(List<TaskItem> activeTasks) {
        //noinspection unchecked
        itemAdapter.setNewList((List<IItem>) (List<?>) activeTasks);
    }

    @Override
    public void onCompletedTasksLoaded(List<TaskItem> completedTasks) {
        final boolean emptyAdapter = completedHeaderAdapter.getAdapterItemCount() == 0;
        if (completedTasks.isEmpty()) {
            completedHeaderAdapter.clear();
        } else {
            if (emptyAdapter) {
                final CategoryItem completedHeader = new CategoryItem<>()
                        .withTitle(R.string.completed)
                        .withIsExpanded(tasksPresenter.getShowCompleted())
                        .withIdentifier(completedHeaderId);
                completedHeader.withSubItems(completedTasks);
                completedHeaderAdapter.add(completedHeader);
                new Handler().post(() -> completedHeader.toggleArrow(false));
            } else {
                final CategoryItem completedHeader = completedHeaderAdapter.getAdapterItem(completedHeaderAdapter.getAdapterPosition(completedHeaderId));
                completedHeader.withSubItems(completedTasks);
                fastAdapter.notifyAdapterItemChanged(fastAdapter.getPosition(completedHeaderId));
            }
        }
    }

    @Override
    public void onTasksLoadError(@Nullable Intent resolveIntent) {
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE);
        } else {
            Toast.makeText(getContext(), R.string.error_tasks_loading, Toast.LENGTH_SHORT).show();
        }
        onRefreshDone();
    }

    @Override
    public void onTasksEmpty() {
        itemAdapter.clear();
        completedHeaderAdapter.clear();
        tasksBinding.tasksLoading.setVisibility(GONE);
        tasksBinding.tasksEmpty.setVisibility(VISIBLE);
        onRefreshDone();
    }

    @Override
    public void onTasksLoaded() {
        tasksBinding.tasksLoading.setVisibility(GONE);
        tasksBinding.tasksEmpty.setVisibility(GONE);
        onRefreshDone();

        tasksBinding.tasksList.post(() -> {
            final LinearLayoutManager layoutManager = (LinearLayoutManager) tasksBinding.tasksList.getLayoutManager();
            final int position = layoutManager.findLastVisibleItemPosition();
            if ((itemAdapter.getAdapterItemCount() - 1) <= position || position == RecyclerView.NO_POSITION) {
                ((MainActivity) getActivity()).disableScrolling(true);
            } else {
                ((MainActivity) getActivity()).enableScrolling();
            }
        });
    }

    @Override
    public void onRefreshDone() {
        tasksBinding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onSyncDone(int taskSyncCount) {
        if (taskSyncCount != 0)
            Toast.makeText(getContext(), "Synchronized " + taskSyncCount + " tasks", Toast.LENGTH_SHORT).show();
        tasksPresenter.refreshTasks(taskListId);
    }

    @Override
    public void onRefresh() {
        refreshTasks();
    }

    /** Cache the Pair holding the navigation bar view and its associated transition name */
    void createNavBarPair() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View navBarView = getActivity().getWindow().getDecorView().findViewById(android.R.id.navigationBarBackground);
            navBar = Pair.create(navBarView, NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
            pairs[1] = navBar;
        }
    }

    /**
     * Trigger the refresh process on an active network connection.
     */
    private void refreshTasks() {
        if (!networkInfoReceiver.isOnline(getContext()) || taskListId == null) {
            onRefreshDone();
        } else {
            tasksPresenter.syncTasks(taskListId);
        }
    }

    /**
     * Switch the task list associated with this fragment and reload the tasks.
     *
     * @param newTaskListId task list identifier
     */
    public void setTaskList(String newTaskListId) {
        if (!newTaskListId.equals(taskListId)) {
            // Disable fragment scrolling only if attached to prevent crashing
            if (isAdded())
                ((MainActivity) getActivity()).disableScrolling(false);
            taskListId = newTaskListId;
            if (tasksPresenter != null) {
                tasksPresenter.getTasks(taskListId);
                refreshTasks();
            }
        }
    }
}
