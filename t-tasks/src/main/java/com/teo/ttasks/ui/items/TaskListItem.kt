package com.teo.ttasks.ui.items

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.teo.ttasks.R
import com.teo.ttasks.data.model.TaskList
import com.teo.ttasks.databinding.ItemTaskListBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import timber.log.Timber

class TaskListItem(private val taskList: TaskList,
                   private val taskCount: Long) : AbstractFlexibleItem<TaskListItem.ViewHolder>() {

    init {
        Timber.d("count %d", taskCount)
    }

    val title: String
        get() = taskList.title

    val id: String
        get() = taskList.id

    override fun getLayoutRes(): Int {
        return R.layout.item_task_list
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                viewHolder: ViewHolder, position: Int, payloads: MutableList<Any?>) {
        val itemTaskBinding = viewHolder.itemTaskListBinding
        val context = itemTaskBinding.root.context
        val taskCountInt = taskCount.toInt()

        itemTaskBinding.taskListTitle.text = taskList.title
        itemTaskBinding.taskListSize.text =
                if (taskCountInt > 0) {
                    context.resources.getQuantityString(R.plurals.task_list_size, taskCountInt, taskCountInt)
                } else {
                    context.getString(R.string.empty_task_list)
                }
        itemTaskBinding.deleteTaskList.setOnClickListener(viewHolder)
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    override fun equals(other: Any?): Boolean = other is TaskListItem && id == other.id

    override fun hashCode(): Int {
        var result = taskList.hashCode()
        result = 31 * result + taskCount.hashCode()
        return result
    }

    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<out IFlexible<*>>) : FlexibleViewHolder(view, adapter) {
        var itemTaskListBinding = ItemTaskListBinding.bind(view)
    }
}
