package com.teo.ttasks.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(context: Context, attrs: AttributeSet?) : RecyclerView.ItemDecoration() {

    private var mOrientation = -1
    private val mDivider: Drawable?
    private val mShowFirstDivider = false
    private val mShowLastDivider = false
    private val mDividerWidth: Int

    init {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.listDivider))
        mDivider = a.getDrawable(0)
        a.recycle()
        mDividerWidth = convertDpToPixel(0.5f, context)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        if (mDivider == null) {
            return
        }

        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION || position == 0 && !mShowFirstDivider) {
            return
        }

        if (mOrientation == -1)
            getOrientation(parent)

        if (mOrientation == LinearLayoutManager.VERTICAL) {
            outRect.top = mDividerWidth
            if (mShowLastDivider && position == state.itemCount - 1) {
                outRect.bottom = outRect.top
            }
        } else {
            outRect.left = mDividerWidth
            if (mShowLastDivider && position == state.itemCount - 1) {
                outRect.right = outRect.left
            }
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mDivider == null) {
            super.onDrawOver(c, parent, state)
            return
        }

        // Initialization needed to avoid compiler warning
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        val orientation = if (mOrientation != -1) mOrientation else getOrientation(parent)
        val childCount = parent.childCount

        if (orientation == LinearLayoutManager.VERTICAL) {
            left = parent.paddingLeft
            right = parent.width - parent.paddingRight
        } else { //horizontal
            top = parent.paddingTop
            bottom = parent.height - parent.paddingBottom
        }

        for (i in (if (mShowFirstDivider) 0 else 1) until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            if (orientation == LinearLayoutManager.VERTICAL) {
                top = child.top - params.topMargin - mDividerWidth
                bottom = top + mDividerWidth
            } else { //horizontal
                left = child.left - params.leftMargin
                right = left + mDividerWidth
            }
            mDivider.setBounds(left, top, right, bottom)
            mDivider.draw(c)
        }

        // show last divider
        if (mShowLastDivider && childCount > 0) {
            val child = parent.getChildAt(childCount - 1)
            if (parent.getChildAdapterPosition(child) == state.itemCount - 1) {
                val params = child.layoutParams as RecyclerView.LayoutParams
                if (orientation == LinearLayoutManager.VERTICAL) {
                    top = child.bottom + params.bottomMargin
                    bottom = top + mDividerWidth
                } else { // horizontal
                    left = child.right + params.rightMargin
                    right = left + mDividerWidth
                }
                mDivider.setBounds(left, top, right, bottom)
                mDivider.draw(c)
            }
        }
    }

    private fun getOrientation(parent: RecyclerView): Int {
        if (mOrientation == -1) {
            if (parent.layoutManager is LinearLayoutManager) {
                val layoutManager = parent.layoutManager as LinearLayoutManager
                mOrientation = layoutManager.orientation
            } else {
                throw IllegalStateException("DividerItemDecoration can only be used with a LinearLayoutManager.")
            }
        }
        return mOrientation
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.

     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * *
     * @param context Context to get resources and device specific display metrics
     * *
     * @return A float value to represent px equivalent to dp depending on device density
     */
    private fun convertDpToPixel(dp: Float, context: Context): Int {
        val r = context.resources
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.displayMetrics))
    }
}
