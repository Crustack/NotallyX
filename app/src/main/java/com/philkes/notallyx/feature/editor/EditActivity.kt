package com.philkes.notallyx.feature.editor

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.changeStatusAndNavigationBarColor
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.result.FileError
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.dialog.ColorPickerDialog
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.DualTextInputDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.isLightColor
import com.philkes.notallyx.core.ui.extensions.repeatOnLifecycleScope
import com.philkes.notallyx.core.ui.extensions.setLightStatusAndNavBar
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.generateBaseNote
import com.philkes.notallyx.feature.editor.bottomsheet.ActionSelectionBottomSheet
import com.philkes.notallyx.feature.editor.bottomsheet.MoreNoteBottomSheet
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.feature.main.MainActivity.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.feature.widget.NoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class EditActivity(private val type: Type) : LockedActivity<ComposeBinding>() {
    protected var search = Search()

    internal val notallyModel: EditActivityViewModel by viewModel()
    protected val actionHandler: NoteActionHandler by lazy { NoteActionHandler(this, notallyModel) }

    internal lateinit var changeHistory: ChangeHistory
    protected var undo: View? = null
    protected var redo: View? = null
    protected var jumpToTop: View? = null
    protected var jumpToBottom: View? = null

    internal var colorInt: Int = -1
    protected var inputMethodManager: InputMethodManager? = null

    protected val titleState = mutableStateOf("")
    protected val bodyTextState = mutableStateOf("")
    protected val isFormattingMenuVisible = mutableStateOf(false)
    protected val labelsState = mutableStateOf<List<String>>(emptyList())
    protected val colorIntState = mutableIntStateOf(0)

    protected val isSearchActiveState = mutableStateOf(false)
    protected val searchQueryState = mutableStateOf("")
    protected val searchResultPosState = mutableIntStateOf(0)
    protected val searchResultsCountState = mutableIntStateOf(0)

    val searchState: SearchState
        get() =
            SearchState(
                isActive = isSearchActiveState.value,
                query = searchQueryState.value,
                resultsCount = searchResultsCountState.intValue,
                resultPos = searchResultPosState.intValue,
            )

    fun updateLabels(labels: List<String>) {
        labelsState.value = labels
    }

    @Composable open fun RenderScreen() {}

    protected val canEdit
        get() = notallyModel.viewMode.value == NoteViewMode.EDIT

    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private val autoSaveRunnable = Runnable {
        lifecycleScope.launch(Dispatchers.Main) {
            updateModel()
            if (notallyModel.isModified()) {
                Log.d(TAG, "Auto-saving note...")
                saveNote(checkAutoSave = false)
            }
        }
    }

    override fun finish() {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            checkSave()
            super.finish()
        }
    }

    protected open suspend fun checkSave() {
        if (notallyModel.isEmpty()) {
            notallyModel.deleteBaseNote(checkAutoSave = false)
        } else if (notallyModel.isModified()) {
            saveNote()
        } else {
            notallyModel.checkBackupOnSave()
        }
    }

    internal open fun updateModel() {
        notallyModel.modifiedTimestamp = System.currentTimeMillis()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("id", notallyModel.id)
        if (notallyModel.isModified()) {
            lifecycleScope.launch { saveNote() }
        }
    }

    open suspend fun saveNote(checkAutoSave: Boolean = true): Long {
        updateModel()
        return notallyModel.saveNote(checkAutoSave).also {
            NoteWidget.sendBroadcast(application, longArrayOf(it))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val selectedId = intent.getLongExtra(EXTRA_SELECTED_BASE_NOTE, -1L)
        if (selectedId != -1L) {
            lifecycleScope.launch {
                checkSave()
                loadNote(selectedId, null, null, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionHandler.setupActivityResultLaunchers()
        inputMethodManager =
            ContextCompat.getSystemService(baseContext, InputMethodManager::class.java)
        notallyModel.type = type

        val composeView =
            ComposeView(this).apply {
                setContent {
                    NotallyXTheme(theme = preferences.theme.value) {
                        when (notallyModel.uiState) {
                            UiState.Loading -> Box(modifier = Modifier.fillMaxSize()) {}
                            UiState.Success -> {
                                RenderScreen()
                                EditActivityDialogs()
                            }
                        }
                    }
                }
            }
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        initialiseBinding()
        initChangeHistory()
        notallyModel.changeUiState(UiState.Loading)
        lifecycleScope.launch {
            val persistedId = savedInstanceState?.getLong("id")
            val selectedId = intent.getLongExtra(EXTRA_SELECTED_BASE_NOTE, 0L)
            val id = persistedId ?: selectedId
            loadNote(id, persistedId, savedInstanceState, true)
            notallyModel.changeUiState(UiState.Success)
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                updateModel()
                runBlocking(Dispatchers.IO) { saveNote() }
            } catch (e: Exception) {
                log(TAG, msg = "Saving note on Crash failed", throwable = e)
            } finally {
                DEFAULT_EXCEPTION_HANDLER?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun initialiseBinding() {
        repeatOnLifecycleScope {
            launch {
                notallyModel.eventBus.collect { event ->
                    event?.handle { errors -> displayFileErrors(errors) }
                }
            }
        }
    }

    private suspend fun loadNote(
        id: Long,
        persistedId: Long?,
        savedInstanceState: Bundle?,
        initListeners: Boolean,
    ) {
        changeHistory.reset()
        if (persistedId == null || notallyModel.originalNote == null) {
            notallyModel.setState(id, intent.data == null)
        }
        if (notallyModel.isNewNote) {
            when (intent.action) {
                Intent.ACTION_SEND,
                Intent.ACTION_SEND_MULTIPLE,
                Intent.ACTION_VIEW -> handleSharedNote()

                else ->
                    intent.getStringExtra(EXTRA_DISPLAYED_LABEL)?.let {
                        notallyModel.setLabels(listOf(it))
                    }
            }
        }

        initBottomMenu()
        resetToolbars()
        if (initListeners) setupListeners()
        setStateFromModel(savedInstanceState)

        if (
            !notallyModel.isNewNote && notallyModel.type == Type.LIST && savedInstanceState == null
        ) {
            val lastUsedViewMode = notallyModel.viewMode.value
            notallyModel.setViewMode(
                preferences.defaultListNoteViewMode.value.toNoteViewMode(lastUsedViewMode)
            )
        }
        if (initListeners) configureUI()
    }

    override fun onRestart() {
        super.onRestart()
        lifecycleScope.launch { notallyModel.refreshOriginalNote() }
    }

    open fun toggleCanEdit(mode: NoteViewMode) {}

    fun bindPinned() {}

    override fun onDestroy() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        super.onDestroy()
    }

    internal fun resetIdleTimer() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        val idleTime = preferences.autoSaveAfterIdleTime.value
        if (idleTime > -1) {
            autoSaveHandler.postDelayed(autoSaveRunnable, idleTime.toLong() * 1000)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION -> {
                if (
                    grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    actionHandler.startRecordAudioActivity()
                } else handleRejection(R.string.to_record_audio)
            }
            NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS -> {
                if (
                    grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    notallyModel.isPinnedToStatus = true
                    refreshStatusBarPin(notallyModel.getBaseNote())
                } else handleRejection(R.string.to_pin_note_status_bar)
            }
        }
    }

    protected open fun initChangeHistory() {
        changeHistory =
            ChangeHistory().apply {
                canUndo.collectIn(this@EditActivity) { canUndo -> undo?.isEnabled = canUndo }
                canRedo.collectIn(this@EditActivity) { canRedo -> redo?.isEnabled = canRedo }
                stackPointer.collectIn(this@EditActivity) { _ -> resetIdleTimer() }
            }
    }

    fun finishAfterDeleteForever() = super.finish()

    protected open fun resetToolbars() {}

    private var searchJob: Job? = null

    protected fun updateSearchResults(query: String) {
        val amountBefore = search.results.value
        searchJob?.cancel()
        if (query.isEmpty()) {
            searchResultsCountState.intValue = 0
            searchResultPosState.intValue = 0
            search.setResults(0)
            search.setResultPos(0)
            return
        }
        searchJob = lifecycleScope.launch {
            val amount = highlightSearchResults(query)
            searchResultsCountState.intValue = amount
            search.setResults(amount)
            if (amount > 0) {
                val newPos =
                    when {
                        amountBefore < 1 -> 0
                        search.resultPos.value >= amount -> amount - 1
                        else -> search.resultPos.value
                    }
                searchResultPosState.intValue = newPos
                search.setResultPos(newPos)
                selectSearchResult(newPos)
            } else {
                searchResultPosState.intValue = 0
                search.setResultPos(0)
            }
        }
    }

    abstract suspend fun highlightSearchResults(search: String): Int

    abstract fun selectSearchResult(resultPos: Int)

    open fun startSearch() {
        isSearchActiveState.value = true
        searchQueryState.value = ""
        searchResultsCountState.intValue = 0
        searchResultPosState.intValue = 0
        search = Search()
    }

    open fun endSearch() {
        isSearchActiveState.value = false
        searchQueryState.value = ""
        searchResultsCountState.intValue = 0
        searchResultPosState.intValue = 0
        search = Search()
    }

    open fun onSearchQueryChange(query: String) {
        searchQueryState.value = query
        search.query = query
        updateSearchResults(query)
    }

    open fun onNextSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            val nextPos = (searchResultPosState.intValue + 1) % total
            searchResultPosState.intValue = nextPos
            search.setResultPos(nextPos)
            selectSearchResult(nextPos)
        }
    }

    open fun onPrevSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            val current = searchResultPosState.intValue
            val prevPos = if (current > 0) current - 1 else total - 1
            searchResultPosState.intValue = prevPos
            search.setResultPos(prevPos)
            selectSearchResult(prevPos)
        }
    }

    protected fun isInSearchMode(): Boolean = isSearchActiveState.value || search.query.isNotEmpty()

    protected fun updateJumpButtonsVisibility(manualSize: Int? = null) {}

    protected open fun initBottomMenu() {}

    protected open fun openMoreOptionsBottomSheet() {
        val topActions = preferences.getSafeEditNoteActivityTopActions()
        val bottomAction = preferences.editNoteActivityBottomAction.value

        MoreNoteBottomSheet(notallyModel, colorInt, actionHandler, topActions, bottomAction)
            .show(supportFragmentManager, MoreNoteBottomSheet.TAG)
    }

    protected fun openActionSelectionBottomSheet(
        action: EditAction,
        onUpdateTopActions: (List<EditAction>) -> Unit,
        onUpdateBottomAction: (EditAction) -> Unit,
    ) {
        val topActions = preferences.getSafeEditNoteActivityTopActions()
        val bottomAction = preferences.editNoteActivityBottomAction.value

        val isTopAction = action in topActions
        val index = if (isTopAction) topActions.indexOf(action) else -1

        val allPossibleActions = EditAction.entries
        val availableActions = allPossibleActions.filter {
            it !in topActions &&
                it != bottomAction &&
                (it != EditAction.RESTORE || notallyModel.folder == Folder.DELETED)
        }
        val selectableActions = (listOf(action) + availableActions).distinct()

        ActionSelectionBottomSheet(
                actions = selectableActions,
                model = notallyModel,
                oldAction = action,
                title = getString(R.string.swap_action),
                onReset = {
                    if (isTopAction) {
                        preferences.editNoteActivityTopActions.save(
                            NotallyXPreferences.DEFAULT_EDIT_NOTE_TOP_ACTIONS
                        )
                        onUpdateTopActions(NotallyXPreferences.DEFAULT_EDIT_NOTE_TOP_ACTIONS)
                    } else {
                        preferences.editNoteActivityBottomAction.save(
                            NotallyXPreferences.DEFAULT_EDIT_NOTE_BOTTOM_ACTION
                        )
                        onUpdateBottomAction(NotallyXPreferences.DEFAULT_EDIT_NOTE_BOTTOM_ACTION)
                    }
                },
                color = colorInt,
                onActionSelected = { selectedAction ->
                    if (isTopAction) {
                        val updated = topActions.toMutableList()
                        if (index != -1 && index in updated.indices) {
                            updated[index] = selectedAction
                            preferences.editNoteActivityTopActions.save(updated)
                            onUpdateTopActions(updated)
                        }
                    } else {
                        preferences.editNoteActivityBottomAction.save(selectedAction)
                        onUpdateBottomAction(selectedAction)
                    }
                },
            )
            .show(supportFragmentManager, ActionSelectionBottomSheet.TAG)
    }

    fun updateToggleViewMode() {}

    abstract fun configureUI()

    open fun setupListeners() {
        search.results
            .combine(search.resultPos) { amount, pos -> Pair(amount, pos) }
            .drop(1)
            .collectIn(this) { (amount, pos) ->
                val hasResults = amount > 0
                search.nextMenuItem?.isEnabled = hasResults
                search.prevMenuItem?.isEnabled = hasResults
            }

        search.resultPos.drop(1).collectIn(this) { pos -> selectSearchResult(pos) }
        setupAdditionalListeners()
    }

    protected open fun setupAdditionalListeners() {
        repeatOnLifecycleScope {
            launch {
                notallyModel.viewMode.collect { value ->
                    updateToggleViewMode()
                    toggleCanEdit(value)
                }
            }
        }
    }

    open fun setStateFromModel(savedInstanceState: Bundle?) {
        titleState.value = notallyModel.title
        labelsState.value = notallyModel.labels.toList()
        setColor()
    }

    private fun handleSharedNote() {
        val sharedNote = intent.generateBaseNote(this)
        notallyModel.title = sharedNote.title
        notallyModel.body = Editable.Factory.getInstance().newEditable(sharedNote.text)
        titleState.value = sharedNote.title
        bodyTextState.value = sharedNote.text
    }

    private fun displayFileErrors(errors: List<FileError>) {
        notallyModel.activeDialog.value = EditorDialogState.FileErrors(errors)
    }

    @Composable
    protected fun EditActivityDialogs() {
        val activeDialog by notallyModel.activeDialog.collectAsStateWithLifecycle()
        val addingFilesProgress by notallyModel.addingFiles.collectAsStateWithLifecycle()

        if (addingFilesProgress?.inProgress == true) {
            val progress = addingFilesProgress!!
            val msg =
                when {
                    progress.indeterminate -> stringResource(R.string.calculating)
                    else ->
                        stringResource(R.string.count, progress.current, progress.total) +
                            (progress.countSuffix?.let { " $it" } ?: "")
                }
            val progressValue =
                if (progress.indeterminate || progress.total == 0) null
                else progress.current.toFloat() / progress.total.toFloat()

            ProgressDialog(
                title = stringResource(progress.titleId),
                message = msg,
                progress = progressValue,
            )
        }

        when (val dialogState = activeDialog) {
            is EditorDialogState.ColorPicker -> {
                ColorPickerDialog(
                    currentColor = dialogState.currentColor,
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                    onColorSelected = { selectedColor ->
                        dialogState.onColorSelected(selectedColor)
                        notallyModel.activeDialog.value = null
                    },
                )
            }
            is EditorDialogState.DeleteForeverConfirm -> {
                ConfirmDialog(
                    message = stringResource(R.string.delete_note_forever),
                    positiveButtonText = stringResource(R.string.delete),
                    negativeButtonText = stringResource(R.string.cancel),
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                    onConfirm = {
                        dialogState.onConfirm()
                        notallyModel.activeDialog.value = null
                    },
                )
            }
            is EditorDialogState.AudioPermissionRationale -> {
                ConfirmDialog(
                    message = stringResource(R.string.please_grant_notally_audio),
                    positiveButtonText = stringResource(R.string.continue_),
                    negativeButtonText = stringResource(R.string.cancel),
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                    onConfirm = {
                        dialogState.onConfirm()
                        notallyModel.activeDialog.value = null
                    },
                )
            }
            is EditorDialogState.PermissionRationale -> {
                ConfirmDialog(
                    message = stringResource(dialogState.messageResId),
                    positiveButtonText = stringResource(R.string.continue_),
                    negativeButtonText = stringResource(R.string.cancel),
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                    onConfirm = {
                        dialogState.onConfirm()
                        notallyModel.activeDialog.value = null
                    },
                )
            }
            is EditorDialogState.RejectionConfirm -> {
                ConfirmDialog(
                    message = stringResource(dialogState.msgResId),
                    positiveButtonText = stringResource(R.string.settings),
                    negativeButtonText = stringResource(R.string.cancel),
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                    onConfirm = {
                        dialogState.onConfirm()
                        notallyModel.activeDialog.value = null
                    },
                )
            }
            is EditorDialogState.FileErrors -> {
                ListDialog(
                    title = stringResource(R.string.cant_load_file),
                    items = dialogState.errors.map { "${it.name}: ${it.description}" },
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                )
            }
            is EditorDialogState.Error -> {
                ErrorDialog(
                    title = stringResource(dialogState.titleResId),
                    message = dialogState.message,
                    onDismissRequest = { notallyModel.activeDialog.value = null },
                )
            }
            is EditorDialogState.EditLink -> {
                if (dialogState.isNoteUrl) {
                    TextInputDialog(
                        title = stringResource(R.string.edit_link),
                        initialValue = dialogState.displayTextBefore,
                        hint = stringResource(R.string.display_text),
                        onDismissRequest = {
                            dialogState.onDismiss()
                            notallyModel.activeDialog.value = null
                        },
                        onConfirm = { displayTextAfter ->
                            dialogState.onConfirm(dialogState.urlBefore, displayTextAfter)
                            notallyModel.activeDialog.value = null
                        },
                    )
                } else {
                    DualTextInputDialog(
                        title = stringResource(R.string.edit_link),
                        initialValue1 = dialogState.displayTextBefore,
                        initialValue2 = dialogState.urlBefore,
                        hint1 = stringResource(R.string.display_text),
                        hint2 = stringResource(R.string.link),
                        onDismissRequest = {
                            dialogState.onDismiss()
                            notallyModel.activeDialog.value = null
                        },
                        onConfirm = { displayTextAfter, urlAfter ->
                            dialogState.onConfirm(urlAfter, displayTextAfter)
                            notallyModel.activeDialog.value = null
                        },
                    )
                }
            }
            null -> {}
        }
    }

    open fun setColor() {
        colorInt = extractColor(notallyModel.color)
        colorIntState.intValue = colorInt
        changeStatusAndNavigationBarColor(colorInt)
        window.setLightStatusAndNavBar(colorInt.isLightColor())
    }

    class Search(
        var query: String = "",
        var prevMenuItem: MenuItem? = null,
        var nextMenuItem: MenuItem? = null,
    ) {
        val resultPos = MutableStateFlow(-1)
        val results = MutableStateFlow(-1)

        fun setResultPos(pos: Int) {
            resultPos.value = pos
        }

        fun setResults(amount: Int) {
            results.value = amount
        }
    }

    companion object {
        private const val TAG = "EditActivity"
        const val REQUEST_AUDIO_PERMISSION = 0

        const val EXTRA_SELECTED_BASE_NOTE = "notallyx.intent.extra.EXTRA_SELECTED_BASE_NOTE"
        const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.EXTRA_SELECTED_LABELS"
        const val EXTRA_NOTE_ID = "notallyx.intent.extra.EXTRA_NOTE_ID"
        const val EXTRA_FOLDER_FROM = "notallyx.intent.extra.EXTRA_FOLDER_FROM"
        const val EXTRA_FOLDER_TO = "notallyx.intent.extra.EXTRA_FOLDER_TO"
        const val EXTRA_INITIAL_SEARCH_QUERY = "notallyx.intent.extra.EXTRA_INITIAL_SEARCH_QUERY"

        val DEFAULT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler()
    }
}
