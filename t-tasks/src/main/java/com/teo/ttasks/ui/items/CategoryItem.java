package com.teo.ttasks.ui.items;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.StringRes;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.mikepenz.fastadapter.commons.items.AbstractExpandableItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.teo.ttasks.R;
import com.teo.ttasks.databinding.ItemCategoryBinding;

import java.util.List;

/**
 * RecyclerView Item which represents a Date in an Order list
 */
public class CategoryItem extends AbstractExpandableItem<CategoryItem, CategoryItem.ViewHolder, TaskItem> {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

    private static final TypedValue typedValue = new TypedValue();

    public StringHolder title;

    private List<TaskItem> subItems;

    private boolean expanded;

    private ImageView arrowView;

    public CategoryItem withTitle(String title) {
        this.title = new StringHolder(title);
        return this;
    }

    public CategoryItem withTitle(@StringRes int titleRes) {
        this.title = new StringHolder(titleRes);
        return this;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public CategoryItem withIsExpanded(boolean expanded) {
        this.expanded = expanded;
        return this;
    }

    @Override
    public List<TaskItem> getSubItems() {
        return subItems;
    }

    @Override
    public boolean isAutoExpanding() {
        return true;
    }

    public CategoryItem withSubItems(List<TaskItem> subItems) {
        this.subItems = subItems;
        return this;
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
    public void bindView(ViewHolder viewHolder, List<Object> payloads) {
        super.bindView(viewHolder, payloads);

        final ItemCategoryBinding binding = viewHolder.itemCategoryBinding;
        arrowView = binding.arrow;

        // set the text for the title
        StringHolder.applyTo(title, binding.text);
    }

    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }

    /**
     * Switch the arrow drawable to point up or down, depending on the expanded state of this item.
     *
     * @param animate animate the arrow (rotate it)
     */
    public void toggleArrow(boolean animate) {
        final int res = !expanded ? R.drawable.anim_ic_more_to_less_24dp : R.drawable.anim_ic_less_to_more_24dp;

        // A little hack is necessary since the animated vectors don't seem to be updated immediately
        // after night mode has changed and there seems to be no way to force a refresh of the drawable.
        // Therefore, the only way to have the arrow display the right color all the time is to explicitly
        // set the tint on the drawable
        final AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(arrowView.getContext(), res);
        final int color = getThemeAttrColor(arrowView.getContext(), typedValue, R.attr.colorControlNormal);
        //noinspection ConstantConditions
        drawable.setTint(color);

        arrowView.setImageDrawable(drawable);

        // Start the animation if requested
        if (animate)
            drawable.start();
    }

    private int getThemeAttrColor(Context context, TypedValue typedValue, int attr) {
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

    private static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ItemCategoryBinding itemCategoryBinding;

        ViewHolder(View view) {
            super(view);
            itemCategoryBinding = DataBindingUtil.bind(view);
        }
    }
}
