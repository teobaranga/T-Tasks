package com.teo.ttasks.ui.items;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.TypedValue;
import android.view.View;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.teo.ttasks.R;
import com.teo.ttasks.databinding.ItemCategoryBinding;

import java.util.List;

/**
 * RecyclerView Item which represents a Date in an Order list
 */
public class CategoryItem<Parent extends IItem & IExpandable, SubItem extends IItem & ISubItem> extends AbstractExpandableItem<CategoryItem<Parent, SubItem>, CategoryItem.ViewHolder, TaskItem> {

    private ViewHolder viewHolder;

    public StringHolder title;

    public CategoryItem<Parent, SubItem> withTitle(String title) {
        this.title = new StringHolder(title);
        return this;
    }

    public CategoryItem<Parent, SubItem> withTitle(@StringRes int titleRes) {
        this.title = new StringHolder(titleRes);
        return this;
    }

    @Override
    public CategoryItem<Parent, SubItem> withIsExpanded(boolean expanded) {
        return (CategoryItem<Parent, SubItem>) super.withIsExpanded(expanded);
    }

    @Override
    public int getType() {
        return R.id.category_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_category;
    }

    @Override
    public ViewHolder getViewHolder(@NonNull View view) {
        viewHolder = new ViewHolder(view);
        return viewHolder;
    }
    public void toggleArrow(boolean animate) {
        if (viewHolder != null) {
            viewHolder.toggleArrow(animate);
        }
    }

    static class ViewHolder extends FastAdapter.ViewHolder<CategoryItem> {

        private static final TypedValue typedValue = new TypedValue();

        private final ItemCategoryBinding itemCategoryBinding;
        private CategoryItem categoryItem;

        ViewHolder(View view) {
            super(view);
            itemCategoryBinding = DataBindingUtil.bind(view);
        }

        @Override
        public void bindView(@NonNull CategoryItem item, @NonNull List<Object> payloads) {
            categoryItem = item;
            StringHolder.applyTo(item.title, itemCategoryBinding.text);
        }

        @Override
        public void unbindView(@NonNull CategoryItem item) {
            categoryItem = null;
            StringHolder.applyTo(null, itemCategoryBinding.text);
        }

        /**
         * Switch the arrow drawable to point up or down, depending on the expanded state of this item.
         *
         * @param animate animate the arrow (rotate it)
         */
        void toggleArrow(boolean animate) {
            if (categoryItem == null) {
                return;
            }

            final int res = !categoryItem.isExpanded() ? R.drawable.anim_ic_more_to_less_24dp : R.drawable.anim_ic_less_to_more_24dp;

            // A little hack is necessary since the animated vectors don't seem to be updated immediately
            // after night mode has changed and there seems to be no way to force a refresh of the drawable.
            // Therefore, the only way to have the arrow display the right color all the time is to explicitly
            // set the tint on the drawable
            final AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(itemCategoryBinding.arrow.getContext(), res);
            final int color = getThemeAttrColor(itemCategoryBinding.arrow.getContext(), typedValue, R.attr.colorControlNormal);
            drawable.setTint(color);

            itemCategoryBinding.arrow.setImageDrawable(drawable);

            // Start the animation if requested
            if (animate) {
                drawable.start();
            }
        }

        private static int getThemeAttrColor(Context context, TypedValue typedValue, int attr) {
            if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
                if (typedValue.type >= TypedValue.TYPE_FIRST_INT
                        && typedValue.type <= TypedValue.TYPE_LAST_INT) {
                    return typedValue.data;
                } else if (typedValue.type == TypedValue.TYPE_STRING) {
                    return ResourcesCompat.getColor(context.getResources(), typedValue.resourceId, context.getTheme());
                }
            }
            return 0;
        }
    }
}
