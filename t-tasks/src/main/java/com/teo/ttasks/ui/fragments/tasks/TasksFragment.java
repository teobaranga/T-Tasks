package com.teo.ttasks.ui.fragments.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.ui.adapters.TasksAdapter;
import com.teo.ttasks.data.model.Task;
import com.teo.ttasks.ui.DividerItemDecoration;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.base.BaseFragment;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.realm.RealmResults;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TasksFragment extends BaseFragment implements TasksView, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";
    private static final String ARG_TASK_LIST_NAME = "taskListName";

    @Bind(R.id.list) RecyclerView mTaskList;
    @Bind(R.id.fab) FloatingActionButton mFloatingActionButton;
    @Bind(R.id.items_loading_ui) View loadingUiView;
    @Bind(R.id.items_loading_error_ui) View errorUiView;
    @Bind(R.id.items_empty) View emptyUiView;
    @Bind(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;

    @Inject TasksPresenter mTasksPresenter;

    private TasksAdapter mTasksAdapter;

    private String mTaskListId;
    private String mTaskListName;

    /**
     * Create a new instance of this fragment using the provided task list ID
     */
    public static TasksFragment newInstance(String taskListId, String taskListName) {
        TasksFragment tasksFragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_LIST_ID, taskListId);
        args.putString(ARG_TASK_LIST_NAME, taskListName);
        tasksFragment.setArguments(args);
        return tasksFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTaskListId = getArguments().getString(ARG_TASK_LIST_ID);
        mTaskListName = getArguments().getString(ARG_TASK_LIST_NAME);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(getContext()).tasksApiComponent().plus(new TasksFragmentModule()).inject(this);
        mTasksAdapter = new TasksAdapter(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        ((MainActivity) getActivity()).toolbar().setTitle(mTaskListName);
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mTasksPresenter.bindView(this);

        // All the task items have the same size
        mTaskList.setHasFixedSize(true);
        mTaskList.setLayoutManager(new LinearLayoutManager(getContext()));
        mTaskList.addItemDecoration(new DividerItemDecoration(getContext(), null));
        mTaskList.setAdapter(mTasksAdapter);

        mFloatingActionButton.setOnClickListener(view1 -> Toast.makeText(getContext(), "Click", Toast.LENGTH_SHORT).show());

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mTasksPresenter.loadTasks(mTaskListId);
    }

    @Override
    public void onRefresh() {
        mTasksPresenter.reloadTasks(mTaskListId);
    }

    @Override
    public void showLoadingUi() {
        runOnUiThreadIfFragmentAlive(() -> {
            loadingUiView.setVisibility(VISIBLE);
            errorUiView.setVisibility(GONE);
            emptyUiView.setVisibility(GONE);
        });
    }

    @Override
    public void showErrorUi() {
        runOnUiThreadIfFragmentAlive(() -> {
            mSwipeRefreshLayout.setRefreshing(false);
//            mTasksAdapter.clear();
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(VISIBLE);
            emptyUiView.setVisibility(GONE);
        });
    }

    @Override
    public void showEmptyUi() {
        runOnUiThreadIfFragmentAlive(() -> {
            mTasksAdapter.clear();
            mSwipeRefreshLayout.setRefreshing(false);
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(GONE);
            emptyUiView.setVisibility(VISIBLE);
        });
    }

    @Override
    public void showContentUi(@NonNull RealmResults<Task> tasks) {
        runOnUiThreadIfFragmentAlive(() -> {
            mTasksAdapter.reloadData(tasks);
            mSwipeRefreshLayout.setRefreshing(false);
            loadingUiView.setVisibility(GONE);
            errorUiView.setVisibility(GONE);
            emptyUiView.setVisibility(GONE);
        });
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);
        mTasksPresenter.unbindView(this);
        super.onDestroyView();
    }

    public String getTaskListId() {
        return mTaskListId;
    }
}