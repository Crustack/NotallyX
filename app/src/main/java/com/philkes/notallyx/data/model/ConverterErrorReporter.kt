package com.philkes.notallyx.data.model

import android.app.Dialog
import androidx.lifecycle.MutableLiveData
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/** Helper to register Converter errors while accessing the database. */
object ConverterErrorReporter {
    val enabled = AtomicBoolean(true)
    val errors = MutableLiveData<Throwable?>(null)
    val activeDialogs = Collections.synchronizedSet(mutableSetOf<Dialog>())

    fun reportError(throwable: Throwable) {
        if (enabled.get() && errors.value == null) {
            errors.postValue(throwable)
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
        errors.postValue(null)
    }
}
