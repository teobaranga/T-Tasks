package com.teo.ttasks.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.teo.ttasks.R
import com.teo.ttasks.util.DateUtils
import org.threeten.bp.ZonedDateTime
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * See [com.teo.ttasks.R.styleable.TaskDateView]
 *
 * @attr ref com.teo.ttasks.R.styleable.TaskDateView_dueDate
 */
class TaskDateView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        private const val datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        private const val dayNumberSizeSp = 24f
        private const val dayNameSizeSp = 14f
        private val dateFormat: DateFormat

        init {
            dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private var dayNumber: String? = null
    private var dayName: String? = null

    private lateinit var dayNumberPaint: Paint
    private lateinit var dayNamePaint: Paint

    private lateinit var dayNumberBounds: Rect
    private lateinit var dayNameBounds: Rect

    /**
     * The date shown in this view. Can be a due date or a completion date.
     */
    var date: ZonedDateTime? = null
        set(value) {
            if (value == null) {
                dayName = null
                dayNumber = null
            } else {
                dayName = value.format(DateUtils.formatterDayName)
                dayNumber = value.format(DateUtils.formatterDayNumber)
            }
            invalidate()
            requestLayout()
        }

    /**
     * The date shown in this view represented as a string the format specified in [datePattern].
     * Used only for preview in Android Studio.
     */
    var dateString: String? = null
        set(value) {
            date = if (value != null) ZonedDateTime.parse(value) else null
        }

    init {
        init(context, attrs)
    }

    override fun onDraw(canvas: Canvas) {
        if (dayNumber != null) {
            drawDayNumber(canvas)
        }
        if (dayName != null) {
            drawDayName(canvas)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val typedValue = TypedValue()
        val typedArray = context.theme.obtainStyledAttributes(
            typedValue.data,
            intArrayOf(android.R.attr.textColorPrimary, android.R.attr.textColorTertiary)
        )
        val primaryColor = typedArray.getColor(0, Color.BLACK)
        val tertiaryColor = typedArray.getColor(1, Color.BLACK)
        typedArray.recycle()

        // TODO fix the paint remaining constant on a night mode change
        dayNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNumberSizeSp, resources.displayMetrics)
            color = primaryColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        dayNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNameSizeSp, resources.displayMetrics)
            color = tertiaryColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Compute the maximum bounds of each view element individually
        val dateDay = "33"
        dayNumberBounds = Rect()
        dayNumberPaint.getTextBounds(dateDay, 0, dateDay.length, dayNumberBounds)

        val dateMonth = "MMM"
        dayNameBounds = Rect()
        dayNamePaint.getTextBounds(dateMonth, 0, dateMonth.length, dayNameBounds)

        if (isInEditMode) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TaskDateView, 0, 0)
            try {
                dateString = a.getString(R.styleable.TaskDateView_date)
            } finally {
                a.recycle()
            }
        }
    }

    private fun measureHeight(measureSpec: Int): Int {
        var size = paddingTop + paddingBottom
        size += dayNumberBounds.bottom + dayNumberBounds.height()
        size += dayNameBounds.bottom + dayNameBounds.height()
        size += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, resources.displayMetrics).toInt()
        return View.resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureWidth(measureSpec: Int): Int {
        var size = paddingLeft + paddingRight
        size += Math.max(dayNumberBounds.left + dayNumberBounds.width(), dayNameBounds.left + dayNameBounds.width())
        return View.resolveSizeAndState(size, measureSpec, 0)
    }

    private fun drawDayNumber(canvas: Canvas) {
        val x = (paddingLeft + width / 2).toFloat()
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        val y = (paddingTop + dayNumberBounds.bottom + dayNumberBounds.height()).toFloat()
        canvas.drawText(dayNumber!!, x, y, dayNumberPaint)
    }

    private fun drawDayName(canvas: Canvas) {
        val x = (paddingLeft + width / 2).toFloat()
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        var y = (paddingTop + dayNumberBounds.bottom + dayNumberBounds.height()).toFloat()
        y += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)
        y += (dayNameBounds.bottom + dayNameBounds.height()).toFloat()
        canvas.drawText(dayName!!, x, y, dayNamePaint)
    }
}
