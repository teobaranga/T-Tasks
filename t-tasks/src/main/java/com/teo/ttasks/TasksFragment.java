package com.teo.ttasks;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.api.services.tasks.model.Tasks;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.ui.floatingactionbutton.FloatingActionButton;
import com.teo.ttasks.activities.MainActivity;

/**
 * @author Teo
 */

// TODO: don't load tasks everytime onViewCreated is called
public class TasksFragment extends Fragment {

    private TasksDBAdapter dbHelper;
    private Context mContext;
    private MyListCursorAdapter adapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

        dbHelper = new TasksDBAdapter(mContext.getApplicationContext());
        dbHelper.open();
        adapter = new MyListCursorAdapter(mContext.getApplicationContext(), dbHelper.fetchAllTasks());
        dbHelper.close();
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
        ultimateRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ultimateRecyclerView.setAdapter(adapter);

    }

    public void fetchTasks() {
        // Get the token and load the tasks
        new TaskUtils.GetTokenTask(mContext, ((MainActivity) mContext).profile.getEmail().getText()) {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                // TODO: enable swipe refresh
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                new TaskUtils.GetTasks() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        dbHelper.open();
                    }

                    @Override
                    protected void onPostExecute(Tasks tasks) {
                        super.onPostExecute(tasks);
//                        if (success) {
//                            adapter.swapCursor(dbHelper.fetchAllTasks());
//                        }
                        dbHelper.close();

                        // TODO: disable swipe refresh
                    }
                }.execute();
            }
        }.execute();
    }
}