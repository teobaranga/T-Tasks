package com.teo.ttasks.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.teo.ttasks.R
import com.teo.ttasks.util.DateUtils
import kotlinx.android.synthetic.main.view_task_date.view.*
import org.threeten.bp.ZonedDateTime

/**
 * See [R.styleable.TaskDateView]
 *
 * @attr ref R.styleable.TaskDateView_dueDate
 */
class TaskDateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * The date shown in this view. Can be a due date or a completion date.
     */
    var date: ZonedDateTime? = null
        set(value) {
            val equals = field == value
            if (value == null) {
                dayOfWeek.text = null
                dayOfMonth.text = null
            } else {
                dayOfWeek.text = value.format(DateUtils.formatterDayName)
                dayOfMonth.text = value.format(DateUtils.formatterDayNumber)
            }
            field = value
            if (!equals) {
                invalidate()
                requestLayout()
            }
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_task_date, this, true)
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TaskDateView, 0, 0)
        try {
            date = a.getString(R.styleable.TaskDateView_date)?.let { ZonedDateTime.parse(it) }
        } finally {
            a.recycle()
        }
    }
}
