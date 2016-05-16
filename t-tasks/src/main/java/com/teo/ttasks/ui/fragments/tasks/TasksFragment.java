package com.teo.ttasks.ui.fragments.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TasksFragment extends Fragment implements TasksView, SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_TASK_LIST_ID = "taskListId";

    @BindView(R.id.list) RecyclerView mTaskList;
    @BindView(R.id.fab) FloatingActionButton mFloatingActionButton;
    @BindView(R.id.items_loading_ui) View loadingUiView;
    @BindView(R.id.items_loading_error_ui) View errorUiView;
    @BindView(R.id.items_empty) View emptyUiView;
    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeRefreshLayout;

    @Inject TasksPresenter mTasksPresenter;

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
        TTasksApp.get(context).tasksComponent().inject(this);
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

        // All the task items have the same size
        mTaskList.setHasFixedSize(true);
        mTaskList.setLayoutManager(new LinearLayoutManager(getContext()));
        //mTaskList.addItemDecoration(new DividerItemDecoration(getContext(), null));
        ((SimpleItemAnimator) mTaskList.getItemAnimator()).setSupportsChangeAnimations(false);
        mTaskList.setAdapter(mItemAdapter.wrap(mFastAdapter));

        mFloatingActionButton.setOnClickListener(view1 -> Toast.makeText(getContext(), "Click", Toast.LENGTH_SHORT).show());

        mSwipeRefreshLayout.setOnRefreshListener(this);

        mTasksPresenter.getTasks(mTaskListId, false);
    }

    @Override
    public void onRefresh() {
        mTasksPresenter.getTasks(mTaskListId, true);
    }

    @Override
    public void showLoadingUi() {
        loadingUiView.setVisibility(VISIBLE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(GONE);
    }

    @Override
    public void showErrorUi() {
        mSwipeRefreshLayout.setRefreshing(false);
        mItemAdapter.clear();
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(VISIBLE);
        emptyUiView.setVisibility(GONE);
    }

    @Override
    public void showEmptyUi() {
        mItemAdapter.clear();
        mSwipeRefreshLayout.setRefreshing(false);
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(VISIBLE);
    }

    @Override
    public void showContentUi(@NonNull List<IItem> taskItems) {
        mItemAdapter.setNewList(taskItems);
        mSwipeRefreshLayout.setRefreshing(false);
        loadingUiView.setVisibility(GONE);
        errorUiView.setVisibility(GONE);
        emptyUiView.setVisibility(GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTasksPresenter.unbindView();
        mUnbinder.unbind();
    }

    public String getTaskListId() {
        return mTaskListId;
    }
}