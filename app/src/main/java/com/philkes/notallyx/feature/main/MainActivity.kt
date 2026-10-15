package com.philkes.notallyx.feature.main

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.deleteAttachments
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.ConverterErrorReporter
import com.philkes.notallyx.core.database.LATEST_DATA_SCHEMA
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.database.runMigrations
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.dialog.ColorPickerDialog
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TriStateMultiChoiceDialog
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.movedToResId
import com.philkes.notallyx.core.ui.extensions.repeatOnLifecycleScope
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.EditListActivity
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.handleRejection
import com.philkes.notallyx.feature.labels.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.feature.labels.LabelsFragment
import com.philkes.notallyx.feature.main.tab.NotesFragment
import com.philkes.notallyx.feature.main.tab.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.tab.NotesFragmentViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.main.tab.NotesFragmentViewModel.Companion.CURRENT_LABEL_NONE
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportNotes
import com.philkes.notallyx.feature.migration.progress.MigrationProgress
import com.philkes.notallyx.feature.search.SearchFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : LockedActivity<ComposeBinding>() {

    private val notallyFragmentModel: NotesFragmentViewModel by viewModel()
    private val mainActivityViewModel: MainActivityViewModel by viewModel()

    private var navController: NavController? = null
    private var configuration: AppBarConfiguration? = null
    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportNotesActivityResultLauncher: ActivityResultLauncher<Intent>

    private val currentDestinationIdFlow = MutableStateFlow(R.id.Notes)
    private val currentTitleFlow = MutableStateFlow("")

    private var isStartViewFragment = false
    private val actionModeCancelCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                notallyFragmentModel.actionMode.close(true)
            }
        }

    var getCurrentFragmentNotes: (() -> Collection<BaseNote>?)? = null

    override fun onSupportNavigateUp(): Boolean {
        notallyFragmentModel.keyword = ""
        val config = configuration ?: return false
        return navController?.navigateUp(config) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val composeView = ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        setupActivityResultLaunchers()

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                val activeDialog by mainActivityViewModel.activeDialog.collectAsStateWithLifecycle()
                val mainProgress by mainActivityViewModel.progress.collectAsStateWithLifecycle()
                val fragmentProgress by notallyFragmentModel.progress.collectAsStateWithLifecycle()
                val migrationProgressState by migrationProgress.collectAsStateWithLifecycle()

                val labelsState =
                    mainActivityViewModel.labels.collectAsStateWithLifecycle(emptyList())
                val actionModeEnabledState =
                    notallyFragmentModel.actionMode.enabled.collectAsStateWithLifecycle(false)
                val selectedNoteIds by
                    notallyFragmentModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(
                        emptySet()
                    )
                val selectedNotesCount = selectedNoteIds.size

                val currentDestinationId by currentDestinationIdFlow.collectAsStateWithLifecycle()
                val currentTitleState by currentTitleFlow.collectAsStateWithLifecycle()
                val currentTitle = currentTitleState.ifEmpty { getString(R.string.app_name) }

                val currentFolder by
                    notallyFragmentModel.folder.collectAsStateWithLifecycle(Folder.NOTES)

                val selectedNotes = notallyFragmentModel.actionMode.selectedNotes.values
                val isAllPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned }
                val isAllPinnedToStatus =
                    selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinnedToStatus }

                val actionModeListener = remember {
                    object : ActionModeListener {
                        override fun onClose() {
                            notallyFragmentModel.actionMode.close(true)
                        }

                        override fun onSelectAll() {
                            getCurrentFragmentNotes?.invoke()?.let { notes ->
                                notallyFragmentModel.actionMode.add(notes)
                            }
                        }

                        override fun onPin() {
                            val baseNotes = notallyFragmentModel.actionMode.selectedNotes.values
                            val allPinned = baseNotes.isNotEmpty() && baseNotes.all { it.pinned }
                            notallyFragmentModel.pinBaseNotes(!allPinned)
                        }

                        override fun onLabels() {
                            val baseNotes = notallyFragmentModel.actionMode.selectedNotes.values
                            lifecycleScope.launch {
                                val labels = notallyFragmentModel.getAllLabels()
                                if (labels.isNotEmpty()) {
                                    displaySelectLabelsDialog(labels, baseNotes)
                                } else {
                                    notallyFragmentModel.actionMode.close(true)
                                    navigateWithAnimation(R.id.Labels)
                                }
                            }
                        }

                        override fun onDelete() {
                            moveSelectedNotes(Folder.DELETED)
                        }

                        override fun onRestore() {
                            moveSelectedNotes(Folder.NOTES)
                        }

                        override fun onArchive() {
                            moveSelectedNotes(Folder.ARCHIVED)
                        }

                        override fun onDeleteForever() {
                            deleteSelectedNotesForever()
                        }

                        override fun onDuplicate() {
                            notallyFragmentModel.duplicateSelectedBaseNotes()
                        }

                        override fun onChangeColor() {
                            lifecycleScope.launch {
                                val database =
                                    NotallyDatabase.getDatabase(this@MainActivity).value
                                        ?: return@launch
                                val colors =
                                    withContext(Dispatchers.IO) {
                                        database.getBaseNoteDao().getAllColors().toSet()
                                    }
                                val currentColor =
                                    notallyFragmentModel.actionMode.selectedNotes.values
                                        .map { it.color }
                                        .distinct()
                                        .takeIf { it.size == 1 }
                                        ?.firstOrNull()
                                mainActivityViewModel.activeDialog.value =
                                    MainActiveDialog.ColorPicker(
                                        colors = colors,
                                        currentColor = currentColor,
                                        onColorSelected = { selectedColor ->
                                            if (selectedColor == BaseNote.COLOR_NEW) {
                                                mainActivityViewModel.activeDialog.value =
                                                    MainActiveDialog.ColorPicker(
                                                        colors =
                                                            colors +
                                                                preferences.defaultNoteColor.value,
                                                        currentColor = null,
                                                        onColorSelected = { color ->
                                                            notallyFragmentModel.colorBaseNote(
                                                                color
                                                            )
                                                        },
                                                    )
                                            } else {
                                                notallyFragmentModel.colorBaseNote(selectedColor)
                                            }
                                        },
                                    )
                            }
                        }

                        override fun onPinToStatus() {
                            val baseNotes = notallyFragmentModel.actionMode.selectedNotes.values
                            val anyNotPinned = baseNotes.any { !it.isPinnedToStatus }
                            if (anyNotPinned) {
                                checkNotificationPermission(
                                    NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
                                    alsoCheckAlarmPermission = false,
                                ) {
                                    notallyFragmentModel.pinBaseNotesToStatusBar(
                                        this@MainActivity,
                                        true,
                                    )
                                }
                            } else {
                                notallyFragmentModel.pinBaseNotesToStatusBar(
                                    this@MainActivity,
                                    false,
                                )
                            }
                        }

                        override fun onShare() {
                            if (!notallyFragmentModel.actionMode.isEmpty()) {
                                val baseNote = notallyFragmentModel.actionMode.getFirstNote()
                                shareNote(baseNote)
                            }
                        }

                        override fun onExport() {
                            displayExportFormatDialog()
                        }
                    }
                }

                MainScreen(
                    title = currentTitle,
                    labels = labelsState.value,
                    selectedDestinationId = currentDestinationId,
                    actionModeEnabled = actionModeEnabledState.value,
                    selectedCount = selectedNotesCount,
                    currentFolder = currentFolder,
                    isAllPinned = isAllPinned,
                    isAllPinnedToStatus = isAllPinnedToStatus,
                    showFabs =
                        currentDestinationId == R.id.Notes ||
                            currentDestinationId == R.id.DisplayLabel ||
                            currentDestinationId == R.id.Unlabeled,
                    showSearchOption =
                        !preferences.alwaysShowSearchBar.value &&
                            !ACTIVITES_WITHOUT_SEARCH.contains(currentDestinationId),
                    isInSearchMode = currentDestinationId == R.id.Search,
                    onNavigationItemClick = { destinationId, labelArg ->
                        if (destinationId == R.id.DisplayLabel && labelArg != null) {
                            navigateToLabel(labelArg)
                        } else {
                            navigateWithAnimation(destinationId)
                        }
                    },
                    onTakeNoteClick = {
                        val intent = Intent(this@MainActivity, EditNoteActivity::class.java)
                        startActivity(prepareNewNoteIntent(intent))
                    },
                    onMakeListClick = {
                        val intent = Intent(this@MainActivity, EditListActivity::class.java)
                        startActivity(prepareNewNoteIntent(intent))
                    },
                    onSearchClick = {
                        if (currentDestinationId == R.id.Search) {
                            notallyFragmentModel.keyword = ""
                            navController?.popBackStack()
                        } else {
                            navController?.navigate(
                                R.id.Search,
                                Bundle().apply {
                                    putSerializable(
                                        SearchFragment.EXTRA_INITIAL_FOLDER,
                                        notallyFragmentModel.folder.value,
                                    )
                                    putSerializable(
                                        SearchFragment.EXTRA_INITIAL_LABEL,
                                        notallyFragmentModel.currentLabel,
                                    )
                                },
                            )
                        }
                    },
                    onAddClick =
                        if (currentDestinationId == R.id.Labels) {
                            {
                                val navHost =
                                    supportFragmentManager.findFragmentById(R.id.NavHostFragment)
                                        as? NavHostFragment
                                val labelsFragment =
                                    navHost?.childFragmentManager?.fragments?.firstOrNull()
                                        as? LabelsFragment
                                labelsFragment?.displayAddLabelDialog()
                            }
                        } else null,
                    actionModeListener = actionModeListener,
                ) {
                    AndroidView(
                        factory = { context ->
                            FragmentContainerView(context).apply {
                                id = R.id.NavHostFragment
                                addOnAttachStateChangeListener(
                                    object : android.view.View.OnAttachStateChangeListener {
                                        override fun onViewAttachedToWindow(v: android.view.View) {
                                            val existingFragment =
                                                supportFragmentManager.findFragmentById(id)
                                                    as? NavHostFragment
                                            val fragment =
                                                existingFragment
                                                    ?: NavHostFragment.create(
                                                            R.navigation.navigation
                                                        )
                                                        .also { navHost ->
                                                            supportFragmentManager
                                                                .beginTransaction()
                                                                .replace(id, navHost)
                                                                .setPrimaryNavigationFragment(
                                                                    navHost
                                                                )
                                                                .commitNowAllowingStateLoss()
                                                        }
                                            navController = fragment.navController
                                            configuration =
                                                AppBarConfiguration(fragment.navController.graph)
                                            setupNavigationListeners()
                                            checkForMigrations(savedInstanceState)
                                        }

                                        override fun onViewDetachedFromWindow(
                                            v: android.view.View
                                        ) {}
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                MainActivityDialogs(
                    activeDialog = activeDialog,
                    mainProgress = mainProgress,
                    fragmentProgress = fragmentProgress,
                    migrationProgress = migrationProgressState,
                    onDismissDialog = { mainActivityViewModel.activeDialog.value = null },
                )
            }
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
    }

    override fun initViewModel() {
        // Observers are started in checkForMigrations -> proceed()
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
        val proceed: () -> Unit = {
            mainActivityViewModel.startObserving()
            val fragmentIdToLoad = intent.getIntExtra(EXTRA_FRAGMENT_TO_OPEN, -1)
            if (fragmentIdToLoad != -1) {
                navController?.navigate(fragmentIdToLoad, intent.extras)
            } else if (savedInstanceState == null) {
                navigateToStartView()
            }
        }
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
        mainActivityViewModel.activeDialog.value =
            MainActiveDialog.Confirm(
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

    private fun getStartViewNavigation(): Pair<Int, Bundle> {
        return when (val startView = preferences.startView.value) {
            START_VIEW_DEFAULT -> Pair(R.id.Notes, Bundle())
            START_VIEW_UNLABELED -> Pair(R.id.Unlabeled, Bundle())
            else -> {
                val bundle = Bundle().apply { putString(EXTRA_DISPLAYED_LABEL, startView) }
                Pair(R.id.DisplayLabel, bundle)
            }
        }
    }

    private fun navigateToStartView() {
        val (id, bundle) = getStartViewNavigation()
        navController?.navigate(id, bundle)
    }

    private fun prepareNewNoteIntent(intent: Intent): Intent {
        return supportFragmentManager
            .findFragmentById(R.id.NavHostFragment)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull()
            ?.let { fragment ->
                return if (fragment is NotesFragment) {
                    fragment.prepareNewNoteIntent(intent)
                } else intent
            } ?: intent
    }

    private fun navigateToLabel(label: String) {
        val bundle = Bundle().apply { putString(EXTRA_DISPLAYED_LABEL, label) }
        navController?.navigate(R.id.DisplayLabel, bundle)
    }

    private fun setupNavigationListeners() {
        navController?.addOnDestinationChangedListener { _, destination, bundle ->
            currentDestinationIdFlow.value = destination.id
            currentTitleFlow.value = destination.label?.toString() ?: getString(R.string.app_name)
            when (destination.id) {
                R.id.DisplayLabel ->
                    bundle?.getString(EXTRA_DISPLAYED_LABEL)?.let {
                        notallyFragmentModel.currentLabel = it
                    }
                R.id.Unlabeled -> {
                    notallyFragmentModel.currentLabel = CURRENT_LABEL_NONE
                }
                else -> {
                    notallyFragmentModel.currentLabel = CURRENT_LABEL_EMPTY
                }
            }
            isStartViewFragment = isStartViewFragment(destination.id, bundle)
        }
    }

    private fun isStartViewFragment(id: Int, bundle: Bundle?): Boolean {
        val (startViewId, startViewBundle) = getStartViewNavigation()
        return startViewId == id &&
            startViewBundle.getString(EXTRA_DISPLAYED_LABEL) ==
                bundle?.getString(EXTRA_DISPLAYED_LABEL)
    }

    internal fun navigateWithAnimation(id: Int) {
        val options = navOptions {
            launchSingleTop = true
            anim {
                exit = androidx.navigation.ui.R.anim.nav_default_exit_anim
                enter = androidx.navigation.ui.R.anim.nav_default_enter_anim
                popExit = androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
                popEnter = androidx.navigation.ui.R.anim.nav_default_pop_enter_anim
            }
            val startId = navController?.graph?.startDestinationId ?: R.id.Notes
            popUpTo(startId) { inclusive = false }
        }
        navController?.navigate(id, null, options)
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

    private fun displayExportFormatDialog() {
        mainActivityViewModel.activeDialog.value =
            MainActiveDialog.ExportFormat(
                formats = ExportMimeType.entries,
                onFormatSelected = { mimeType -> exportSelectedNotes(mimeType) },
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
        mainActivityViewModel.activeDialog.value =
            MainActiveDialog.Confirm(
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

    private fun displaySelectLabelsDialog(labels: Array<String>, baseNotes: Collection<BaseNote>) {
        val checkedPositions =
            labels
                .map { label ->
                    if (baseNotes.all { it.labels.contains(label) }) {
                        ToggleableState.On
                    } else if (baseNotes.any { it.labels.contains(label) }) {
                        ToggleableState.Indeterminate
                    } else {
                        ToggleableState.Off
                    }
                }
                .toTypedArray()

        mainActivityViewModel.activeDialog.value =
            MainActiveDialog.SelectLabels(
                labels = labels,
                initialStates = checkedPositions,
                onConfirm = { finalStates ->
                    val checkedLabels = finalStates.mapIndexedNotNull { index, checked ->
                        if (checked == ToggleableState.On) {
                            labels[index]
                        } else null
                    }
                    val uncheckedLabels = finalStates.mapIndexedNotNull { index, checked ->
                        if (checked == ToggleableState.Off) {
                            labels[index]
                        } else null
                    }
                    val updatedBaseNotesLabels = baseNotes.map { baseNote ->
                        val noteLabels = baseNote.labels.toMutableList()
                        checkedLabels.forEach { checkedLabel ->
                            if (!noteLabels.contains(checkedLabel)) {
                                noteLabels.add(checkedLabel)
                            }
                        }
                        uncheckedLabels.forEach { uncheckedLabel ->
                            if (noteLabels.contains(uncheckedLabel)) {
                                noteLabels.remove(uncheckedLabel)
                            }
                        }
                        noteLabels
                    }
                    baseNotes.zip(updatedBaseNotesLabels).forEach { (baseNote, updatedLabels) ->
                        notallyFragmentModel.updateBaseNoteLabels(updatedLabels, baseNote.id)
                    }
                    notallyFragmentModel.actionMode.close(true)
                },
            )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentDestinationId = navController?.currentDestination?.id
        if (
            !preferences.alwaysShowSearchBar.value &&
                !ACTIVITES_WITHOUT_SEARCH.contains(currentDestinationId)
        ) {
            val isInSearchFragment = currentDestinationId == R.id.Search
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
                val isInSearchFragment = navController?.currentDestination?.id == R.id.Search
                if (isInSearchFragment) {
                    notallyFragmentModel.keyword = ""
                    navController?.popBackStack()
                } else {
                    navController?.navigate(
                        R.id.Search,
                        Bundle().apply {
                            putSerializable(
                                SearchFragment.EXTRA_INITIAL_FOLDER,
                                notallyFragmentModel.folder.value,
                            )
                            putSerializable(
                                SearchFragment.EXTRA_INITIAL_LABEL,
                                notallyFragmentModel.currentLabel,
                            )
                        },
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Composable
    private fun MainActivityDialogs(
        activeDialog: MainActiveDialog?,
        mainProgress: Progress?,
        fragmentProgress: Progress?,
        migrationProgress: MigrationProgress?,
        onDismissDialog: () -> Unit,
    ) {
        when (activeDialog) {
            is MainActiveDialog.ColorPicker -> {
                ColorPickerDialog(
                    currentColor = activeDialog.currentColor ?: BaseNote.COLOR_DEFAULT,
                    onDismissRequest = onDismissDialog,
                    onColorSelected = { color ->
                        onDismissDialog()
                        activeDialog.onColorSelected(color)
                    },
                )
            }
            is MainActiveDialog.ExportFormat -> {
                ListDialog(
                    title = stringResource(R.string.export),
                    items = activeDialog.formats.map { it.name },
                    onDismissRequest = onDismissDialog,
                    onItemClick = { index ->
                        onDismissDialog()
                        activeDialog.onFormatSelected(activeDialog.formats[index])
                    },
                )
            }
            is MainActiveDialog.SelectLabels -> {
                TriStateMultiChoiceDialog(
                    title = stringResource(R.string.labels),
                    items = activeDialog.labels,
                    initialStates = activeDialog.initialStates,
                    onDismissRequest = onDismissDialog,
                    onConfirm = { finalStates ->
                        onDismissDialog()
                        activeDialog.onConfirm(finalStates)
                    },
                )
            }
            is MainActiveDialog.Confirm -> {
                ConfirmDialog(
                    title = activeDialog.title,
                    message = activeDialog.message,
                    positiveButtonText =
                        activeDialog.positiveButtonText ?: stringResource(R.string.delete),
                    negativeButtonText =
                        activeDialog.negativeButtonText ?: stringResource(R.string.cancel),
                    onDismissRequest = onDismissDialog,
                    onConfirm = {
                        onDismissDialog()
                        activeDialog.onConfirm()
                    },
                )
            }
            is MainActiveDialog.Error -> {
                ErrorDialog(
                    title = activeDialog.title,
                    message = activeDialog.message,
                    details = activeDialog.details,
                    neutralButtonText = activeDialog.neutralButtonText,
                    onNeutralButtonClick = activeDialog.onNeutralButtonClick,
                    onDismissRequest = onDismissDialog,
                )
            }
            null -> {}
        }

        val activeProgress =
            listOfNotNull(mainProgress, fragmentProgress, migrationProgress).firstOrNull {
                it.inProgress
            }

        if (activeProgress != null) {
            val msg =
                when {
                    activeProgress.indeterminate -> stringResource(R.string.calculating)
                    else ->
                        stringResource(
                            R.string.count,
                            activeProgress.current,
                            activeProgress.total,
                        ) + (activeProgress.countSuffix?.let { " $it" } ?: "")
                }
            val progressValue =
                if (activeProgress.indeterminate || activeProgress.total == 0) null
                else activeProgress.current.toFloat() / activeProgress.total.toFloat()

            ProgressDialog(
                title = stringResource(activeProgress.titleId),
                message = msg,
                progress = progressValue,
            )
        }
    }

    companion object {
        const val EXTRA_FRAGMENT_TO_OPEN = "notallyx.intent.extra.FRAGMENT_TO_OPEN"
        const val EXTRA_SKIP_START_VIEW_ON_BACK = "notallyx.intent.extra.SKIP_START_VIEW_ON_BACK"
        private const val ACTION_SEARCH = 1001
        val ACTIVITES_WITHOUT_SEARCH = setOf(R.id.Settings, R.id.Reminders, R.id.Labels)
    }
}
