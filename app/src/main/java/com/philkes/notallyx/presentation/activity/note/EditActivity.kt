package com.philkes.notallyx.presentation.activity.note

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spanned
import android.text.style.URLSpan
import android.util.Log
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.VISIBLE
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.generateBaseNote
import com.philkes.notallyx.databinding.ActivityEditBinding
import com.philkes.notallyx.presentation.activity.LockedActivity
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.activity.main.MainActivity.Companion.EXTRA_FRAGMENT_TO_OPEN
import com.philkes.notallyx.presentation.activity.main.MainActivity.Companion.EXTRA_SKIP_START_VIEW_ON_BACK
import com.philkes.notallyx.presentation.activity.main.fragment.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.presentation.activity.note.reminders.RemindersActivity
import com.philkes.notallyx.presentation.add
import com.philkes.notallyx.presentation.addFastScroll
import com.philkes.notallyx.presentation.addIconButton
import com.philkes.notallyx.presentation.bindLabels
import com.philkes.notallyx.presentation.displayEditLabelDialog
import com.philkes.notallyx.presentation.displayFormattedTimestamp
import com.philkes.notallyx.presentation.extractColor
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.hideKeyboard
import com.philkes.notallyx.presentation.isLightColor
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.setControlsContrastColorForAllViews
import com.philkes.notallyx.presentation.setLightStatusAndNavBar
import com.philkes.notallyx.presentation.setupProgressDialog
import com.philkes.notallyx.presentation.setupReminderChip
import com.philkes.notallyx.presentation.showKeyboard
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.view.note.ErrorAdapter
import com.philkes.notallyx.presentation.view.note.action.ActionSelectionBottomSheet
import com.philkes.notallyx.presentation.view.note.action.AddBottomSheet
import com.philkes.notallyx.presentation.view.note.action.MoreNoteBottomSheet
import com.philkes.notallyx.presentation.view.note.audio.AudioAdapter
import com.philkes.notallyx.presentation.view.note.preview.PreviewFileAdapter
import com.philkes.notallyx.presentation.view.note.preview.PreviewImageAdapter
import com.philkes.notallyx.presentation.viewmodel.NotallyModel
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.EditAction
import com.philkes.notallyx.presentation.viewmodel.preference.ListItemSort
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import com.philkes.notallyx.presentation.widget.WidgetProvider
import com.philkes.notallyx.utils.FileError
import com.philkes.notallyx.utils.changeStatusAndNavigationBarColor
import com.philkes.notallyx.utils.changehistory.ChangeHistory
import com.philkes.notallyx.utils.findWebUrls
import com.philkes.notallyx.utils.getUriForFile
import com.philkes.notallyx.utils.isInLandscapeMode
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.mergeSkipFirst
import com.philkes.notallyx.utils.observeSkipFirst
import com.philkes.notallyx.utils.textMaxLengthFilter
import com.philkes.notallyx.utils.wrapWithChooser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class EditActivity(private val type: Type) : LockedActivity<ActivityEditBinding>() {
    private lateinit var audioAdapter: AudioAdapter
    private lateinit var fileAdapter: PreviewFileAdapter
    protected var search = Search()

    internal val notallyModel: NotallyModel by viewModels()
    protected val actionHandler: NoteActionHandler by lazy { NoteActionHandler(this, notallyModel) }

    internal lateinit var changeHistory: ChangeHistory
    protected var undo: View? = null
    protected var redo: View? = null
    protected var jumpToTop: View? = null
    protected var jumpToBottom: View? = null

    internal var colorInt: Int = -1
    protected var inputMethodManager: InputMethodManager? = null

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
        lifecycleScope.launch(Dispatchers.Main) {
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
            WidgetProvider.sendBroadcast(application, longArrayOf(it))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionHandler.setupActivityResultLaunchers()
        inputMethodManager =
            ContextCompat.getSystemService(baseContext, InputMethodManager::class.java)
        notallyModel.type = type
        initialiseBinding()
        setContentView(binding.root)
        configureEdgeToEdgeInsets()

        initChangeHistory()
        lifecycleScope.launch {
            val persistedId = savedInstanceState?.getLong("id")
            val selectedId = intent.getLongExtra(EXTRA_SELECTED_BASE_NOTE, 0L)
            val id = persistedId ?: selectedId
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
            setupListeners()
            setStateFromModel(savedInstanceState)

            configureUI()
            binding.ScrollView.apply {
                visibility = View.VISIBLE
                addFastScroll(this@EditActivity)
            }
            setupEditNoteReminderChip()
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                updateModel()
                runBlocking(Dispatchers.IO) { saveNote() }
            } catch (e: Exception) {
                log(TAG, msg = "Saving note on Crash failed", throwable = e)
            } finally {
                // Let the system handle the crash
                DEFAULT_EXCEPTION_HANDLER?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        lifecycleScope.launch {
            notallyModel.refreshOriginalNote()
            setupEditNoteReminderChip()
        }
    }

    override fun onStart() {
        super.onStart()
        // If launched with an initial search query (from global search), auto-start in-note search
        intent.getStringExtra(EXTRA_INITIAL_SEARCH_QUERY)?.let { initialQuery ->
            if (initialQuery.isNotBlank()) {
                binding.EnterSearchKeyword.postOnAnimation {
                    startSearch()
                    binding.EnterSearchKeyword.setText(initialQuery)
                    binding.EnterSearchKeyword.setSelection(initialQuery.length)
                }
            }
            intent.removeExtra(EXTRA_INITIAL_SEARCH_QUERY)
        }
    }

    private fun configureEdgeToEdgeInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContentLayout) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()) // For keyboard

            // Apply top padding to your main content to push it below the status bar
            // and bottom padding to push it above the navigation bar / keyboard / BottomAppBar
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                // Combine navigation bar inset with keyboard inset for bottom padding
                // and ensure there's enough space for the BottomAppBar
                systemBarsInsets.bottom + imeInsets.bottom,
            )

            // Let the BottomAppBar consume its own insets
            // CoordinatorLayout usually handles this well with its children having specific
            // behaviors.
            // If the BottomAppBar isn't adapting, you might need a custom Behavior or ensure its
            // height is consistent.
            // A common approach is to set the bottom margin of your main content
            // to the height of the BottomAppBar + the nav bar inset.
            val navBarBottom = systemBarsInsets.bottom

            // Option A: Set padding on the ScrollView inside main_content_layout
            // This is often more effective for scrollable content.
            binding.ScrollView.apply {
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    binding.BottomAppBarLayout.height +
                        navBarBottom, // Space for BottomAppBar + nav bar
                )
            }
            insets
        }
    }

    open fun toggleCanEdit(mode: NoteViewMode) {
        binding.EnterTitle.apply {
            if (isFocused) {
                when {
                    mode == NoteViewMode.EDIT -> showKeyboard(this)
                    else -> hideKeyboard(this)
                }
            }
            setCanEdit(mode == NoteViewMode.EDIT)
        }
    }

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
                } else actionHandler.handleRejection()
            }
        }
    }

    protected open fun initChangeHistory() {
        changeHistory =
            ChangeHistory().apply {
                canUndo.observe(this@EditActivity) { canUndo -> undo?.isEnabled = canUndo }
                canRedo.observe(this@EditActivity) { canRedo -> redo?.isEnabled = canRedo }
                stackPointer.observe(this@EditActivity) { _ -> resetIdleTimer() }
            }
    }

    fun finishAfterDeleteForever() = super.finish()

    private fun showActionSelectionDialog(
        oldAction: EditAction,
        index: Int,
        isBottomBar: Boolean = false,
    ) {
        val actions = EditAction.entries.filter { it != EditAction.RESTORE }
        ActionSelectionBottomSheet(
                actions,
                notallyModel,
                oldAction,
                title = getString(R.string.swap_action),
                onReset = {
                    val prefs = NotallyXPreferences.getInstance(this)
                    if (isBottomBar) {
                        prefs.editNoteActivityBottomAction.save(
                            NotallyXPreferences.DEFAULT_EDIT_NOTE_BOTTOM_ACTION
                        )
                    } else {
                        val currentActions =
                            prefs.getSafeEditNoteActivityTopActions().toMutableList()
                        if (index in currentActions.indices) {
                            currentActions[index] =
                                NotallyXPreferences.DEFAULT_EDIT_NOTE_TOP_ACTIONS[index]
                            prefs.editNoteActivityTopActions.save(currentActions)
                        }
                    }
                },
                colorInt,
            ) { newAction ->
                val prefs = NotallyXPreferences.getInstance(this)
                if (isBottomBar) {
                    prefs.editNoteActivityBottomAction.save(newAction)
                } else {
                    val currentActions = prefs.getSafeEditNoteActivityTopActions().toMutableList()
                    if (index in currentActions.indices) {
                        currentActions[index] = newAction
                        prefs.editNoteActivityTopActions.save(currentActions)
                    }
                }
            }
            .show(supportFragmentManager, ActionSelectionBottomSheet.TAG)
    }

    protected open fun resetToolbars() {
        binding.Toolbar.setNavigationOnClickListener { finish() }
        updateTopActions(preferences.getSafeEditNoteActivityTopActions())
        updateBottomActions(preferences.editNoteActivityBottomAction.value)
    }

    protected fun updateSearchResults(query: String) {
        val amountBefore = search.results.value
        val amount = highlightSearchResults(query)
        this.search.results.value = amount
        if (amount > 0) {
            search.resultPos.value =
                when {
                    amountBefore < 1 -> 0
                    search.resultPos.value >= amount -> amount - 1
                    else -> search.resultPos.value
                }
        }
    }

    /**
     * Visibly highlights found search results in the UI.
     *
     * @return amount of search results found
     */
    abstract fun highlightSearchResults(search: String): Int

    abstract fun selectSearchResult(resultPos: Int)

    private var navigationIconBeforeSearch: Drawable? = null

    fun startSearch() {
        binding.Toolbar.apply {
            menu.clear()
            search.nextMenuItem =
                menu
                    .add(R.string.previous, R.drawable.arrow_upward) {
                        search.resultPos.apply {
                            if (value > 0) {
                                value -= 1
                            } else {
                                value = search.results.value - 1
                            }
                        }
                    }
                    .setEnabled(false)
            search.prevMenuItem =
                menu
                    .add(R.string.next, R.drawable.arrow_downward) {
                        search.resultPos.apply {
                            if (value < search.results.value - 1) {
                                value += 1
                            } else {
                                value = 0
                            }
                        }
                    }
                    .setEnabled(false)
            setNavigationOnClickListener { endSearch() }
            navigationIconBeforeSearch = navigationIcon
            setNavigationIcon(R.drawable.close)
            setControlsContrastColorForAllViews(colorInt, overwriteBackground = false)
        }
        binding.EnterSearchKeyword.apply {
            visibility = VISIBLE
            requestFocus()
            showKeyboard(this)
        }
        binding.SearchResults.apply {
            text = ""
            visibility = VISIBLE
        }
    }

    protected fun isInSearchMode(): Boolean = binding.EnterSearchKeyword.visibility == VISIBLE

    protected fun updateJumpButtonsVisibility(manualSize: Int? = null) {
        jumpToTop?.post {
            val show =
                when (notallyModel.type) {
                    Type.NOTE ->
                        (manualSize ?: binding.EnterBody.lineCount) >
                            (if (isInLandscapeMode) 30 else 75)
                    Type.LIST ->
                        (manualSize ?: notallyModel.items.size) >
                            (if (isInLandscapeMode) 15 else 25)
                }
            jumpToTop?.isVisible = show
            jumpToBottom?.isVisible = show
        }
    }

    protected fun endSearch() {
        binding.EnterSearchKeyword.apply {
            visibility = GONE
            setText("")
        }
        binding.SearchResults.apply {
            visibility = GONE
            text = ""
        }
        resetToolbars()
        binding.Toolbar.navigationIcon = navigationIconBeforeSearch
        binding.Toolbar.setControlsContrastColorForAllViews(colorInt, overwriteBackground = false)
    }

    protected open fun initBottomMenu() {
        binding.BottomAppBarLeft.apply {
            removeAllViews()
            addIconButton(R.string.adding_files, R.drawable.add, marginStart = 0) {
                AddBottomSheet(actionHandler, colorInt)
                    .show(supportFragmentManager, AddBottomSheet.TAG)
            }
        }
        binding.BottomAppBarCenter.apply {
            removeAllViews()
            jumpToTop =
                addIconButton(
                    R.string.jump_to_top,
                    R.drawable.vertical_align_top,
                    marginStart = 0,
                ) {
                    binding.ScrollView.apply { post { fullScroll(View.FOCUS_UP) } }
                }
            undo =
                addIconButton(
                        R.string.undo,
                        R.drawable.undo,
                        marginStart = 2,
                        onLongClick = {
                            try {
                                changeHistory.undoAll()
                            } catch (e: ChangeHistory.ChangeHistoryException) {
                                application.log(TAG, throwable = e)
                            }
                            true
                        },
                    ) {
                        try {
                            changeHistory.undo()
                        } catch (e: ChangeHistory.ChangeHistoryException) {
                            application.log(TAG, throwable = e)
                        }
                    }
                    .apply { isEnabled = changeHistory.canUndo.value }

            redo =
                addIconButton(
                        R.string.redo,
                        R.drawable.redo,
                        marginStart = 2,
                        onLongClick = {
                            try {
                                changeHistory.redoAll()
                            } catch (e: ChangeHistory.ChangeHistoryException) {
                                application.log(TAG, throwable = e)
                            }
                            true
                        },
                    ) {
                        try {
                            changeHistory.redo()
                        } catch (e: ChangeHistory.ChangeHistoryException) {
                            application.log(TAG, throwable = e)
                        }
                    }
                    .apply { isEnabled = changeHistory.canRedo.value }
            jumpToBottom =
                addIconButton(
                    R.string.jump_to_bottom,
                    R.drawable.vertical_align_bottom,
                    marginStart = 2,
                ) {
                    binding.ScrollView.apply {
                        post {
                            val lastChild: View? = binding.ScrollView.getChildAt(0)
                            if (lastChild != null) {
                                val bottom: Int =
                                    lastChild.bottom + binding.ScrollView.paddingBottom
                                binding.ScrollView.smoothScrollTo(0, bottom)
                            } else {
                                fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    }
                }
            updateJumpButtonsVisibility()
        }
        updateBottomActions(preferences.editNoteActivityBottomAction.value)
        setBottomAppBarColor(colorInt)
    }

    protected open fun openMoreOptionsBottomSheet() {
        val prefs = NotallyXPreferences.getInstance(this@EditActivity)
        val topActions = prefs.getSafeEditNoteActivityTopActions()
        val bottomAction = prefs.editNoteActivityBottomAction.value

        MoreNoteBottomSheet(notallyModel, colorInt, actionHandler, topActions, bottomAction)
            .show(supportFragmentManager, MoreNoteBottomSheet.TAG)
    }

    private fun updateBottomActions(bottomAction: EditAction) {
        binding.BottomAppBarRight.apply {
            removeAllViews()

            addBottomAction(bottomAction)
            addIconButton(R.string.tap_for_more_options, R.drawable.more_vert, marginStart = 0) {
                openMoreOptionsBottomSheet()
            }
        }
    }

    fun ViewGroup.addBottomAction(action: EditAction) {
        val (title, icon) =
            action.getTitleAndIcon(
                notallyModel.pinned,
                notallyModel.viewMode.value,
                notallyModel.folder,
                notallyModel.type,
            )
        val button = addIconButton(title, icon) { actionHandler.handleAction(action) }

        // Try to get the view for long click
        post {
            button.setOnLongClickListener {
                showActionSelectionDialog(action, index = 0, isBottomBar = true)
                true
            }
        }
    }

    fun updateToggleViewMode() {
        updateTopActions(preferences.editNoteActivityTopActions.value)
        updateBottomActions(preferences.editNoteActivityBottomAction.value)
    }

    abstract fun configureUI()

    open fun setupListeners() {
        binding.EnterTitle.initHistory(changeHistory) { text ->
            notallyModel.title = text.trim().toString()
        }
        notallyModel.viewMode.observe(this) { value ->
            updateToggleViewMode()
            value?.let { toggleCanEdit(it) }
        }
        val textMaxLengthFilter = application.textMaxLengthFilter()
        binding.EnterTitle.filters = textMaxLengthFilter
        binding.EnterBody.filters = textMaxLengthFilter

        search.results.mergeSkipFirst(search.resultPos).observe(this) { (amount, pos) ->
            val hasResults = amount > 0
            binding.SearchResults.text = if (hasResults) "${pos + 1}/$amount" else "0"
            search.nextMenuItem?.isEnabled = hasResults
            search.prevMenuItem?.isEnabled = hasResults
        }

        search.resultPos.observeSkipFirst(this) { pos -> selectSearchResult(pos) }

        binding.EnterSearchKeyword.apply {
            doAfterTextChanged { text ->
                this@EditActivity.search.query = text.toString()
                updateSearchResults(this@EditActivity.search.query)
            }
        }

        preferences.editNoteActivityTopActions.observe(this) { topActions ->
            updateTopActions(topActions)
        }
        preferences.editNoteActivityBottomAction.observe(this) { bottomAction ->
            updateBottomActions(bottomAction)
        }
    }

    open fun setStateFromModel(savedInstanceState: Bundle?) {
        val (date, datePrefixResId) =
            when (preferences.notesSorting.value.sortedBy) {
                NotesSortBy.CREATION_DATE -> Pair(notallyModel.timestamp, R.string.creation_date)
                NotesSortBy.MODIFIED_DATE ->
                    Pair(notallyModel.modifiedTimestamp, R.string.modified_date)
                else -> Pair(null, null)
            }
        val dateFormat =
            if (preferences.applyDateFormatInNoteView.value) {
                preferences.dateFormat.value
            } else DateFormat.ABSOLUTE
        binding.Date.displayFormattedTimestamp(date, dateFormat, datePrefixResId)
        binding.EnterTitle.setText(notallyModel.title)
        bindLabels()
        setColor()
    }

    private fun bindLabels() {
        binding.LabelGroup.bindLabels(
            notallyModel.labels,
            notallyModel.textSize,
            paddingTop = true,
            colorInt,
            onClick = { label ->
                val bundle = Bundle()
                bundle.putString(EXTRA_DISPLAYED_LABEL, label)
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        putExtra(EXTRA_FRAGMENT_TO_OPEN, R.id.DisplayLabel)
                        putExtra(EXTRA_DISPLAYED_LABEL, label)
                        putExtra(EXTRA_SKIP_START_VIEW_ON_BACK, true)
                    }
                )
            },
            onLongClick = { label ->
                displayEditLabelDialog(label, baseModel) { oldLabel, newLabel ->
                    notallyModel.labels.apply {
                        remove(oldLabel)
                        add(newLabel)
                    }
                    bindLabels()
                }
            },
        )
    }

    private fun handleSharedNote() {
        val baseNote = intent.generateBaseNote(this)
        notallyModel.apply {
            body =
                Editable.Factory.getInstance().newEditable(baseNote.text).apply {
                    findWebUrls().forEach { (urlStart, urlEnd) ->
                        setSpan(
                            URLSpan(baseNote.text.substring(urlStart, urlEnd)),
                            urlStart,
                            urlEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
            title = baseNote.title
            addImages(baseNote.images.map { Uri.parse(it.originalName) }.toTypedArray())
            addFiles(baseNote.files.map { Uri.parse(it.originalName) }.toTypedArray())
        }
    }

    private fun setupImages() {
        val imageAdapter =
            PreviewImageAdapter(notallyModel.imageRoot) { position ->
                val intent =
                    Intent(this, ViewImageActivity::class.java).apply {
                        putExtra(ViewImageActivity.EXTRA_POSITION, position)
                        putExtra(EXTRA_SELECTED_BASE_NOTE, notallyModel.id)
                    }
                actionHandler.viewImagesActivityResultLauncher.launch(intent)
            }

        imageAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {

                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    binding.ImagePreview.scrollToPosition(positionStart)
                    binding.ImagePreviewPosition.text =
                        "${positionStart + 1}/${imageAdapter.itemCount}"
                }
            }
        )
        binding.ImagePreview.apply {
            setHasFixedSize(true)
            adapter = imageAdapter
            layoutManager = LinearLayoutManager(this@EditActivity, RecyclerView.HORIZONTAL, false)

            val pagerSnapHelper = PagerSnapHelper()
            pagerSnapHelper.attachToRecyclerView(this)
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val snappedView = pagerSnapHelper.findSnapView(layoutManager)
                            if (snappedView != null) {
                                val position = recyclerView.getChildAdapterPosition(snappedView)
                                binding.ImagePreviewPosition.text =
                                    "${position + 1}/${imageAdapter.itemCount}"
                            }
                        }
                    }
                }
            )
        }

        notallyModel.images.observe(this) { list ->
            imageAdapter.submitList(list)
            binding.ImagePreview.isVisible = list.isNotEmpty()
            binding.ImagePreviewPosition.isVisible = list.size > 1
        }
    }

    private fun setupFiles() {
        fileAdapter =
            PreviewFileAdapter({ fileAttachment ->
                if (notallyModel.filesRoot == null) {
                    return@PreviewFileAdapter
                }
                val intent =
                    Intent(Intent.ACTION_VIEW)
                        .apply {
                            val file = File(notallyModel.filesRoot, fileAttachment.localName)
                            val uri = this@EditActivity.getUriForFile(file)
                            setDataAndType(uri, fileAttachment.mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        .wrapWithChooser(this@EditActivity)
                startActivity(intent)
            }) { fileAttachment ->
                MaterialAlertDialogBuilder(this)
                    .setMessage(getString(R.string.delete_file, fileAttachment.originalName))
                    .setCancelButton()
                    .setPositiveButton(R.string.delete) { _, _ ->
                        notallyModel.deleteFiles(arrayListOf(fileAttachment))
                    }
                    .show()
                return@PreviewFileAdapter true
            }

        binding.FilesPreview.apply {
            setHasFixedSize(true)
            adapter = fileAdapter
            layoutManager =
                LinearLayoutManager(this@EditActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        notallyModel.files.observe(this) { list ->
            fileAdapter.submitList(list)
            val visible = list.isNotEmpty()
            binding.FilesPreview.apply {
                isVisible = visible
                if (visible) {
                    post {
                        scrollToPosition(fileAdapter.itemCount)
                        requestLayout()
                    }
                }
            }
        }
    }

    private fun displayFileErrors(errors: List<FileError>) {
        val recyclerView =
            RecyclerView(this).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                adapter = ErrorAdapter(errors)
                layoutManager = LinearLayoutManager(this@EditActivity)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scrollIndicators = View.SCROLL_INDICATOR_TOP or View.SCROLL_INDICATOR_BOTTOM
                }
            }

        val message =
            if (errors.isNotEmpty() && errors[0].fileType == NotallyModel.FileType.IMAGE) {
                R.plurals.cant_add_images
            } else {
                R.plurals.cant_add_files
            }
        val title = getQuantityString(message, errors.size)
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(recyclerView)
            .setCancelButton()
            .setCancelable(false)
            .show()
    }

    private fun setupAudios() {
        audioAdapter = AudioAdapter { position: Int ->
            if (position != -1) {
                val audio = notallyModel.audios.value[position]
                val intent = Intent(this, PlayAudioActivity::class.java)
                intent.putExtra(PlayAudioActivity.EXTRA_AUDIO, audio)
                actionHandler.playAudioActivityResultLauncher.launch(intent)
            }
        }
        binding.AudioRecyclerView.adapter = audioAdapter

        notallyModel.audios.observe(this) { list ->
            audioAdapter.submitList(list)
            binding.AudioHeader.isVisible = list.isNotEmpty()
            binding.AudioRecyclerView.isVisible = list.isNotEmpty()
        }
    }

    open fun setColor() {
        colorInt = extractColor(notallyModel.color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            changeStatusAndNavigationBarColor(colorInt)
            window.setLightStatusAndNavBar(colorInt.isLightColor())
        }
        binding.apply {
            ScrollView.apply {
                setBackgroundColor(colorInt)
                setControlsContrastColorForAllViews(colorInt)
            }
            root.setBackgroundColor(colorInt)
            MainListView.setBackgroundColor(colorInt)
            CheckedListView.setBackgroundColor(colorInt)
            Toolbar.backgroundTintList = ColorStateList.valueOf(colorInt)
            Toolbar.setControlsContrastColorForAllViews(colorInt)
        }
        setBottomAppBarColor(colorInt)
        if (::fileAdapter.isInitialized) fileAdapter.setColor(colorInt)
        if (::audioAdapter.isInitialized) audioAdapter.setColor(colorInt)
    }

    protected fun setBottomAppBarColor(@ColorInt color: Int) {
        binding.apply {
            BottomAppBar.setBackgroundColor(color)
            BottomAppBar.setControlsContrastColorForAllViews(color)
            BottomAppBarLayout.backgroundTint = ColorStateList.valueOf(color)
        }
    }

    private fun initialiseBinding() {
        binding = ActivityEditBinding.inflate(layoutInflater)
        when (type) {
            Type.NOTE -> {
                binding.AddItem.visibility = GONE
                binding.MainListView.visibility = GONE
                binding.CheckedListView.visibility = GONE
            }
            Type.LIST -> {
                binding.EnterBody.visibility = GONE
                binding.CheckedListView.visibility =
                    if (preferences.listItemSorting.value == ListItemSort.AUTO_SORT_BY_CHECKED)
                        VISIBLE
                    else GONE
            }
        }

        val title = notallyModel.textSize.editTitleSize
        val date = notallyModel.textSize.displayBodySize
        val body = notallyModel.textSize.editBodySize

        binding.EnterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, title)
        binding.Date.setTextSize(TypedValue.COMPLEX_UNIT_SP, date)
        binding.EnterBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        setupImages()
        setupFiles()
        setupAudios()
        notallyModel.addingFiles.setupProgressDialog(this)
        notallyModel.eventBus.observe(this) { event ->
            event.handle { errors -> displayFileErrors(errors) }
        }

        binding.root.isSaveFromParentEnabled = false
    }

    fun bindPinned() {
        updateTopActions(preferences.editNoteActivityTopActions.value)
        updateBottomActions(preferences.editNoteActivityBottomAction.value)
    }

    private fun updateTopActions(topActions: List<EditAction>) {
        binding.Toolbar.menu.apply {
            clear()
            topActions.forEachIndexed { idx, action ->
                val (title, icon) =
                    action.getTitleAndIcon(
                        notallyModel.pinned,
                        notallyModel.viewMode.value,
                        notallyModel.folder,
                        notallyModel.type,
                    )
                add(title, icon, MenuItem.SHOW_AS_ACTION_ALWAYS, itemId = idx) {
                    actionHandler.handleAction(action)
                }
                // Try to get the view for long click
                binding.Toolbar.post {
                    findViewById<View>(idx)?.setOnLongClickListener {
                        showActionSelectionDialog(action, idx)
                        true
                    }
                }
            }
        }
    }

    fun setupEditNoteReminderChip() {
        notallyModel.originalNote?.let { note ->
            binding.EditNoteReminderChip.setupReminderChip(note)
            binding.EditNoteReminderChip.setOnClickListener {
                val intent =
                    Intent(this@EditActivity, RemindersActivity::class.java)
                        .putExtra(RemindersActivity.NOTE_ID, note.id)
                startActivity(intent)
            }
        }
    }

    data class Search(
        var query: String = "",
        var prevMenuItem: MenuItem? = null,
        var nextMenuItem: MenuItem? = null,
        var resultPos: NotNullLiveData<Int> = NotNullLiveData(-1),
        var results: NotNullLiveData<Int> = NotNullLiveData(-1),
    )

    companion object {
        private const val TAG = "EditActivity"
        const val REQUEST_AUDIO_PERMISSION = 36

        const val EXTRA_SELECTED_BASE_NOTE = "notallyx.intent.extra.SELECTED_BASE_NOTE"
        const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.SELECTED_LABELS"
        const val EXTRA_NOTE_ID = "notallyx.intent.extra.NOTE_ID"
        const val EXTRA_FOLDER_FROM = "notallyx.intent.extra.FOLDER_FROM"
        const val EXTRA_FOLDER_TO = "notallyx.intent.extra.FOLDER_TO"
        const val EXTRA_INITIAL_SEARCH_QUERY = "notallyx.intent.extra.INITIAL_SEARCH_QUERY"

        val DEFAULT_EXCEPTION_HANDLER = Thread.getDefaultUncaughtExceptionHandler()
    }
}
