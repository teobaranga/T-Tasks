package com.teo.ttasks;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.melnykov.fab.FloatingActionButton;

/**
 * @author Teo
 */

public class TasksFragment extends Fragment {

    MainActivity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
        setRetainInstance(true);

        TasksDBAdapter dbHelper = new TasksDBAdapter(mActivity.getApplicationContext());
        dbHelper.open();
        mActivity.adapter = new MyListCursorAdapter(mActivity.getApplicationContext(), dbHelper.fetchAllTasks());
        mActivity.adapter.SetOnItemClickListener(new MyListCursorAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(View v, int position) {
                // TODO: do something with position
                //Toast.makeText(mActivity.getApplicationContext(), "YOLO", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mActivity.adapter);
    }
}