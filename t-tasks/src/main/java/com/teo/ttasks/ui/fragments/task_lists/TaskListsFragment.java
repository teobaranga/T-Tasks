package com.teo.ttasks.ui.fragments.task_lists;

import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.helpers.ClickListenerHelper;
import com.teo.ttasks.R;
import com.teo.ttasks.TTasksApp;
import com.teo.ttasks.databinding.FragmentTaskListsBinding;
import com.teo.ttasks.receivers.NetworkInfoReceiver;
import com.teo.ttasks.ui.DividerItemDecoration;
import com.teo.ttasks.ui.activities.main.MainActivity;
import com.teo.ttasks.ui.items.TaskListItem;
import com.teo.ttasks.util.NightHelper;

import java.util.List;

import javax.inject.Inject;

import static android.view.View.GONE;

public class TaskListsFragment extends Fragment implements TaskListsView, SwipeRefreshLayout.OnRefreshListener {

    @Inject NetworkInfoReceiver networkInfoReceiver;
    @Inject TaskListsPresenter taskListsPresenter;

    ClickListenerHelper<TaskListItem> clickListenerHelper;
    FastAdapter<TaskListItem> fastAdapter;

    private ItemAdapter<TaskListItem> itemAdapter;

    private FragmentTaskListsBinding taskListsBinding;

    public static TaskListsFragment newInstance() {
        return new TaskListsFragment();
    }

    void showDeleteTaskListDialog(String taskListId) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_task_list)
                .setMessage(R.string.delete_task_list_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> taskListsPresenter.deleteTaskList(taskListId))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> { })
                .show();
    }

    private void showEditTaskListDialog(@Nullable TaskListItem taskListItem) {
        boolean newTaskList = taskListItem == null;

        int themeResId = NightHelper.isNight(getContext()) ? R.style.MaterialBaseTheme_AlertDialog : R.style.MaterialBaseTheme_Light_AlertDialog;

        final AlertDialog editDialog = new AlertDialog.Builder(getActivity(), themeResId)
                .setView(R.layout.dialog_task_list_edit)
                .setTitle(newTaskList ? R.string.new_task_list : R.string.edit_task_list)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> { })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> { })
                .show();

        // Create the task list if the title is valid
        editDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final EditText title = (EditText) editDialog.findViewById(R.id.task_list_title);
            //noinspection ConstantConditions
            final String taskListTitle = title.getText().toString();
            final Context context = getContext();
            if (!networkInfoReceiver.isOnline(context)) {
                Toast.makeText(context, "You must be online to be able to create a task list", Toast.LENGTH_SHORT).show();
            } else if (!taskListTitle.isEmpty()) {
                taskListsPresenter.setTaskListTitle(taskListTitle);
                if (taskListItem != null) {
                    taskListsPresenter.updateTaskList(taskListItem.getId(), networkInfoReceiver.isOnline(context));
                } else {
                    taskListsPresenter.createTaskList();
                }
                editDialog.dismiss();
            } else {
                Toast.makeText(context, R.string.error_task_list_title_missing, Toast.LENGTH_SHORT).show();
            }
        });

        // Set the task list title
        if (!newTaskList)
            //noinspection ConstantConditions
            ((EditText) editDialog.findViewById(R.id.task_list_title)).setText(taskListItem.getTitle());

        // Make sure the soft keyboard is displayed at the same time as the dialog
        //noinspection ConstantConditions
        editDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        editDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TTasksApp.get(getContext()).userComponent().inject(this);
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();
        clickListenerHelper = new ClickListenerHelper<>(fastAdapter);
        fastAdapter.withOnClickListener((v, adapter, item, position) -> {
            showEditTaskListDialog(item);
            return true;
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionButton fab = ((MainActivity) getActivity()).fab();
        fab.setOnClickListener(v -> showEditTaskListDialog(null));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        taskListsBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_task_lists, container, false);

        // Handle delete button click
        fastAdapter.withOnCreateViewHolderListener(new FastAdapter.OnCreateViewHolderListener() {
            @Override
            public RecyclerView.ViewHolder onPreCreateViewHolder(ViewGroup parent, int viewType) {
                return fastAdapter.getTypeInstance(viewType).getViewHolder(parent);
            }

            @Override
            public RecyclerView.ViewHolder onPostCreateViewHolder(final RecyclerView.ViewHolder viewHolder) {
                clickListenerHelper.listen(viewHolder, ((TaskListItem.ViewHolder) viewHolder).itemTaskListBinding.deleteTaskList,
                        (v, position, item) -> showDeleteTaskListDialog(item.getId()));
                return viewHolder;
            }
        });

        taskListsBinding.taskLists.setLayoutManager(new LinearLayoutManager(getContext()));
        taskListsBinding.taskLists.addItemDecoration(new DividerItemDecoration(getContext(), null));
        taskListsBinding.taskLists.setAdapter(itemAdapter.wrap(fastAdapter));

        taskListsBinding.swipeRefreshLayout.setOnRefreshListener(this);

        taskListsPresenter.bindView(this);
        taskListsPresenter.getTaskLists();

        return taskListsBinding.getRoot();
    }

    @Override
    public void onRefresh() {
        // TODO: 2016-08-16 implement
        taskListsBinding.swipeRefreshLayout.setRefreshing(false);
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
    public void onTaskListsLoaded(List<TaskListItem> taskListItems) {
        itemAdapter.setNewList(taskListItems);
        taskListsBinding.taskListsLoading.setVisibility(GONE);
        taskListsBinding.taskListsLoadingError.setVisibility(GONE);
        taskListsBinding.taskListsEmpty.setVisibility(GONE);

        // Enable or disable scrolling depending on the amount of task lists to be displayed
        taskListsBinding.taskLists.post(() -> {
            final LinearLayoutManager layoutManager = (LinearLayoutManager) taskListsBinding.taskLists.getLayoutManager();
            final int position = layoutManager.findLastVisibleItemPosition();
            if ((itemAdapter.getItemCount() - 1) <= position || position == RecyclerView.NO_POSITION) {
                // All task lists fit on the screen, no need for scrolling
                ((MainActivity) getActivity()).disableScrolling(true);
            } else {
                ((MainActivity) getActivity()).enableScrolling();
            }
        });
    }

    @Override
    public void onTaskListUpdateError() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        taskListsPresenter.unbindView(this);
    }
}
