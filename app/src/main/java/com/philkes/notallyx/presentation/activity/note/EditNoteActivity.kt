package com.philkes.notallyx.presentation.activity.note

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.style.URLSpan
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.getNoteIdFromUrl
import com.philkes.notallyx.data.model.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.isNoteUrl
import com.philkes.notallyx.databinding.BottomTextFormattingMenuBinding
import com.philkes.notallyx.databinding.RecyclerToggleBinding
import com.philkes.notallyx.presentation.addIconButton
import com.philkes.notallyx.presentation.dp
import com.philkes.notallyx.presentation.hideKeyboard
import com.philkes.notallyx.presentation.setControlsContrastColorForAllViews
import com.philkes.notallyx.presentation.setOnNextAction
import com.philkes.notallyx.presentation.showKeyboard
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.note.TextFormattingAdapter
import com.philkes.notallyx.presentation.view.note.action.AddNoteBottomSheet
import com.philkes.notallyx.utils.LinkMovementMethod
import com.philkes.notallyx.utils.copyToClipBoard
import com.philkes.notallyx.utils.findAllOccurrences
import com.philkes.notallyx.utils.openNote
import com.philkes.notallyx.utils.wrapWithChooser

class EditNoteActivity : EditActivity(Type.NOTE) {

    private lateinit var textFormatMenu: View

    private var textFormattingAdapter: TextFormattingAdapter? = null

    private var searchResultIndices: List<Pair<Int, Int>>? = null

    override fun configureUI() {
        binding.EnterTitle.setOnNextAction { binding.EnterBody.requestFocus() }

        if (notallyModel.isNewNote) {
            binding.EnterBody.requestFocus()
        }
    }

    override fun toggleCanEdit(mode: NoteViewMode) {
        super.toggleCanEdit(mode)
        textFormatMenu.isVisible = mode == NoteViewMode.EDIT
        when {
            mode == NoteViewMode.EDIT -> showKeyboard(binding.EnterBody)
            binding.EnterBody.isFocused -> hideKeyboard(binding.EnterBody)
        }
        binding.EnterBody.setCanEdit(mode == NoteViewMode.EDIT)
        setupEditor()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putInt(EXTRA_SELECTION_START, binding.EnterBody.selectionStart)
            putInt(EXTRA_SELECTION_END, binding.EnterBody.selectionEnd)
        }
    }

    override fun highlightSearchResults(search: String): Int {
        binding.EnterBody.clearHighlights()
        if (search.isEmpty()) {
            return 0
        }
        searchResultIndices =
            notallyModel.body.toString().findAllOccurrences(search).onEach { (startIdx, endIdx) ->
                binding.EnterBody.highlight(startIdx, endIdx, false)
            }
        return searchResultIndices!!.size
    }

    override fun selectSearchResult(resultPos: Int) {
        if (resultPos < 0) {
            binding.EnterBody.unselectHighlight()
            return
        }
        searchResultIndices?.get(resultPos)?.let { (startIdx, endIdx) ->
            val selectedLineTop = binding.EnterBody.highlight(startIdx, endIdx, true)
            selectedLineTop?.let { binding.ScrollView.scrollTo(0, it) }
        }
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.EnterBody.initHistory(changeHistory) { text ->
            val textChanged = !notallyModel.body.toString().contentEquals(text)
            notallyModel.body = text
            if (textChanged) {
                updateSearchResults(search.query)
                updateJumpButtonsVisibility()
            }
        }
    }

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        updateEditText()
        savedInstanceState?.let {
            val selectionStart = it.getInt(EXTRA_SELECTION_START, -1)
            val selectionEnd = it.getInt(EXTRA_SELECTION_END, -1)
            if (selectionStart > -1) {
                binding.EnterBody.postOnAnimation {
                    binding.EnterBody.focusAndSelect(selectionStart, selectionEnd)
                }
            }
        }
    }

    private fun updateEditText() {
        binding.EnterBody.text = notallyModel.body
    }

    private fun setupEditor() {
        setupMovementMethod()
        binding.EnterBody.customSelectionActionModeCallback =
            if (canEdit) {
                FormattingActionModeCallback(this, binding.EnterBody)
            } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.EnterBody.customInsertionActionModeCallback =
                if (canEdit) {
                    FormattingActionModeCallback(this, binding.EnterBody)
                } else null
        }
        if (canEdit) {
            binding.EnterBody.setOnSelectionChange { selStart, selEnd ->
                if (selEnd - selStart > 0) {
                    if (!textFormatMenu.isEnabled) {
                        initBottomTextFormattingMenu()
                    }
                    textFormatMenu.isEnabled = true
                    textFormattingAdapter?.updateTextFormattingToggles(selStart, selEnd)
                } else {
                    if (textFormatMenu.isEnabled) {
                        initBottomMenu()
                    }
                    textFormatMenu.isEnabled = false
                }
            }
        } else {
            binding.EnterBody.setOnSelectionChange { _, _ -> }
        }
        binding.ContentLayout.setOnClickListener {
            binding.EnterBody.apply {
                requestFocus()
                if (canEdit) {
                    setSelection(length())
                    showKeyboard(this)
                }
            }
        }
    }

    override fun initBottomMenu() {
        super.initBottomMenu()
        binding.BottomAppBarCenter.visibility = VISIBLE
        binding.BottomAppBarLeft.apply {
            removeAllViews()
            addIconButton(R.string.add_item, R.drawable.add, colorInt, marginStart = 0) {
                AddNoteBottomSheet(actionHandler, colorInt)
                    .show(supportFragmentManager, AddNoteBottomSheet.TAG)
            }
            updateLayoutParams<ConstraintLayout.LayoutParams> { endToStart = -1 }
            textFormatMenu =
                addIconButton(R.string.edit, R.drawable.text_format, colorInt) {
                        initBottomTextFormattingMenu()
                    }
                    .apply { isEnabled = binding.EnterBody.isActionModeOn }
        }
        setBottomAppBarColor(colorInt)
    }

    private fun initBottomTextFormattingMenu() {
        binding.BottomAppBarCenter.visibility = GONE
        val extractColor = colorInt
        binding.BottomAppBarRight.apply {
            removeAllViews()
            addView(
                RecyclerToggleBinding.inflate(layoutInflater, this, false).root.apply {
                    setIconResource(R.drawable.close)
                    contentDescription = context.getString(R.string.cancel)
                    setOnClickListener { initBottomMenu() }

                    updateLayoutParams<LinearLayout.LayoutParams> {
                        marginEnd = 0
                        marginStart = 10.dp
                    }
                    setControlsContrastColorForAllViews(extractColor)
                    setBackgroundColor(0)
                }
            )
        }
        binding.BottomAppBarLeft.apply {
            removeAllViews()
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                endToStart = R.id.BottomAppBarRight
            }
            requestLayout()
            val layout = BottomTextFormattingMenuBinding.inflate(layoutInflater, this, false)
            layout.MainListView.apply {
                textFormattingAdapter =
                    TextFormattingAdapter(this@EditNoteActivity, binding.EnterBody, colorInt)
                adapter = textFormattingAdapter
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }
            addView(layout.root)
        }
    }

    private fun setupMovementMethod() {
        val movementMethod = LinkMovementMethod { span ->
            val items =
                if (span.url.isNoteUrl()) {
                    if (canEdit) {
                        arrayOf(
                            getString(R.string.open_note),
                            getString(R.string.remove_link),
                            getString(R.string.change_note),
                            getString(R.string.edit),
                        )
                    } else arrayOf(getString(R.string.open_note))
                } else {
                    if (canEdit) {
                        arrayOf(
                            getString(R.string.open_link),
                            getString(R.string.copy),
                            getString(R.string.remove_link),
                            getString(R.string.edit),
                        )
                    } else arrayOf(getString(R.string.open_link), getString(R.string.copy))
                }
            MaterialAlertDialogBuilder(this)
                .setTitle(
                    if (span.url.isNoteUrl())
                        "${getString(R.string.note)}: ${
                            binding.EnterBody.getSpanText(span)
                        }"
                    else span.url
                )
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> openLink(span)
                        1 ->
                            if (span.url.isNoteUrl()) {
                                removeLink(span)
                            } else copyLink(span)
                        2 ->
                            if (span.url.isNoteUrl()) {
                                actionHandler.updateNoteLink(span)
                            } else removeLink(span)
                        3 -> editLink(span)
                    }
                }
                .show()
        }
        binding.EnterBody.movementMethod = movementMethod
    }

    private fun openLink(span: URLSpan) {
        span.url?.let {
            if (it.isNoteUrl()) {
                span.navigateToNote()
            } else {
                openLink(span.url)
            }
        }
    }

    private fun editLink(span: URLSpan) {
        binding.EnterBody.showEditDialog(span)
    }

    private fun copyLink(span: URLSpan) {
        copyToClipBoard(span.url)
        showToast(R.string.copied_link)
    }

    private fun removeLink(span: URLSpan) {
        binding.EnterBody.removeSpanWithHistory(
            span,
            span.url.isNoteUrl() || span.url == binding.EnterBody.getSpanText(span),
        )
    }

    private fun openLink(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(this)
        try {
            startActivity(intent)
        } catch (exception: Exception) {
            showToast(R.string.cant_open_link)
        }
    }

    private fun URLSpan.navigateToNote() {
        openNote(this.url.getNoteIdFromUrl(), this.url.getNoteTypeFromUrl())
    }

    companion object {
        private const val EXTRA_SELECTION_START = "notallyx.intent.extra.EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "notallyx.intent.extra.EXTRA_SELECTION_END"
    }
}
