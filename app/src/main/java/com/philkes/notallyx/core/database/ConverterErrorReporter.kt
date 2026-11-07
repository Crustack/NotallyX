package com.philkes.notallyx.core.database

import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Helper to register Converter errors while accessing the database. */
object ConverterErrorReporter {
    val enabled = AtomicBoolean(true)
    val errors: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)
    val dismissCallbacks = Collections.synchronizedSet(mutableSetOf<() -> Unit>())

    fun reportError(throwable: Throwable) {
        if (enabled.get() && errors.value == null) {
            errors.value = throwable
        }
    }

    fun dismissAllDialogs() {
        dismissCallbacks.toList().forEach { it.invoke() }
        synchronized(dismissCallbacks) { dismissCallbacks.clear() }
        errors.value = null
    }
}
