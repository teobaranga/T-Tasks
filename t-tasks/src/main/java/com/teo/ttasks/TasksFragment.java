package com.teo.ttasks;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.ui.floatingactionbutton.FloatingActionButton;
import com.teo.ttasks.model.Task;

import java.util.ArrayList;
import java.util.List;

public class TasksFragment extends Fragment {

    private Context mContext;
    private TasksAdapter mTasksAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mTasksAdapter = new TasksAdapter(new ArrayList<>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final UltimateRecyclerView ultimateRecyclerView = (UltimateRecyclerView) view.findViewById(R.id.ultimateRecyclerView);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        ultimateRecyclerView.setDefaultFloatingActionButton(fab);
        ultimateRecyclerView.showDefaultFloatingActionButton();
        // All the task items have the same size
        ultimateRecyclerView.setHasFixedSize(true);
        ultimateRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ultimateRecyclerView.setAdapter(mTasksAdapter);
    }

    public void addTasks(List<Task> tasks) {
        for (Task task : tasks)
            mTasksAdapter.add(task);
    }
}