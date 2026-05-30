package com.philkes.notallyx.data.model

import android.app.Dialog
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicBoolean

/** Helper to register Converter errors while accessing the database. */
object ConverterErrorReporter {
    var enabled = AtomicBoolean(true)
    val errors = MutableLiveData<Throwable?>(null)
    val activeDialogs = mutableSetOf<Dialog>()

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
        activeDialogs.forEach { it.dismiss() }
        activeDialogs.clear()
        errors.postValue(null)
    }
}
