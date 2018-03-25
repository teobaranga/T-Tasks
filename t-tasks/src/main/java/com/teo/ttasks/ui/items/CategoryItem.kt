package com.teo.ttasks.ui.items

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import com.teo.ttasks.R
import com.teo.ttasks.databinding.ItemCategoryBinding
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.ExpandableViewHolder

/**
 * RecyclerView Item which represents a Date in an Order list
 */
class CategoryItem(private val titleExpanded: String,
                   private val titleCollapsed: String)
    : AbstractExpandableItem<CategoryItem.ViewHolder, TaskItem>() {

    constructor(title: String): this(title, title)

    private var viewHolder: ViewHolder? = null

    override fun getLayoutRes(): Int = R.layout.item_category

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    /**
     * Switch the arrow drawable to point up or down, depending on the expanded state of this item.

     * @param animate animate the arrow (rotate it)
     */
    private fun toggleArrow(animate: Boolean) {
        if (viewHolder == null) {
            return
        }

        val itemCategoryBinding = viewHolder!!.itemCategoryBinding
        val res = if (!isExpanded) R.drawable.anim_ic_more_to_less_24dp else R.drawable.anim_ic_less_to_more_24dp

        // A little hack is necessary since the animated vectors don't seem to be updated immediately
        // after night mode has changed and there seems to be no way to force a refresh of the drawable.
        // Therefore, the only way to have the arrow display the right color all the time is to explicitly
        // set the tint on the drawable
        val drawable = AnimatedVectorDrawableCompat.create(itemCategoryBinding.arrow.context, res)!!
        val color = getThemeAttrColor(itemCategoryBinding.arrow.context, typedValue, R.attr.colorControlNormal)
        drawable.setTint(color)

        itemCategoryBinding.arrow.setImageDrawable(drawable)

        // Start the animation if requested
        if (animate) {
            drawable.start()
        }
    }

    private fun getThemeAttrColor(context: Context, typedValue: TypedValue, attr: Int): Int {
        if (context.theme.resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_INT && typedValue.type <= TypedValue.TYPE_LAST_INT) {
                return typedValue.data
            } else if (typedValue.type == TypedValue.TYPE_STRING) {
                return ResourcesCompat.getColor(context.resources, typedValue.resourceId, context.theme)
            }
        }
        return 0
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>, viewHolder: ViewHolder, position: Int, payloads: MutableList<Any?>?) {
        this.viewHolder = viewHolder
        viewHolder.itemCategoryBinding.text.text =
                if (isExpanded) String.format(titleExpanded, subItemsCount)
                else String.format(titleCollapsed, subItemsCount)
        toggleArrow(true)
    }

    override fun equals(other: Any?) =
            other is CategoryItem
                    && titleExpanded == other.titleExpanded

    override fun hashCode() = titleExpanded.hashCode()

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : ExpandableViewHolder(view, adapter) {
        val itemCategoryBinding: ItemCategoryBinding = DataBindingUtil.bind(view)

        override fun shouldNotifyParentOnClick(): Boolean = true
    }

    companion object {
        private val typedValue = TypedValue()
    }
}
