package com.philkes.notallyx.data.model

import android.app.Dialog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Helper to register Converter errors while accessing the database. */
object ConverterErrorReporter {
    private val _errors = MutableSharedFlow<Throwable>(extraBufferCapacity = 10)
    val errors = _errors.asSharedFlow()
    val activeDialogs = mutableSetOf<Dialog>()

    fun reportError(throwable: Throwable) {
        _errors.tryEmit(throwable)
    }

    fun registerDialog(dialog: Dialog) {
        activeDialogs.add(dialog)
        // Auto-remove when it's dismissed naturally
        dialog.setOnDismissListener { activeDialogs.remove(dialog) }
    }

    fun dismissAllDialogs() {
        activeDialogs.forEach { it.dismiss() }
        activeDialogs.clear()
    }
}
