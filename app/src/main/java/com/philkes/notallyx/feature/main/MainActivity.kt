package com.philkes.notallyx.feature.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.deleteAttachments
import com.philkes.notallyx.core.common.extensions.getNavKey
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.database.ConverterErrorReporter
import com.philkes.notallyx.core.database.LATEST_DATA_SCHEMA
import com.philkes.notallyx.core.database.runMigrations
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.movedToResId
import com.philkes.notallyx.core.ui.extensions.repeatOnLifecycleScope
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.generateBaseNote
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel.Companion.CURRENT_LABEL_NONE
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportNotes
import com.philkes.notallyx.feature.migration.progress.MigrationProgress
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : LockedActivity<ComposeBinding>() {

    private val notallyRouteModel: NotesRouteViewModel by viewModel()
    val mainActivityViewModel: MainActivityViewModel by viewModel()

    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportNotesActivityResultLauncher: ActivityResultLauncher<Intent>

    private val currentNavKeyFlow = MutableStateFlow<NotallyNavKey>(NotallyNavKey.Notes)
    private val currentTitleFlow = MutableStateFlow("")
    private val pendingNavKeyFlow = MutableStateFlow<NotallyNavKey?>(null)

    private var isStartViewRoute = false
    private var lastSelectedNotePosition = -1
    private val actionModeCancelCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                notallyRouteModel.actionMode.close(true)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val composeView = androidx.compose.ui.platform.ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        setupActivityResultLaunchers()

        val startKey = getStartNavKey()

        composeView.setContent {
            val migrationProgressState by migrationProgress.collectAsStateWithLifecycle()
            NotallyApp(
                startKey = startKey,
                activity = this@MainActivity,
                currentNavKeyFlow = currentNavKeyFlow,
                currentTitleFlow = currentTitleFlow,
                onStartViewChanged = { isStartView -> isStartViewRoute = isStartView },
                onPrepareNewNoteIntent = { intent -> prepareNewNoteIntent(intent) },
                onNoteClick = { note, pos -> handleNoteClick(note, pos) },
                onNoteLongClick = { note, pos, items -> handleNoteLongClick(note, pos, items) },
                onExportNotes = { mimeType -> exportSelectedNotes(mimeType) },
                onMoveSelectedNotes = { folderTo -> moveSelectedNotes(folderTo) },
                onDeleteSelectedNotesForever = { deleteSelectedNotesForever() },
                onRequestNotificationPermission = {
                    checkNotificationPermission(
                        NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
                        onShowRationale = { msgResId, onConfirm ->
                            mainActivityViewModel.activeDialog.value =
                                MainActiveDialog.Confirm(
                                    message = getString(msgResId),
                                    positiveButtonText = getString(R.string.continue_),
                                    onConfirm = onConfirm,
                                )
                        },
                    ) {
                        notallyRouteModel.pinBaseNotesToStatusBar(
                            this@MainActivity,
                            true,
                        )
                    }
                },
                onShowToast = { resId -> showToast(resId) },
                migrationProgressState = migrationProgressState,
                skipStartViewOnBack = intent.getBooleanExtra(EXTRA_SKIP_START_VIEW_ON_BACK, false),
                pendingNavKeyFlow = pendingNavKeyFlow,
                mainActivityViewModel = mainActivityViewModel,
                notallyRouteModel = notallyRouteModel,
            )
        }

        repeatOnLifecycleScope {
            launch { preferences.alwaysShowSearchBar.flow.collect { invalidateOptionsMenu() } }
            launch {
                notallyRouteModel.actionMode.enabled.collect { enabled ->
                    actionModeCancelCallback.isEnabled = enabled
                    if (!enabled) {
                        lastSelectedNotePosition = -1
                    }
                }
            }
        }

        ConverterErrorReporter.errors.collectIn(this@MainActivity) { throwable ->
            if (throwable == null) {
                return@collectIn
            }
            application.log("Converters", throwable = throwable)
            mainActivityViewModel.activeDialog.value =
                MainActiveDialog.Error(
                    title = getString(R.string.error),
                    message = getString(R.string.error_loading_data, getString(R.string.clean_up)),
                    details = throwable.stackTraceToString(),
                    neutralButtonText = getString(R.string.clean_up),
                    onNeutralButtonClick = {
                        ConverterErrorReporter.dismissAllDialogs()
                        mainActivityViewModel.cleanupDatabase {
                            showToast(getString(R.string.cleanup_finished_title))
                        }
                    },
                )
        }

        onBackPressedDispatcher.addCallback(this, actionModeCancelCallback)

        checkForMigrations(savedInstanceState)
    }

    override fun initViewModel() {
        // Observers are started in checkForMigrations -> proceed()
    }

    private fun handleNoteClick(note: BaseNote, position: Int) {
        if (notallyRouteModel.actionMode.isEnabled()) {
            handleNoteSelection(note.id, position, note)
        }
    }

    private fun handleNoteLongClick(
        note: BaseNote,
        position: Int,
        items: List<Item> = emptyList(),
    ) {
        val selectedNotes = notallyRouteModel.actionMode.selectedNotes
        if (selectedNotes.isNotEmpty()) {
            val anchorPosition =
                if (
                    lastSelectedNotePosition != -1 &&
                        items.getOrNull(lastSelectedNotePosition)?.let {
                            it is BaseNote && selectedNotes.contains(it.id)
                        } == true
                ) {
                    lastSelectedNotePosition
                } else if (items.isNotEmpty()) {
                    items.indexOfFirst { it is BaseNote && selectedNotes.contains(it.id) }
                } else {
                    -1
                }

            if (anchorPosition != -1 && items.isNotEmpty()) {
                val start = minOf(anchorPosition, position)
                val end = maxOf(anchorPosition, position)
                val notesToAdd =
                    (start..end)
                        .mapNotNull { pos -> items.getOrNull(pos) as? BaseNote }
                        .filter { !selectedNotes.contains(it.id) }
                if (notesToAdd.isNotEmpty()) {
                    notallyRouteModel.actionMode.add(notesToAdd)
                } else if (!selectedNotes.contains(note.id)) {
                    notallyRouteModel.actionMode.add(note.id, note)
                }
                lastSelectedNotePosition = position
            } else {
                handleNoteSelection(note.id, position, note)
            }
        } else {
            handleNoteSelection(note.id, position, note)
        }
    }

    private fun handleNoteSelection(id: Long, position: Int, baseNote: BaseNote) {
        if (notallyRouteModel.actionMode.selectedNotes.contains(id)) {
            notallyRouteModel.actionMode.remove(id)
            if (notallyRouteModel.actionMode.isEmpty()) {
                lastSelectedNotePosition = -1
            }
        } else {
            notallyRouteModel.actionMode.add(id, baseNote)
            lastSelectedNotePosition = position
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS -> {
                if (
                    grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    val baseNotes = notallyRouteModel.actionMode.selectedNotes.values
                    notallyRouteModel.pinBaseNotesToStatusBar(
                        this@MainActivity,
                        baseNotes.any { !it.isPinnedToStatus },
                    )
                } else handleRejection(R.string.to_pin_note_status_bar)
            }
        }
    }

    private fun handleRejection(msgResId: Int) {
        mainActivityViewModel.showConfirmDialog(
            message = getString(msgResId),
            positiveButtonText = getString(R.string.settings),
            onConfirm = {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            },
        )
    }

    private fun checkForMigrations(savedInstanceState: Bundle?) {
        val proceed: () -> Unit = { mainActivityViewModel.startObserving() }
        if (preferences.dataSchemaId.value < LATEST_DATA_SCHEMA) {
            lifecycleScope.launch {
                setMigrationProgress(MigrationProgress(R.string.migrating_data))
                val migrationResult = application.runMigrations { titleId ->
                    setMigrationProgress(MigrationProgress(titleId))
                }
                setMigrationProgress(MigrationProgress(R.string.migrating_data, inProgress = false))
                if (migrationResult.showBiometricWarning) {
                    showBiometricEncryptionWarningDialog()
                }
                proceed()
            }
        } else {
            proceed()
        }
    }

    private fun showBiometricEncryptionWarningDialog() {
        mainActivityViewModel.showConfirmDialog(
            title = getString(R.string.biometric_lock),
            message = getString(R.string.biometric_encryption_experimental_warning),
            positiveButtonText = getString(R.string.decrypt),
            negativeButtonText = getString(android.R.string.ok),
            onConfirm = {
                AuthenticatorProvider.instance.authenticate(
                    this,
                    preferences.iv.value,
                    isForDecrypt = true,
                    onSuccess = { cipher ->
                        DatabaseTransitionActivity.start(
                            this,
                            enableBiometricLock = true,
                            encryptDatabase = false,
                            cipher = cipher,
                        )
                    },
                ) {}
            },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val key = resolveNavKeyFromIntent(intent)
        if (key != null) {
            pendingNavKeyFlow.value = key
        }
    }

    private fun resolveNavKeyFromIntent(intent: Intent): NotallyNavKey? {
        intent.getNavKey()?.let {
            return it
        }

        val className = intent.component?.className ?: ""

        if (className.endsWith("EditTextPlainActivity")) {
            return NotallyNavKey.EditTextPlain(fileUri = intent.data?.toString())
        }

        if (intent.hasExtra(EXTRA_SELECTED_BASE_NOTE)) {
            val id = intent.getLongExtra(EXTRA_SELECTED_BASE_NOTE, 0L)
            val type = intent.getStringExtra("type")
            return if (type == Type.LIST.name || className.endsWith("EditListActivity")) {
                NotallyNavKey.EditList(noteId = id)
            } else {
                NotallyNavKey.EditNote(noteId = id)
            }
        }

        if (className.endsWith("EditListActivity")) {
            return NotallyNavKey.EditList()
        }
        if (className.endsWith("EditNoteActivity")) {
            if (
                intent.action == Intent.ACTION_SEND ||
                    intent.action == Intent.ACTION_SEND_MULTIPLE ||
                    (intent.action == Intent.ACTION_VIEW && intent.data != null)
            ) {
                val shared = intent.generateBaseNote(this)
                return NotallyNavKey.EditNote(
                    initialTitle = shared.title.ifEmpty { null },
                    initialBody = shared.text.ifEmpty { null },
                    initialImageUris = shared.images.map { it.originalName },
                    initialFileUris = shared.files.map { it.originalName },
                )
            }
            return NotallyNavKey.EditNote()
        }

        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val shared = intent.generateBaseNote(this)
            return NotallyNavKey.EditNote(
                initialTitle = shared.title.ifEmpty { null },
                initialBody = shared.text.ifEmpty { null },
                initialImageUris = shared.images.map { it.originalName },
                initialFileUris = shared.files.map { it.originalName },
            )
        }

        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val mimeType = intent.type ?: intent.data?.let { contentResolver.getType(it) }
            if (
                mimeType?.startsWith("text/") == true ||
                    mimeType == "application/json" ||
                    mimeType == "application/xml"
            ) {
                return NotallyNavKey.EditTextPlain(fileUri = intent.data?.toString())
            }
        }

        return null
    }

    private fun getStartNavKey(): NotallyNavKey {
        resolveNavKeyFromIntent(intent)?.let {
            return it
        }
        return when (val startView = preferences.startView.value) {
            START_VIEW_DEFAULT -> NotallyNavKey.Notes
            START_VIEW_UNLABELED -> NotallyNavKey.Unlabeled
            else -> NotallyNavKey.DisplayLabel(startView)
        }
    }

    private fun prepareNewNoteIntent(intent: Intent): Intent {
        val currentLabel = notallyRouteModel.currentLabel
        if (
            !currentLabel.isNullOrEmpty() &&
                currentLabel != CURRENT_LABEL_EMPTY &&
                currentLabel != CURRENT_LABEL_NONE
        ) {
            intent.putExtra(EXTRA_DISPLAYED_LABEL, currentLabel)
        }
        return intent
    }

    private fun setupActivityResultLaunchers() {
        exportFileActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        notallyRouteModel.exportSelectedNoteToFile(uri, binding.root)
                    }
                }
            }
        exportNotesActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        notallyRouteModel.exportSelectedNotesToFolder(uri, binding.root)
                    }
                }
            }
    }

    internal fun exportSelectedNotes(mimeType: ExportMimeType) {
        notallyRouteModel.selectedExportMimeType = mimeType
        exportNotes(
            notallyRouteModel.actionMode.selectedNotes.values,
            mimeType,
            exportFileActivityResultLauncher,
            exportNotesActivityResultLauncher,
        )
    }

    private fun moveSelectedNotes(folderTo: Folder) {
        if (notallyRouteModel.actionMode.isLoading() || notallyRouteModel.actionMode.isEmpty()) {
            return
        }
        try {
            notallyRouteModel.actionMode.setLoading(true)
            val folderFrom = notallyRouteModel.actionMode.getFirstNote().folder
            val ids =
                notallyRouteModel.moveBaseNotes(folderTo) {
                    notallyRouteModel.actionMode.setLoading(false)
                }
            Snackbar.make(
                    binding.root,
                    getQuantityString(folderTo.movedToResId(), ids.size),
                    Snackbar.LENGTH_SHORT,
                )
                .apply {
                    setAction(R.string.undo) { notallyRouteModel.moveBaseNotes(ids, folderFrom) }
                }
                .show()
        } catch (_: Exception) {
            notallyRouteModel.actionMode.setLoading(false)
        }
    }

    private fun deleteSelectedNotesForever() {
        val removedNotes = notallyRouteModel.actionMode.selectedNotes.values.toList()
        lifecycleScope.launch {
            val deletedNotes = notallyRouteModel.deleteSelectedBaseNotes()
            Snackbar.make(
                    binding.root,
                    getQuantityString(
                        R.plurals.deleted_selected_notes,
                        removedNotes.size,
                    ),
                    Snackbar.LENGTH_SHORT,
                )
                .apply {
                    setAction(R.string.undo) { notallyRouteModel.saveNotes(removedNotes) }
                    addCallback(
                        object : Snackbar.Callback() {
                            override fun onDismissed(
                                transientBottomBar: Snackbar?,
                                event: Int,
                            ) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    deleteAttachments(
                                        deletedNotes,
                                        notallyRouteModel.progress,
                                    )
                                }
                            }
                        }
                    )
                }
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentNavKey = currentNavKeyFlow.value
        if (
            !preferences.alwaysShowSearchBar.value &&
                !ACTIVITES_WITHOUT_SEARCH.contains(currentNavKey)
        ) {
            val isInSearchRoute = currentNavKey is NotallyNavKey.Search
            val iconRes = if (isInSearchRoute) R.drawable.close else R.drawable.search
            val titleRes = if (isInSearchRoute) R.string.cancel else R.string.search

            menu
                .add(Menu.NONE, ACTION_SEARCH, Menu.NONE, titleRes)
                .setIcon(iconRes)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ACTION_SEARCH -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_SELECTED_BASE_NOTE = "notallyx.intent.extra.EXTRA_SELECTED_BASE_NOTE"
        const val EXTRA_ROUTE_TO_OPEN = "notallyx.intent.extra.EXTRA_TO_OPEN"
        const val EXTRA_SKIP_START_VIEW_ON_BACK = "notallyx.intent.extra.SKIP_START_VIEW_ON_BACK"
        const val EXTRA_DISPLAYED_LABEL = "notallyx.intent.extra.DISPLAYED_LABEL"
        private const val ACTION_SEARCH = 1001
        val ACTIVITES_WITHOUT_SEARCH =
            setOf(NotallyNavKey.Settings, NotallyNavKey.Reminders, NotallyNavKey.Labels)
    }
}
