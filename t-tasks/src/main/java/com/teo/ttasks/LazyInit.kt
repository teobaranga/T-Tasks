package com.teo.ttasks


open class LazyInit<T : Any>(
    private val getter: () -> T
) : LateInit<T>(
    getter = {
        if (!isInitialized()) {
            field = getter()
        }
        field
    }
)
