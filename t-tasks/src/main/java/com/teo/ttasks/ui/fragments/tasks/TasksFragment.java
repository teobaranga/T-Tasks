package com.teo.ttasks.ui.fragments.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.activities.edit_task.EditTaskActivity;
import com.teo.ttasks.ui.activities.task_detail.TaskDetailActivity;
import com.teo.ttasks.ui.items.TaskItem;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TasksFragment extends Fragment implements TasksView, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";

    private static final int RC_USER_RECOVERABLE = 1;

    @BindView(R.id.list) RecyclerView taskList;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.items_loading_ui) View loadingUiView;
    @BindView(R.id.items_loading_error_ui) View errorUiView;
    @BindView(R.id.items_empty) View emptyUiView;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;

    @Inject TasksPresenter tasksPresenter;
    @Inject NetworkInfoReceiver networkInfoReceiver;

    private FastAdapter<IItem> fastAdapter;
    private ItemAdapter<IItem> itemAdapter;

    private String taskListId;

    private Unbinder unbinder;

    private Pair<View, String> navBar;

    /**
     * Create a new instance of this fragment using the provided task list ID
     */
    public static TasksFragment newInstance(@NonNull String taskListId) {
        TasksFragment tasksFragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_LIST_ID, taskListId);
        tasksFragment.setArguments(args);
        return tasksFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        taskListId = getArguments().getString(ARG_TASK_LIST_ID);
        TTasksApp.get(context).userComponent().inject(this);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        View navigationBar = getActivity().getWindow().getDecorView().findViewById(android.R.id.navigationBarBackground);
        navBar = Pair.create(navigationBar, "navBar");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        unbinder = ButterKnife.bind(this, view);

        networkInfoReceiver.setOnConnectionChangedListener(isOnline -> {
            if (isOnline) {
                // Sync tasks
                tasksPresenter.syncTasks(taskListId);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tasksPresenter.bindView(this);

        fab.setOnClickListener(view1 ->
                EditTaskActivity.startCreate(this, taskListId, null));

        fastAdapter.withOnClickListener((v, adapter, item, position) -> {
            if (item instanceof TaskItem) {
                TaskItem.ViewHolder viewHolder = ((TaskItem) item).getViewHolder(v);
                Pair<View, String> taskHeader = Pair.create(viewHolder.taskLayout, getResources().getString(R.string.transition_task_header));
                //noinspection unchecked
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), taskHeader, navBar);
                TaskDetailActivity.start(getContext(), ((TaskItem) item).getTaskId(), taskListId, options.toBundle());
            }
            return true;
        });

        // All the task items have the same size
        taskList.setLayoutManager(new LinearLayoutManager(getContext()));
        taskList.setAdapter(itemAdapter.wrap(fastAdapter));

        swipeRefreshLayout.setOnRefreshListener(this);

        tasksPresenter.getTasks(taskListId);

        // Synchronize tasks and then refresh this task list
        if (networkInfoReceiver.isOnline(getContext()))
            tasksPresenter.syncTasks(taskListId);
    }

    @Override
    public void onRefresh() {
        if (networkInfoReceiver.isOnline(getContext())) {
            tasksPresenter.syncTasks(taskListId);
        } else {
            onRefreshDone();
        }
    }

    @Override
    public void onTasksLoading() {
        loadingUiView.setVisibility(VISIBLE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(GONE);
    }

    @Override
    public void onTasksLoadError(@Nullable Intent resolveIntent) {
        if (resolveIntent != null) {
            startActivityForResult(resolveIntent, RC_USER_RECOVERABLE);
        } else {
            itemAdapter.clear();
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(VISIBLE);
            emptyUiView.setVisibility(GONE);
        }
        onRefreshDone();
    }

    @Override
    public void showEmptyUi() {
        itemAdapter.clear();
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(VISIBLE);
        onRefreshDone();
    }

    @Override
    public void showContentUi(@NonNull List<IItem> taskItems) {
        itemAdapter.setNewList(taskItems);
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(GONE);
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
        swipeRefreshLayout.setRefreshing(false);
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
    public void onDestroyView() {
        super.onDestroyView();
        tasksPresenter.unbindView(this);
        unbinder.unbind();
    }

    public String getTaskListId() {
        return taskListId;
    }
}
