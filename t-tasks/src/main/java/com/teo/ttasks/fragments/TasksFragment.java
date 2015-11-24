package com.teo.ttasks.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.tasks.model.Tasks;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.marshalchen.ultimaterecyclerview.ui.floatingactionbutton.FloatingActionButton;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasks;
import com.teo.ttasks.adapters.TasksAdapter;
import com.teo.ttasks.model.Task;
import com.teo.ttasks.util.PrefUtils;
import com.teo.ttasks.util.TaskUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import timber.log.Timber;

public class TasksFragment extends Fragment {

    private TasksAdapter mTasksAdapter;
    private String mTaskListId;
    private Realm realm;
    private List<Task> mTasks;

    /**
     * Create a new instance of this fragment using the provided task list ID
     */
    public static TasksFragment newInstance(String taskListId) {
        TasksFragment tasksFragment = new TasksFragment();
        Bundle args = new Bundle();
        args.putString("id", taskListId);
        tasksFragment.setArguments(args);
        return tasksFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mTaskListId = getArguments().getString("id");
        GoogleAccountCredential credential = GoogleAccountCredential
                .usingOAuth2(context.getApplicationContext(), Arrays.asList(TTasks.SCOPES2))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(getActivity().getPreferences(Context.MODE_PRIVATE).getString(PrefUtils.PREF_ACCOUNT_NAME, null));
        new TaskUtils.GetTasks(mTaskListId, credential) {
            @Override
            protected void onPostExecute(Tasks tasks) {
                super.onPostExecute(tasks);
                Timber.d("Found %d tasks", tasks.size());
                realm.executeTransaction((Realm realm) -> {
                    List<String> taskIds = new ArrayList<>();
                    for (com.google.api.services.tasks.model.Task task : tasks.getItems()) {
                        taskIds.add(task.getId());
                        Task t = realm.createOrUpdateObjectFromJson(Task.class, task.toString());
                        t.setTaskListId(mTaskListId);
                        t.setUpdatedDate(TaskUtils.parseRFC3339Date(task.getUpdated().toStringRfc3339()));
                        if (task.getDue() != null)
                            t.setDueDate(TaskUtils.parseRFC3339Date(task.getDue().toStringRfc3339()));
                        if (task.getCompleted() != null)
                            t.setCompletedDate(TaskUtils.parseRFC3339Date(task.getCompleted().toStringRfc3339()));
                    }
                    // Remove tasks deleted outside the app
                    if (mTasks != null)
                        for (Task task : mTasks)
                            if (!taskIds.contains(task.getId()))
                                task.removeFromRealm();
                });
            }
        }.execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        // Load the tasks locally from Realm
        mTasks = realm.where(Task.class).equalTo("taskListId", mTaskListId).findAll();
        mTasksAdapter = new TasksAdapter(mTasks);
        realm.addChangeListener(mTasksAdapter::notifyDataSetChanged);
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
}