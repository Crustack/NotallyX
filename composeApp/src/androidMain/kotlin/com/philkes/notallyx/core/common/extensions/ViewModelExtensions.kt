package com.philkes.notallyx.core.common.extensions

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun isPreview(): Boolean = LocalInspectionMode.current

fun ViewModel.executeAsyncWithCallback(
    function: suspend () -> Unit,
    callback: (success: Boolean) -> Unit,
) {
    viewModelScope.launch {
        val success =
            try {
                withContext(Dispatchers.IO) { function() }
                true
            } catch (_: SQLiteConstraintException) {
                false
            }
        callback(success)
    }
}
