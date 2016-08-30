package com.teo.ttasks.ui.items;

import android.databinding.DataBindingUtil;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.teo.ttasks.R;
import com.teo.ttasks.databinding.ItemCategoryBinding;

import java.util.List;

/**
 * RecyclerView Item which represents a Date in an Order list
 */
public class CategoryItem extends AbstractItem<CategoryItem, CategoryItem.ViewHolder> implements IExpandable<CategoryItem, IItem> {

    public StringHolder name;

    private List<IItem> mSubItems;
    private boolean mExpanded = false;

    public CategoryItem withName(String Name) {
        this.name = new StringHolder(Name);
        return this;
    }

    public CategoryItem withName(@StringRes int NameRes) {
        this.name = new StringHolder(NameRes);
        return this;
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
    public List<IItem> getSubItems() {
        return mSubItems;
    }

    public CategoryItem withSubItems(List<IItem> subItems) {
        this.mSubItems = subItems;
        return this;
    }

    @Override
    public boolean isAutoExpanding() {
        return false;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ItemCategoryBinding itemCategoryBinding;

        public ViewHolder(View view) {
            super(view);
            itemCategoryBinding = DataBindingUtil.bind(view);
        }
    }
}
