package com.philkes.notallyx.data.model

import android.app.Dialog
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Helper to register Converter errors while accessing the database. */
object ConverterErrorReporter {
    val enabled = AtomicBoolean(true)
    val errors: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)
    val activeDialogs = Collections.synchronizedSet(mutableSetOf<Dialog>())

    fun reportError(throwable: Throwable) {
        if (enabled.get() && errors.value == null) {
            errors.value = throwable
        }
    }

    fun registerDialog(dialog: Dialog) {
        activeDialogs.add(dialog)
        // Auto-remove when it's dismissed naturally
        dialog.setOnDismissListener { activeDialogs.remove(dialog) }
    }

    fun dismissAllDialogs() {
        activeDialogs.toList().forEach { it.dismiss() }
        synchronized(activeDialogs) { activeDialogs.clear() }
        errors.value = null
    }
}
