package com.teo.ttasks.ui.fragments.task_lists;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.databinding.FragmentTaskListsBinding;
import com.teo.ttasks.ui.activities.main.MainActivity;

import java.util.List;

import javax.inject.Inject;

import static android.view.View.GONE;

public class TaskListsFragment extends Fragment implements TaskListsView, SwipeRefreshLayout.OnRefreshListener {

    @Inject TaskListsPresenter taskListsPresenter;

    private FastAdapter<IItem> fastAdapter;
    private ItemAdapter<IItem> itemAdapter;

    private FragmentTaskListsBinding taskListsBinding;

    public static TaskListsFragment newInstance() {
        return new TaskListsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(getContext()).userComponent().inject(this);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionButton fab = ((MainActivity) getActivity()).fab();
        fab.setOnClickListener(v -> {});
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        taskListsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_lists, container, false);
        taskListsBinding.taskLists.setLayoutManager(new LinearLayoutManager(getContext()));
        taskListsBinding.taskLists.setAdapter(itemAdapter.wrap(fastAdapter));
        taskListsBinding.swipeRefreshLayout.setOnRefreshListener(this);

        taskListsPresenter.bindView(this);
        taskListsPresenter.getTaskLists();

        return taskListsBinding.getRoot();
    }

    @Override
    public void onRefresh() {
        // TODO: 2016-08-16 implement
    }

    @Override
    public void onTaskListsLoading() {

    }

    @Override
    public void onTaskListsEmpty() {

    }

    @Override
    public void onTaskListsError() {

    }

    @Override
    public void onTaskListsLoaded(List<IItem> taskListItems) {
        itemAdapter.setNewList(taskListItems);
        taskListsBinding.taskListsLoading.setVisibility(GONE);
        taskListsBinding.taskListsLoadingError.setVisibility(GONE);
        taskListsBinding.taskListsEmpty.setVisibility(GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        taskListsPresenter.unbindView(this);
    }
}
