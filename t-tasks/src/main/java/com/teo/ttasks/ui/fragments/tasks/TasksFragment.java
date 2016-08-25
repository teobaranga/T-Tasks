package com.teo.ttasks.ui.fragments.tasks;

import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.databinding.FragmentTasksBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME;

public class TasksFragment extends Fragment implements TasksView, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";

    private static final int RC_USER_RECOVERABLE = 1;

    @Inject TasksPresenter tasksPresenter;

    private NetworkInfoReceiver networkInfoReceiver;

    private FastAdapter<IItem> fastAdapter;
    private ItemAdapter<IItem> itemAdapter;

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

    @SuppressWarnings("unchecked")
    Pair<View, String>[] pairs = new Pair[3];

    private final FastAdapter.OnClickListener<IItem> taskItemClickListener = new FastAdapter.OnClickListener<IItem>() {
        // Reject quick, successive clicks because they break the app
        private static final long MIN_CLICK_INTERVAL = 1000;
        private long lastClickTime = 0;

        @Override public boolean onClick(View v, IAdapter<IItem> adapter, IItem item, int position) {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                lastClickTime = currentTime;
                if (item instanceof TaskItem) {
                    // Make sure the navigation bar view isn't null
                    if (navBar.first == null)
                        createNavBarPair();

                    pairs[1] = Pair.create(((TaskItem) item).getViewHolder(v).binding.layoutTask, getString(R.string.transition_task_header));

                    Pair<View, String>[] sharedElements = tasksBinding.fab.isShown() ? pairs : Arrays.copyOf(pairs, 2);

                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), sharedElements);
                    TaskDetailActivity.start(getContext(), ((TaskItem) item).getTaskId(), taskListId, options.toBundle());
                }
            }
            return true;
        }
    };

    /**
     * Create a new instance of this fragment using the provided task list ID
     */
    public static TasksFragment newInstance(@Nullable String taskListId) {
        TasksFragment tasksFragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_LIST_ID, taskListId);
        tasksFragment.setArguments(args);
        return tasksFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(getContext()).userComponent().inject(this);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        String taskListArg = getArguments().getString(ARG_TASK_LIST_ID);
        if (taskListArg != null) {
            taskListId = taskListArg;
        } else if (savedInstanceState != null) {
            taskListArg = savedInstanceState.getString(ARG_TASK_LIST_ID);
            if (taskListArg != null)
                taskListId = taskListArg;
        }

        createNavBarPair();

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

        pairs[2] = Pair.create(tasksBinding.fab, getString(R.string.transition_fab));

        getContext().registerReceiver(networkInfoReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tasksPresenter.bindView(this);

        tasksBinding.fab.setOnClickListener(view1 -> EditTaskActivity.startCreate(this, taskListId, null));

        tasksBinding.tasksList.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksBinding.tasksList.setAdapter(itemAdapter.wrap(fastAdapter));
        tasksBinding.tasksList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private static final int THRESHOLD = 0;

            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > THRESHOLD)
                    tasksBinding.fab.hide();
                else if (dy < -THRESHOLD)
                    tasksBinding.fab.show();
            }
        });

        tasksBinding.swipeRefreshLayout.setOnRefreshListener(this);

        tasksPresenter.getTasks(taskListId);

        // Synchronize tasks and then refresh this task list
        refreshTasks();
    }

    @Override
    public void onRefresh() {
        refreshTasks();
    }

    @Override
    public void onTasksLoading() {
        tasksBinding.tasksLoading.setVisibility(VISIBLE);
        tasksBinding.tasksLoadingError.setVisibility(GONE);
        tasksBinding.tasksEmpty.setVisibility(GONE);
    }

    @Override
    public void onTasksLoadError(@Nullable Intent resolveIntent) {
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE);
        } else {
            itemAdapter.clear();
            tasksBinding.tasksLoading.setVisibility(GONE);
            tasksBinding.tasksLoadingError.setVisibility(VISIBLE);
            tasksBinding.tasksEmpty.setVisibility(GONE);
        }
        onRefreshDone();
    }

    @Override
    public void showEmptyUi() {
        itemAdapter.clear();
        tasksBinding.tasksLoading.setVisibility(GONE);
        tasksBinding.tasksLoadingError.setVisibility(GONE);
        tasksBinding.tasksEmpty.setVisibility(VISIBLE);
        onRefreshDone();
    }

    @Override
    public void showContentUi(@NonNull List<IItem> taskItems) {
        itemAdapter.setNewList(taskItems);
        tasksBinding.tasksLoading.setVisibility(GONE);
        tasksBinding.tasksLoadingError.setVisibility(GONE);
        tasksBinding.tasksEmpty.setVisibility(GONE);
        onRefreshDone();
    }

    @Override
    public void onSyncDone(int taskSyncCount) {
        if (taskSyncCount != 0)
            Toast.makeText(getContext(), "Synchronized " + taskSyncCount + " tasks", Toast.LENGTH_SHORT).show();
        tasksPresenter.refreshTasks(taskListId);
    }

    @Override
    public void onRefreshDone() {
        tasksBinding.swipeRefreshLayout.setRefreshing(false);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TASK_LIST_ID, taskListId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tasksPresenter.unbindView(this);
        getContext().unregisterReceiver(networkInfoReceiver);
    }

    /** Cache the Pair holding the navigation bar view and its associated transition name */
    void createNavBarPair() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final View navBarView = getActivity().getWindow().getDecorView().findViewById(android.R.id.navigationBarBackground);
            navBar = Pair.create(navBarView, NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME);
            pairs[0] = navBar;
        }
    }

    private void refreshTasks() {
        if (!networkInfoReceiver.isOnline(getContext()) || taskListId == null) {
            onRefreshDone();
        } else {
            tasksPresenter.syncTasks(taskListId);
        }
    }

    public void setTaskList(String newTaskListId) {
        if (!newTaskListId.equals(taskListId)) {
            taskListId = newTaskListId;
            if (tasksPresenter != null) {
                tasksPresenter.getTasks(taskListId);
                refreshTasks();
            }
        }
    }
}
