package com.teo.ttasks.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    int mOrientation = -1;
    private Drawable mDivider;
    private boolean mShowFirstDivider = false;
    private boolean mShowLastDivider = false;
    private int mDividerWidth;

    public DividerItemDecoration(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.listDivider});
        mDivider = a.getDrawable(0);
        a.recycle();
        mDividerWidth = convertDpToPixel(0.5f, context);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (mDivider == null) {
            return;
        }

        int position = parent.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION || (position == 0 && !mShowFirstDivider)) {
            return;
        }

        if (mOrientation == -1)
            getOrientation(parent);

        if (mOrientation == LinearLayoutManager.VERTICAL) {
            outRect.top = mDividerWidth;
            if (mShowLastDivider && position == (state.getItemCount() - 1)) {
                outRect.bottom = outRect.top;
            }
        } else {
            outRect.left = mDividerWidth;
            if (mShowLastDivider && position == (state.getItemCount() - 1)) {
                outRect.right = outRect.left;
            }
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mDivider == null) {
            super.onDrawOver(c, parent, state);
            return;
        }

        // Initialization needed to avoid compiler warning
        int left = 0, right = 0, top = 0, bottom = 0;
        int orientation = mOrientation != -1 ? mOrientation : getOrientation(parent);
        int childCount = parent.getChildCount();

        if (orientation == LinearLayoutManager.VERTICAL) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
        } else { //horizontal
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
        }

        for (int i = mShowFirstDivider ? 0 : 1; i < childCount; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            if (orientation == LinearLayoutManager.VERTICAL) {
                top = child.getTop() - params.topMargin - mDividerWidth;
                bottom = top + mDividerWidth;
            } else { //horizontal
                left = child.getLeft() - params.leftMargin;
                right = left + mDividerWidth;
            }
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }

        // show last divider
        if (mShowLastDivider && childCount > 0) {
            View child = parent.getChildAt(childCount - 1);
            if (parent.getChildAdapterPosition(child) == (state.getItemCount() - 1)) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
                if (orientation == LinearLayoutManager.VERTICAL) {
                    top = child.getBottom() + params.bottomMargin;
                    bottom = top + mDividerWidth;
                } else { // horizontal
                    left = child.getRight() + params.rightMargin;
                    right = left + mDividerWidth;
                }
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    private int getOrientation(RecyclerView parent) {
        if (mOrientation == -1) {
            if (parent.getLayoutManager() instanceof LinearLayoutManager) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
                mOrientation = layoutManager.getOrientation();
            } else {
                throw new IllegalStateException("DividerItemDecoration can only be used with a LinearLayoutManager.");
            }
        }
        return mOrientation;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return Math.round(dp * (metrics.densityDpi / 160f));
    }
}
