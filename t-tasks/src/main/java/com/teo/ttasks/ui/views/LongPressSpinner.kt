package com.teo.ttasks.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatSpinner
import com.teo.ttasks.R

class LongPressSpinner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.spinnerStyle
) : AppCompatSpinner(context, attrs, defStyleAttr) {

    open class LongPressArrayAdapter<T> @JvmOverloads constructor(
        context: Context, @LayoutRes resource: Int, @IdRes textViewResourceId: Int = 0, objects: List<T> = emptyList()
    ) : ArrayAdapter<T>(context, resource, textViewResourceId, objects) {

        var itemLongPressListener: ((position: Int) -> Boolean)? = null

        var itemPressListener: ((position: Int) -> Unit)? = null

        private val longClickListener: OnLongClickListener = OnLongClickListener {
            val position = it.getTag(R.id.tag_view_position) as Int
            return@OnLongClickListener itemLongPressListener?.invoke(position) ?: false
        }

        private val clickListener: OnClickListener = OnClickListener {
            val position = it.getTag(R.id.tag_view_position) as Int
            itemPressListener?.invoke(position)
        }

        protected fun enableLongClick(view: View, position: Int) {
            with(view) {
                setTag(R.id.tag_view_position, position)
                setOnLongClickListener(longClickListener)

                // Add custom click listener to restore selection functionality.
                // This is needed because adding a long click listener breaks it for some reason...
                setOnClickListener(clickListener)
            }
        }
    }

    var itemLongClickListener: ((position: Int) -> Boolean)? = null

    override fun setAdapter(adapter: SpinnerAdapter?) {
        super.setAdapter(adapter)

        (adapter as? LongPressArrayAdapter<*>)?.let {
            it.itemPressListener = { position ->
                setSelection(position)
                onDetachedFromWindow()
            }

            it.itemLongPressListener = { position ->
                onDetachedFromWindow()

                itemLongClickListener?.invoke(position) ?: false
            }
        }
    }
}
