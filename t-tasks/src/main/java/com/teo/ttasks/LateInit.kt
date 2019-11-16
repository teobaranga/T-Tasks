package com.teo.ttasks

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class LateInit<T : Any>(
    private val getter: FieldHolder<T>.() -> T = { field },
    private val setter: FieldHolder<T>.(T) -> Unit = { field = it }
) : ReadWriteProperty<Any?, T> {

    private val fieldHolder = FieldHolder<T>()

    override fun getValue(thisRef: Any?, property: KProperty<*>) = fieldHolder.getter()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = fieldHolder.setter(value)

    class FieldHolder<T : Any> {
        lateinit var field: T

        /** Check if the backing field has been initialized. */
        fun isInitialized() = ::field.isInitialized
    }
}
