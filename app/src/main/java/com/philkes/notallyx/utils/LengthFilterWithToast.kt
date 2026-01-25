package com.philkes.notallyx.utils

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.showToast

class LengthFilterWithToast(private val context: Context, max: Int) :
    InputFilter.LengthFilter(max) {
    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
        val truncated = super.filter(source, start, end, dest, dstart, dend)
        if (truncated != null) {
            context.showToast(context.getString(R.string.note_text_too_long_truncated, max))
        }
        return truncated
    }
}
