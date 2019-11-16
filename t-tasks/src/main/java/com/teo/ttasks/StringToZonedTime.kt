package com.teo.ttasks

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class StringToZonedTime(
    private val getDate: () -> String?,
    private val setDate: (String?) -> Unit
) : ReadWriteProperty<Any, ZonedDateTime?> {

    private var zonedDateTime: ZonedDateTime? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): ZonedDateTime? {
        if (zonedDateTime == null) {
            zonedDateTime = getDate()?.let { ZonedDateTime.parse(it) }
        }
        return zonedDateTime
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: ZonedDateTime?) {
        if (value == null) {
            zonedDateTime = null
            setDate(null)
        } else {
            zonedDateTime = value
            setDate(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
        }
    }
}
