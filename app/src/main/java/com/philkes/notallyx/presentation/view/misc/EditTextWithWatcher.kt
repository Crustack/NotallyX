package com.philkes.notallyx.presentation.view.misc

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.text.method.KeyListener
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.philkes.notallyx.presentation.clone
import com.philkes.notallyx.presentation.showKeyboard

open class EditTextWithWatcher(context: Context, attrs: AttributeSet) :
    AppCompatEditText(context, attrs) {
    var textWatcher: TextWatcher? = null
    private var onSelectionChange: ((selStart: Int, selEnd: Int) -> Unit)? = null
    private var keyListenerInstance: KeyListener? = null

    private var isEditableMode = true

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChange?.invoke(selStart, selEnd)
    }

    fun setOnSelectionChange(callback: (selStart: Int, selEnd: Int) -> Unit) {
        this.onSelectionChange = callback
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        applyWithoutTextWatcher { super.setText(text, type) }
    }

    fun setText(text: Editable) {
        super.setText(text, BufferType.EDITABLE)
    }

    fun setCanEdit(value: Boolean) {
        isEditableMode = value
        if (!value) {
            clearFocus()
        }
        keyListener?.let { keyListenerInstance = it }
        keyListener = if (value) keyListenerInstance else null // Disables text editing
        isCursorVisible = true
        isFocusable = value
        isFocusableInTouchMode = value
        setTextIsSelectable(true)
    }

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: android.graphics.Rect?,
    ) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && isEditableMode) {
            context.showKeyboard(this)
        }
    }

    override fun performClick(): Boolean {
        val result = super.performClick()
        if (isEditableMode) {
            context.showKeyboard(this)
            return true
        }
        return result
    }

    @Deprecated(
        "You should not access text Editable directly, use other member functions to edit/read text properties.",
        replaceWith = ReplaceWith("changeText/applyWithoutTextWatcher/..."),
    )
    override fun getText(): Editable? {
        return getTextSafe()
    }

    fun getTextClone(): Editable {
        return getTextSafe().clone()
    }

    fun applyWithoutTextWatcher(
        callback: EditTextWithWatcher.() -> Unit
    ): Pair<Editable, Editable> {
        val textBefore = getTextClone()
        val editTextWatcher = textWatcher
        editTextWatcher?.let { removeTextChangedListener(it) }
        callback()
        editTextWatcher?.let { addTextChangedListener(it) }
        return Pair(textBefore, getTextClone())
    }

    fun changeText(callback: (text: Editable) -> Unit): Pair<Editable, Editable> {
        return applyWithoutTextWatcher { callback(getTextSafe()!!) }
    }

    private fun getTextSafe() = super.getText() ?: Editable.Factory.getInstance().newEditable("")

    fun focusAndSelect(
        start: Int = selectionStart,
        end: Int = selectionEnd,
        inputMethodManager: InputMethodManager? = null,
    ) {
        requestFocus()
        if (start > -1) {
            setSelection(start, if (end < 0) start else end)
        }
        inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}
