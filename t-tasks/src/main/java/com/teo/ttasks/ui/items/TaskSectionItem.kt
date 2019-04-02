package com.teo.ttasks.ui.items

import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teo.ttasks.R
import com.teo.ttasks.data.model.Task
import com.teo.ttasks.databinding.ItemTaskSectionBinding
import com.teo.ttasks.ui.SpacesItemDecoration
import com.teo.ttasks.ui.views.TasksContainerView
import com.teo.ttasks.util.DateUtils
import com.teo.ttasks.util.dpToPx
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import org.threeten.bp.ZonedDateTime

class TaskSectionItem(
    @DrawableRes private val iconRes: Int,
    @StringRes private val sectionTitleRes: Int,
    private val tasks: List<Task>,
    dateType: DateType
) : AbstractFlexibleItem<TaskSectionItem.ViewHolder>() {

    enum class DateType(val accessFunction: Task.() -> ZonedDateTime?) {
        COMPLETED({ completedDate }),
        DUE({ dueDate });
    }

    private val sortDate: Task.() -> ZonedDateTime? = dateType.accessFunction

    /**
     * Map from Day to tasks in that day
     */
    private val taskDateMap: Map<String?, List<Task>> by lazy {
        val map = mutableMapOf<String?, MutableList<Task>>()
        for (task in tasks) {
            val day = task.sortDate()?.format(DateUtils.formatterDay)
            when (day) {
                in map -> map[day]!!.add(task)
                else -> map[day] = mutableListOf(task)
            }
        }
        return@lazy map
    }

    private inner class TasksAdapter : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

        val taskDateMapAsSequence = taskDateMap.asSequence()

        private inner class ViewHolder(val tasksContainerView: TasksContainerView) :
            RecyclerView.ViewHolder(tasksContainerView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tasksContainerView = TasksContainerView(parent.context)
            tasksContainerView.layoutParams =
                ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            return ViewHolder(tasksContainerView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val previousContainerDate = if (position > 0) {
                taskDateMapAsSequence.elementAt(position - 1).value[0].sortDate()
            } else null
            with(holder.tasksContainerView) {
                val taskList = taskDateMapAsSequence.elementAt(position).value
                showMonth = previousContainerDate?.month != taskList[0].sortDate()?.month
                date = taskList[0].sortDate()
                tasks = taskList
            }
        }

        override fun getItemCount() = taskDateMap.size
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>?
    ) {
        holder.binding.apply {
            iconTitle.setImageResource(iconRes)
            sectionTitle.setText(sectionTitleRes)
            sortType.setText(R.string.sort_mode_date)
            sortDirection.setImageResource(com.teo.ttasks.R.drawable.ic_arrow_downward_24dp)
            taskList.adapter = TasksAdapter()
        }
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): TaskSectionItem.ViewHolder {
        return ViewHolder(view, adapter).apply {
            with(binding.taskList) {
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SpacesItemDecoration(4.dpToPx()))
            }
        }
    }

    override fun getLayoutRes(): Int {
        return R.layout.item_task_section
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TaskSectionItem

        if (iconRes != other.iconRes) return false
        if (sectionTitleRes != other.sectionTitleRes) return false
        if (tasks != other.tasks) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iconRes
        result = 31 * result + sectionTitleRes
        result = 31 * result + tasks.hashCode()
        return result
    }

    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<out IFlexible<*>>) :
        FlexibleViewHolder(view, adapter) {
        val binding = ItemTaskSectionBinding.bind(view)!!
    }
}
