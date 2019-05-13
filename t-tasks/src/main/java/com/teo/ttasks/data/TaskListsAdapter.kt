package com.teo.ttasks.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.teo.ttasks.R
import com.teo.ttasks.data.model.TaskList

class TaskListsAdapter(
    context: Context,
    taskLists: List<TaskList> = emptyList()
) : ArrayAdapter<TaskList>(context, layoutResId, taskLists) {

    companion object {
        private const val layoutResId = R.layout.spinner_item_task_list
        private var layoutResDropDownId = R.layout.spinner_item_task_list_dropdown
    }

    override fun setDropDownViewResource(@LayoutRes resource: Int) {
        layoutResDropDownId = resource
        super.setDropDownViewResource(resource)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        // Get the data item for this position
        val taskList = getItem(position)!!
        // Check if an existing view is being reused, otherwise inflate the view
        val viewHolder: ViewHolder // view lookup cache stored in tag
        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResId, parent, false)
            viewHolder = ViewHolder()
            viewHolder.name = view.findViewById(R.id.taskListTitle)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        // Populate the data into the template view using the data object
        viewHolder.name.text = taskList.title
        // Return the completed view to render on screen
        return view!!
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val taskList = getItem(position)!!
        val viewHolder: ViewHolder
        if (view == null) {
            view = LayoutInflater.from(context).inflate(layoutResDropDownId, parent, false)
            viewHolder = ViewHolder()
            viewHolder.name = view.findViewById(R.id.taskListTitle)
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        viewHolder.name.text = taskList.title
        return view!!
    }

    // View lookup cache
    internal class ViewHolder {
        lateinit var name: TextView
    }
}
