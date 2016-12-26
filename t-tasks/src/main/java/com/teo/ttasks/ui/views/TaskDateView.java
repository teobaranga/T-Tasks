package com.teo.ttasks.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Date;

import static com.teo.ttasks.util.DateUtils.sdfDayName;
import static com.teo.ttasks.util.DateUtils.sdfDayNumber;

public class TaskDateView extends View {

    private static final int dayNumberSizeSp = 24;
    private static final int dayNameSizeSp = 14;

    private String dayNumber = null;
    private String dayName = null;

    private Paint dayNumberPaint;
    private Paint dayNamePaint;

    private Rect dayNumberBounds;
    private Rect dayNameBounds;

    public TaskDateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (dayNumber != null)
            drawDayNumber(canvas);
        if (dayName != null)
            drawDayName(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private void init(Context context) {
        TypedValue typedValue = new TypedValue();
        final TypedArray typedArray = context.getTheme().obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary, android.R.attr.textColorTertiary});
        final int primaryColor = typedArray.getColor(0, Color.BLACK);
        final int tertiaryColor = typedArray.getColor(1, Color.BLACK);
        typedArray.recycle();

        dayNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayNumberPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNumberSizeSp, getResources().getDisplayMetrics()));
        dayNumberPaint.setColor(primaryColor);
        dayNumberPaint.setTextAlign(Paint.Align.CENTER);
        dayNumberPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        dayNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dayNamePaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNameSizeSp, getResources().getDisplayMetrics()));
        dayNamePaint.setColor(tertiaryColor);
        dayNamePaint.setTextAlign(Paint.Align.CENTER);
        dayNamePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        String number = "33";
        dayNumberBounds = new Rect();
        dayNumberPaint.getTextBounds(number, 0, number.length(), dayNumberBounds);

        String name = "MMM";
        dayNameBounds = new Rect();
        dayNamePaint.getTextBounds(name, 0, name.length(), dayNameBounds);
    }

    private int measureHeight(int measureSpec) {
        int size = getPaddingTop() + getPaddingBottom();
        size += dayNumberBounds.bottom + dayNumberBounds.height();
        size += dayNameBounds.bottom + dayNameBounds.height();
        size += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
        return resolveSizeAndState(size, measureSpec, 0);
    }

    private int measureWidth(int measureSpec) {
        int size = getPaddingLeft() + getPaddingRight();
        size += Math.max(dayNumberBounds.left + dayNumberBounds.width(), dayNameBounds.left + dayNameBounds.width());
        return resolveSizeAndState(size, measureSpec, 0);
    }

    public void setDate(@Nullable Date date) {
        if (date == null) {
            dayName = null;
            dayNumber = null;
        } else {
            dayName = sdfDayName.format(date);
            dayNumber = sdfDayNumber.format(date);
        }
        invalidate();
    }

    private void drawDayNumber(Canvas canvas) {
        float x = getPaddingLeft() + getWidth() / 2;
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        float y = getPaddingTop() + dayNumberBounds.bottom + dayNumberBounds.height();
        canvas.drawText(dayNumber, x, y, dayNumberPaint);
    }

    private void drawDayName(Canvas canvas) {
        float x = getPaddingLeft() + getWidth() / 2;
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        float y = getPaddingTop() + dayNumberBounds.bottom + dayNumberBounds.height();
        y += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        y += dayNameBounds.bottom + dayNameBounds.height();
        canvas.drawText(dayName, x, y, dayNamePaint);
    }
}
