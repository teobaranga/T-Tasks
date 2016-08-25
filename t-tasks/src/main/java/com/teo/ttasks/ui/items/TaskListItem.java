package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.items.AbstractItem;
import com.teo.ttasks.R;
import com.teo.ttasks.data.model.TaskList;
import com.teo.ttasks.databinding.ItemTaskListBinding;

public class TaskListItem extends AbstractItem<TaskListItem, TaskListItem.ViewHolder> {

    private final TaskList taskList;

    public TaskListItem(TaskList taskList) {
        this.taskList = taskList;
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
    public void bindView(ViewHolder viewHolder) {
        super.bindView(viewHolder);

        ItemTaskListBinding itemTaskBinding = viewHolder.itemTaskListBinding;
        itemTaskBinding.setTaskList(taskList);
    }

    //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
    public static class ViewHolder extends RecyclerView.ViewHolder {

        ItemTaskListBinding itemTaskListBinding;

        public ViewHolder(View view) {
            super(view);
            itemTaskListBinding = DataBindingUtil.bind(view);
        }
    }
}
