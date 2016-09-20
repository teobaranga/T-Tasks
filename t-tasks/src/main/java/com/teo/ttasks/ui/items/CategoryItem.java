package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

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

    public StringHolder name;

    private List<TaskItem> mSubItems;

    private boolean mExpanded = false;

    public CategoryItem withName(String name) {
        this.name = new StringHolder(name);
        return this;
    }

    public CategoryItem withName(@StringRes int NameRes) {
        this.name = new StringHolder(NameRes);
        return this;
    }

    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }

    @Override
    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public CategoryItem withIsExpanded(boolean expanded) {
        mExpanded = expanded;
        return this;
    }

    @Override
    public List<TaskItem> getSubItems() {
        return mSubItems;
    }

    public CategoryItem withSubItems(List<TaskItem> subItems) {
        this.mSubItems = subItems;
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

        //set the text for the name
        StringHolder.applyTo(name, binding.text);
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
