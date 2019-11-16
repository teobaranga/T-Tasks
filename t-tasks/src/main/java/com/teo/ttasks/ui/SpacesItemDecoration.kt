package com.teo.ttasks.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildLayoutPosition(view)
        when (position) {
            0 -> outRect.bottom = space
            parent.childCount -> outRect.top = space
            else -> {
                outRect.top = space
                outRect.bottom = space
            }
        }
    }
}