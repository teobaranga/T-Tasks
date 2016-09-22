package com.teo.ttasks.ui.items;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TTaskList;
import com.teo.ttasks.databinding.ItemTaskListBinding;

import java.util.List;

import timber.log.Timber;

public class TaskListItem extends AbstractItem<TaskListItem, TaskListItem.ViewHolder> {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

    private final TTaskList taskList;
    private final long taskCount;

    public TaskListItem(TTaskList taskList, long taskCount) {
        this.taskList = taskList;
        this.taskCount = taskCount;
        Timber.d("count %d", taskCount);
    }

    public String getTitle() {
        return taskList.getTitle();
    }

    public String getId() {
        return taskList.getId();
    }

    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }

    @Override
    public int getType() {
        return R.id.task_list_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_task_list;
    }

    @Override
    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        ItemTaskListBinding itemTaskBinding = viewHolder.itemTaskListBinding;
        Context context = itemTaskBinding.getRoot().getContext();

        itemTaskBinding.taskListTitle.setText(taskList.getTitle());
        itemTaskBinding.taskListSize.setText(taskCount > 0 ? context.getString(R.string.task_list_size, taskCount) : context.getString(R.string.empty_task_list));
    }

    private static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ItemTaskListBinding itemTaskListBinding;

        ViewHolder(View view) {
            super(view);
            itemTaskListBinding = DataBindingUtil.bind(view);
        }
    }
}
