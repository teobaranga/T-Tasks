package com.teo.ttasks.ui.items;

import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.utils.ViewHolderFactory;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.teo.ttasks.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView Item which represents a Date in an Order list
 */
public class CategoryItem extends AbstractItem<CategoryItem, CategoryItem.ViewHolder> implements IExpandable<CategoryItem, IItem> {

    private static final ViewHolderFactory<? extends ViewHolder> FACTORY = new ItemFactory();

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
    public int getType() {
        return R.id.category_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_category;
    }

    @Override
    public void bindView(ViewHolder viewHolder) {
        super.bindView(viewHolder);

        //set the text for the name
        StringHolder.applyTo(name, viewHolder.name);
    }

    @Override
    public ViewHolderFactory<? extends ViewHolder> getFactory() {
        return FACTORY;
    }

    /**
     * our ItemFactory implementation which creates the ViewHolder for our adapter.
     * It is highly recommended to implement a ViewHolderFactory as it is 0-1ms faster for ViewHolder creation,
     * and it is also many many times more efficient if you define custom listeners on views within your item.
     */
    protected static class ItemFactory implements ViewHolderFactory<ViewHolder> {
        public ViewHolder create(View v) {
            return new ViewHolder(v);
        }
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.material_drawer_name) TextView name;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
