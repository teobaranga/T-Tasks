package com.teo.ttasks.ui;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import com.teo.ttasks.R;

public class OverflowMenuButton extends AppCompatImageButton {

    public OverflowMenuButton(Context context) {
        super(context, null, R.attr.actionOverflowButtonStyle);
    }

    public OverflowMenuButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverflowMenuButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Set up the hotspot bounds to be centered on the image.
        final Drawable d = getDrawable();
        final Drawable bg = getBackground();
        if (d != null && bg != null) {
            final Rect bounds = d.getBounds();
            final int height = bottom - top;
            final int offset = (height - bounds.width()) / 2;
            final int hotspotLeft = bounds.left + offset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bg.setHotspotBounds(hotspotLeft, 0, bounds.right, height);
            }
        }
    }
}
