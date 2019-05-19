package com.teo.ttasks.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.StyleableRes
import com.teo.ttasks.R
import com.teo.ttasks.util.DateUtils
import org.threeten.bp.ZonedDateTime
import kotlin.math.roundToInt

private const val dayNumberSizeSp = 24f

private const val dayNameSizeSp = 16f

@StyleableRes
private const val primaryColorIdx = 0

@StyleableRes
private const val tertiaryColorIdx = 1

/**
 * See [R.styleable.TaskDateView]
 *
 * @attr ref R.styleable.TaskDateView_dueDate
 */
class TaskDateView : View {

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(context, attrs)
    }

    private var dayNumber: String? = null
    private var dayName: String? = null

    private lateinit var dayOfMonthPaint: Paint
    private lateinit var dayOfWeekPaint: Paint

    private var dayOfMonthWidth = 0
    private var dayOfMonthHeight = 0

    private var dayOfWeekWidth = 0
    private var dayOfWeekHeight = 0

    /**
     * The date shown in this view. Can be a due date or a completion date.
     */
    var date: ZonedDateTime? = null
        set(value) {
            val equals = field == value
            if (value == null) {
                dayName = null
                dayNumber = null
            } else {
                dayName = value.format(DateUtils.formatterDayName)
                dayNumber = value.format(DateUtils.formatterDayNumber)
            }
            field = value
            if (!equals) {
                invalidate()
                requestLayout()
            }
        }

    override fun onDraw(canvas: Canvas) {
        dayNumber?.let { drawDayOfMonth(canvas, it) }
        dayName?.let { drawDayOfWeek(canvas, it) }
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

        val primaryColor = typedArray.getColor(primaryColorIdx, Color.BLACK)
        val tertiaryColor = typedArray.getColor(tertiaryColorIdx, Color.BLACK)
        typedArray.recycle()

        // Compute the maximum bounds of each view element individually
        val dateDay = "33"

        val dateMonth = "MMM"

        // TODO fix the paint remaining constant on a night mode change
        dayOfMonthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNumberSizeSp, resources.displayMetrics)
            color = primaryColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

            dayOfMonthWidth = measureText(dateDay).roundToInt()
            dayOfMonthHeight = -fontMetrics.ascent.roundToInt()
        }

        dayOfWeekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, dayNameSizeSp, resources.displayMetrics)
            color = tertiaryColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            dayOfWeekWidth = measureText(dateMonth).roundToInt()
            dayOfWeekHeight = -fontMetrics.ascent.roundToInt()
        }

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TaskDateView, 0, 0)
        try {
            date = a.getString(R.styleable.TaskDateView_date)?.let { ZonedDateTime.parse(it) }
        } finally {
            a.recycle()
        }
    }

    private fun measureHeight(measureSpec: Int): Int {
        val dayOfMonthHeight = if (dayNumber != null) dayOfMonthHeight else 0
        val dayOfWeekHeight = if (dayName != null) dayOfWeekHeight else 0
        val size = paddingTop + paddingBottom + dayOfMonthHeight + dayOfWeekHeight
        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun measureWidth(measureSpec: Int): Int {
        val dayOfMonthWidth = if (dayNumber != null) dayOfMonthWidth else 0
        val dayOfWeekWidth = if (dayName != null) dayOfWeekWidth else 0
        val size = paddingLeft + paddingRight + Math.max(dayOfMonthWidth, dayOfWeekWidth)
        return resolveSizeAndState(size, measureSpec, 0)
    }

    private fun drawDayOfMonth(canvas: Canvas, dayOfMonth: String) {
        val x = paddingLeft + Math.max(dayOfMonthWidth, dayOfWeekWidth) / 2
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        val y = paddingTop + dayOfMonthHeight
        canvas.drawText(dayOfMonth, x.toFloat(), y.toFloat(), dayOfMonthPaint)
    }

    private fun drawDayOfWeek(canvas: Canvas, dayOfWeek: String) {
        val x = paddingLeft + Math.max(dayOfMonthWidth, dayOfWeekWidth) / 2
        //the y coordinate marks the bottom of the text, so we need to factor in the height
        val y = paddingTop + dayOfMonthHeight + dayOfWeekHeight
        canvas.drawText(dayOfWeek, x.toFloat(), y.toFloat(), dayOfWeekPaint)
    }
}
