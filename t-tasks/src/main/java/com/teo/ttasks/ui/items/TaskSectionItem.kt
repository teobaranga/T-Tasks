package com.teo.ttasks.ui.items

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.teo.ttasks.R
import com.teo.ttasks.databinding.ItemTaskSectionBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder

class TaskSectionItem(
    @DrawableRes private val iconRes: Int,
    @StringRes private val sectionTitleRes: Int
) : AbstractFlexibleItem<TaskSectionItem.ViewHolder>() {

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>?
    ) {
        holder.binding.apply {
            iconTitle.setImageResource(iconRes)
            sectionTitle.setText(sectionTitleRes)
            sortType.setText("Date")
            sortDirection.setImageResource(R.drawable.ic_arrow_downward_24dp)
        }
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): TaskSectionItem.ViewHolder {
        return ViewHolder(view, adapter)
    }

    override fun getLayoutRes(): Int {
        return R.layout.item_task_section
    }

    class ViewHolder internal constructor(view: View, adapter: FlexibleAdapter<out IFlexible<*>>) :
        FlexibleViewHolder(view, adapter) {
        val binding = ItemTaskSectionBinding.bind(view)!!
    }
}
