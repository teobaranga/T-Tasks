package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Animatable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.teo.ttasks.R;
import com.teo.ttasks.databinding.ItemCategoryBinding;

import java.util.List;

/**
 * RecyclerView Item which represents a Date in an Order list
 */
public class CategoryItem extends AbstractItem<CategoryItem, CategoryItem.ViewHolder> implements IExpandable<CategoryItem, TaskItem> {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

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
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
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

    public CategoryItem withSubItems(List<TaskItem> subItems) {
        this.subItems = subItems;
        return this;
    }

    @Override
    public boolean isAutoExpanding() {
        return true;
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
    public void bindView(ViewHolder viewHolder, List payloads) {
        super.bindView(viewHolder, payloads);

        final ItemCategoryBinding binding = viewHolder.itemCategoryBinding;
        arrowView = binding.arrow;

        // set the text for the title
        StringHolder.applyTo(title, binding.text);
    }

    /**
     * Switch the arrow drawable to point up or down, depending on the expanded state of this item.
     *
     * @param animate animate the arrow (rotate it)
     */
    public void toggleArrow(boolean animate) {
        int res = !expanded ? R.drawable.anim_ic_more_to_less_24dp : R.drawable.anim_ic_less_to_more_24dp;
        arrowView.setImageResource(res);
        if (animate)
            ((Animatable) arrowView.getDrawable()).start();
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
