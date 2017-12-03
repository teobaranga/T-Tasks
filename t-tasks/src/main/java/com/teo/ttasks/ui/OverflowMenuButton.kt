package com.teo.ttasks.ui

import android.content.Context
import android.os.Build
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import com.teo.ttasks.R

class OverflowMenuButton : AppCompatImageButton {

    constructor(context: Context) : super(context, null, R.attr.actionOverflowButtonStyle)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Set up the hotspot bounds to be centered on the image.
        val d = drawable
        val bg = background
        if (d != null && bg != null) {
            val bounds = d.bounds
            val height = bottom - top
            val offset = (height - bounds.width()) / 2
            val hotspotLeft = bounds.left + offset
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bg.setHotspotBounds(hotspotLeft, 0, bounds.right, height)
            }
        }
    }
}
