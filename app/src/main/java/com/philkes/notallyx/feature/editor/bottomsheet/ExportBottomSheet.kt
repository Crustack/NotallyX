package com.philkes.notallyx.feature.editor.bottomsheet

import androidx.annotation.ColorInt
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.feature.migration.ExportMimeType

class ExportBottomSheet(@ColorInt color: Int?, callback: (ExportMimeType) -> Unit) :
    ActionBottomSheet(createActions(callback), color) {

    companion object {
        const val TAG = "ExportBottomSheet"

        private fun createActions(callback: (ExportMimeType) -> Unit): List<Action> {
            return ExportMimeType.entries.map { mimeType ->
                Action(labelResId = EditAction.EXPORT.textResId, label = mimeType.name) { _ ->
                    callback.invoke(mimeType)
                    true
                }
            }
        }
    }
}
