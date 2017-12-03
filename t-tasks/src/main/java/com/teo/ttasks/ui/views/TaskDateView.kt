package com.teo.ttasks.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.teo.ttasks.util.DateUtils.Companion.sdfDayName
import com.teo.ttasks.util.DateUtils.Companion.sdfDayNumber
import java.util.*

class TaskDateView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var dayNumber: String? = null
    private var dayName: String? = null

    private var dayNumberPaint: Paint? = null
    private var dayNamePaint: Paint? = null

    private var dayNumberBounds: Rect? = null
    private var dayNameBounds: Rect? = null

    init {
        init(context)
    }

    override fun onDraw(canvas: Canvas) {
        if (dayNumber != null)
            drawDayNumber(canvas)
        if (dayName != null)
            drawDayName(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun init(context: Context) {
        val typedValue = TypedValue()
        val typedArray = context.theme.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorPrimary, android.R.attr.textColorTertiary))
        val primaryColor = typedArray.getColor(0, Color.BLACK)
        val tertiaryColor = typedArray.getColor(1, Color.BLACK)
        typedArray.recycle()

        dayNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dayNumberPaint!!.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNumberSizeSp.toFloat(), resources.displayMetrics)
        dayNumberPaint!!.color = primaryColor
        dayNumberPaint!!.textAlign = Paint.Align.CENTER
        dayNumberPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        dayNamePaint!!.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNameSizeSp.toFloat(), resources.displayMetrics)
        dayNamePaint!!.color = tertiaryColor
        dayNamePaint!!.textAlign = Paint.Align.CENTER
        dayNamePaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val number = "33"
        dayNumberBounds = Rect()
        dayNumberPaint!!.getTextBounds(number, 0, number.length, dayNumberBounds)

        val name = "MMM"
        dayNameBounds = Rect()
        dayNamePaint!!.getTextBounds(name, 0, name.length, dayNameBounds)
    }

    private fun measureHeight(measureSpec: Int): Int {
        var size = paddingTop + paddingBottom
        size += dayNumberBounds!!.bottom + dayNumberBounds!!.height()
        size += dayNameBounds!!.bottom + dayNameBounds!!.height()
        size += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
        return View.resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureWidth(measureSpec: Int): Int {
        var size = paddingLeft + paddingRight
        size += Math.max(dayNumberBounds!!.left + dayNumberBounds!!.width(), dayNameBounds!!.left + dayNameBounds!!.width())
        return View.resolveSizeAndState(size, measureSpec, 0)
    }

    fun setDate(date: Date?) {
        if (date == null) {
            dayName = null
            dayNumber = null
        } else {
            dayName = sdfDayName.format(date)
            dayNumber = sdfDayNumber.format(date)
        }
        invalidate()
    }

    private fun drawDayNumber(canvas: Canvas) {
        val x = (paddingLeft + width / 2).toFloat()
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        val y = (paddingTop + dayNumberBounds!!.bottom + dayNumberBounds!!.height()).toFloat()
        canvas.drawText(dayNumber!!, x, y, dayNumberPaint!!)
    }

    private fun drawDayName(canvas: Canvas) {
        val x = (paddingLeft + width / 2).toFloat()
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        var y = (paddingTop + dayNumberBounds!!.bottom + dayNumberBounds!!.height()).toFloat()
        y += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
        y += (dayNameBounds!!.bottom + dayNameBounds!!.height()).toFloat()
        canvas.drawText(dayName!!, x, y, dayNamePaint!!)
    }

    companion object {

        private const val dayNumberSizeSp = 24
        private const val dayNameSizeSp = 14
    }
}
