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
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.feature.editor.EditListActivity
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.handleRejection
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel.Companion.CURRENT_LABEL_NONE
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportNotes
import com.philkes.notallyx.feature.migration.progress.MigrationProgress
import com.philkes.notallyx.feature.reminder.RemindersActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : LockedActivity<ComposeBinding>() {

    private val notallyFragmentModel: NotesFragmentViewModel by viewModel()
    private val mainActivityViewModel: MainActivityViewModel by viewModel()

    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportNotesActivityResultLauncher: ActivityResultLauncher<Intent>

    private val currentNavKeyFlow = MutableStateFlow<NotallyNavKey>(NotallyNavKey.Notes)
    private val currentTitleFlow = MutableStateFlow("")

    private var isStartViewFragment = false
    private val actionModeCancelCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                notallyFragmentModel.actionMode.close(true)
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
                onStartViewChanged = { isStartView -> isStartViewFragment = isStartView },
                onPrepareNewNoteIntent = { intent -> prepareNewNoteIntent(intent) },
                onNoteClick = { note, pos -> handleNoteClick(note, pos) },
                onNoteLongClick = { note, pos -> handleNoteLongClick(note, pos) },
                onReminderClick = { note -> handleReminderClick(note) },
                onExportNotes = { mimeType -> exportSelectedNotes(mimeType) },
                onMoveSelectedNotes = { folderTo -> moveSelectedNotes(folderTo) },
                onDeleteSelectedNotesForever = { deleteSelectedNotesForever() },
                onRequestNotificationPermission = {
                    checkNotificationPermission(
                        NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
                        alsoCheckAlarmPermission = false,
                    ) {
                        notallyFragmentModel.pinBaseNotesToStatusBar(
                            this@MainActivity,
                            true,
                        )
                    }
                },
                onShowToast = { resId -> showToast(resId) },
                migrationProgressState = migrationProgressState,
                mainActivityViewModel = mainActivityViewModel,
                notallyFragmentModel = notallyFragmentModel,
            )
        }

        repeatOnLifecycleScope {
            launch { preferences.alwaysShowSearchBar.flow.collect { invalidateOptionsMenu() } }
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

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (notallyFragmentModel.actionMode.enabled.value) {
                        return
                    }
                    if (
                        !isStartViewFragment &&
                            !intent.getBooleanExtra(EXTRA_SKIP_START_VIEW_ON_BACK, false)
                    ) {
                        navigateToStartView()
                    } else {
                        finish()
                    }
                }
            },
        )
        onBackPressedDispatcher.addCallback(this, actionModeCancelCallback)

        checkForMigrations(savedInstanceState)
    }

    override fun initViewModel() {
        // Observers are started in checkForMigrations -> proceed()
    }

    private fun handleNoteClick(note: BaseNote, position: Int) {
        if (notallyFragmentModel.actionMode.isEnabled()) {
            handleNoteSelection(note.id, position, note)
        } else {
            val intent =
                Intent(
                        this,
                        if (note.type == Type.NOTE) EditNoteActivity::class.java
                        else EditListActivity::class.java,
                    )
                    .apply { putExtra(EXTRA_SELECTED_BASE_NOTE, note.id) }
            startActivity(intent)
        }
    }

    private fun handleNoteLongClick(note: BaseNote, position: Int) {
        if (notallyFragmentModel.actionMode.selectedNotes.isNotEmpty()) {
            if (notallyFragmentModel.actionMode.selectedNotes.contains(note.id)) {
                notallyFragmentModel.actionMode.remove(note.id)
            } else {
                notallyFragmentModel.actionMode.add(note.id, note)
            }
        } else {
            handleNoteSelection(note.id, position, note)
        }
    }

    private fun handleNoteSelection(id: Long, position: Int, baseNote: BaseNote) {
        if (notallyFragmentModel.actionMode.selectedNotes.contains(id)) {
            notallyFragmentModel.actionMode.remove(id)
        } else {
            notallyFragmentModel.actionMode.add(id, baseNote)
        }
    }

    private fun handleReminderClick(note: BaseNote) {
        if (notallyFragmentModel.actionMode.isEnabled()) {
            if (notallyFragmentModel.actionMode.selectedNotes.contains(note.id)) {
                notallyFragmentModel.actionMode.remove(note.id)
            } else {
                notallyFragmentModel.actionMode.add(note.id, note)
            }
            return
        }
        val intent =
            Intent(this, RemindersActivity::class.java).apply {
                putExtra(RemindersActivity.NOTE_ID, note.id)
            }
        startActivity(intent)
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
                    val baseNotes = notallyFragmentModel.actionMode.selectedNotes.values
                    notallyFragmentModel.pinBaseNotesToStatusBar(
                        this@MainActivity,
                        baseNotes.any { !it.isPinnedToStatus },
                    )
                } else handleRejection(R.string.to_pin_note_status_bar)
            }
        }
    }

    private fun checkForMigrations(savedInstanceState: Bundle?) {
        val proceed: () -> Unit = { mainActivityViewModel.startObserving() }
        if (preferences.dataSchemaId.value < LATEST_DATA_SCHEMA) {
            lifecycleScope.launch {
                setMigrationProgress(
                    MigrationProgress(R.string.migrating_data, indeterminate = true)
                )
                val migrationResult = application.runMigrations { titleId ->
                    setMigrationProgress(MigrationProgress(titleId, indeterminate = true))
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

    private fun getStartNavKey(): NotallyNavKey {
        intent.getNavKey()?.let {
            return it
        }
        return when (val startView = preferences.startView.value) {
            START_VIEW_DEFAULT -> NotallyNavKey.Notes
            START_VIEW_UNLABELED -> NotallyNavKey.Unlabeled
            else -> NotallyNavKey.DisplayLabel(startView)
        }
    }

    private fun navigateToStartView() {
        // Handled by onBackPressed or start key
    }

    private fun prepareNewNoteIntent(intent: Intent): Intent {
        val currentLabel = notallyFragmentModel.currentLabel
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
                        notallyFragmentModel.exportSelectedNoteToFile(uri, binding.root)
                    }
                }
            }
        exportNotesActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        notallyFragmentModel.exportSelectedNotesToFolder(uri, binding.root)
                    }
                }
            }
    }

    internal fun exportSelectedNotes(mimeType: ExportMimeType) {
        notallyFragmentModel.selectedExportMimeType = mimeType
        exportNotes(
            notallyFragmentModel.actionMode.selectedNotes.values,
            mimeType,
            exportFileActivityResultLauncher,
            exportNotesActivityResultLauncher,
        )
    }

    private fun moveSelectedNotes(folderTo: Folder) {
        if (
            notallyFragmentModel.actionMode.isLoading() || notallyFragmentModel.actionMode.isEmpty()
        ) {
            return
        }
        try {
            notallyFragmentModel.actionMode.setLoading(true)
            val folderFrom = notallyFragmentModel.actionMode.getFirstNote().folder
            val ids =
                notallyFragmentModel.moveBaseNotes(folderTo) {
                    notallyFragmentModel.actionMode.setLoading(false)
                }
            Snackbar.make(
                    binding.root,
                    getQuantityString(folderTo.movedToResId(), ids.size),
                    Snackbar.LENGTH_SHORT,
                )
                .apply {
                    setAction(R.string.undo) { notallyFragmentModel.moveBaseNotes(ids, folderFrom) }
                }
                .show()
        } catch (_: Exception) {
            notallyFragmentModel.actionMode.setLoading(false)
        }
    }

    private fun deleteSelectedNotesForever() {
        mainActivityViewModel.showConfirmDialog(
            message = getString(R.string.delete_selected_notes),
            positiveButtonText = getString(R.string.delete),
            onConfirm = {
                val removedNotes = notallyFragmentModel.actionMode.selectedNotes.values.toList()
                lifecycleScope.launch {
                    val deletedNotes = notallyFragmentModel.deleteSelectedBaseNotes()
                    Snackbar.make(
                            binding.root,
                            getQuantityString(
                                R.plurals.deleted_selected_notes,
                                removedNotes.size,
                            ),
                            Snackbar.LENGTH_SHORT,
                        )
                        .apply {
                            setAction(R.string.undo) {
                                notallyFragmentModel.saveNotes(removedNotes)
                            }
                            addCallback(
                                object : Snackbar.Callback() {
                                    override fun onDismissed(
                                        transientBottomBar: Snackbar?,
                                        event: Int,
                                    ) {
                                        if (event != DISMISS_EVENT_ACTION) {
                                            deleteAttachments(
                                                deletedNotes,
                                                notallyFragmentModel.progress,
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        .show()
                }
            },
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentNavKey = currentNavKeyFlow.value
        if (
            !preferences.alwaysShowSearchBar.value &&
                !ACTIVITES_WITHOUT_SEARCH.contains(currentNavKey)
        ) {
            val isInSearchFragment = currentNavKey is NotallyNavKey.Search
            val iconRes = if (isInSearchFragment) R.drawable.close else R.drawable.search
            val titleRes = if (isInSearchFragment) R.string.cancel else R.string.search

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
        const val EXTRA_ROUTE_TO_OPEN = "notallyx.intent.extra.FRAGMENT_TO_OPEN"
        const val EXTRA_SKIP_START_VIEW_ON_BACK = "notallyx.intent.extra.SKIP_START_VIEW_ON_BACK"
        const val EXTRA_DISPLAYED_LABEL = "notallyx.intent.extra.DISPLAYED_LABEL"
        private const val ACTION_SEARCH = 1001
        val ACTIVITES_WITHOUT_SEARCH =
            setOf(NotallyNavKey.Settings, NotallyNavKey.Reminders, NotallyNavKey.Labels)
    }
}
