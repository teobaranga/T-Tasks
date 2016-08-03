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
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TasksFragment extends Fragment implements TasksView, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";

    private static final int RC_USER_RECOVERABLE = 1;
    private static final int RC_CREATE_TASK = 100;

    @BindView(R.id.list) RecyclerView mTaskList;
    @BindView(R.id.fab) FloatingActionButton mFloatingActionButton;
    @BindView(R.id.items_loading_ui) View loadingUiView;
    @BindView(R.id.items_loading_error_ui) View errorUiView;
    @BindView(R.id.items_empty) View emptyUiView;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;

    @Inject TasksPresenter mTasksPresenter;
    @Inject NetworkInfoReceiver mNetworkInfoReceiver;

    private FastAdapter<IItem> mFastAdapter;
    private ItemAdapter<IItem> mItemAdapter;

    private String mTaskListId;

    private Unbinder mUnbinder;

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
        mTaskListId = getArguments().getString(ARG_TASK_LIST_ID);
        TTasksApp.get(context).userComponent().inject(this);
        mFastAdapter = new FastAdapter<>();
        mItemAdapter = new ItemAdapter<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTasksPresenter.bindView(this);

        mFloatingActionButton.setOnClickListener(view1 ->
                EditTaskActivity.startCreate(this, mTaskListId, RC_CREATE_TASK, null));

        mFastAdapter.withOnClickListener((v, adapter, item, position) -> {
            if (item instanceof TaskItem) {
                TaskItem.ViewHolder viewHolder = ((TaskItem) item).getViewHolder(v);
                //noinspection unchecked
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                        Pair.create(viewHolder.taskLayout, getResources().getString(R.string.transition_task_header)));
                TaskDetailActivity.start(getContext(), ((TaskItem) item).getTaskId(), mTaskListId, options.toBundle());
            }
            return true;
        });

        // All the task items have the same size
        mTaskList.setLayoutManager(new LinearLayoutManager(getContext()));
        mTaskList.setAdapter(mItemAdapter.wrap(mFastAdapter));

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mTasksPresenter.getTasks(mTaskListId);

        if (mNetworkInfoReceiver.isOnline(getContext()))
            mTasksPresenter.refreshTasks(mTaskListId);
    }

    @Override
    public void onRefresh() {
        if (mNetworkInfoReceiver.isOnline(getContext())) {
            mTasksPresenter.refreshTasks(mTaskListId);
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
            mItemAdapter.clear();
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(VISIBLE);
            emptyUiView.setVisibility(GONE);
        }
        onRefreshDone();
    }

    @Override
    public void showEmptyUi() {
        mItemAdapter.clear();
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(VISIBLE);
        onRefreshDone();
    }

    @Override
    public void showContentUi(@NonNull List<IItem> taskItems) {
        mItemAdapter.setNewList(taskItems);
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(GONE);
        onRefreshDone();
    }

    @Override
    public void onRefreshDone() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CREATE_TASK:
                if (resultCode == RESULT_OK) {
                    mTasksPresenter.refreshTasks(mTaskListId);
                    Timber.d("refreshed task");
                }
                return;
            case RC_USER_RECOVERABLE:
                if (resultCode == RESULT_OK) {
                    // Re-authorization successful, refresh the tasks
                    mTasksPresenter.refreshTasks(mTaskListId);
                    return;
                }
                Toast.makeText(getContext(), R.string.error_google_permissions_denied, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTasksPresenter.unbindView(this);
        mUnbinder.unbind();
    }

    public String getTaskListId() {
        return mTaskListId;
    }
}
