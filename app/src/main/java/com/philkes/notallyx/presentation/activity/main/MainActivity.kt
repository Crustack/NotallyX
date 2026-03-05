package com.philkes.notallyx.presentation.activity.main

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.Menu.CATEGORY_CONTAINER
import android.view.Menu.CATEGORY_SYSTEM
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.databinding.ActivityMainBinding
import com.philkes.notallyx.databinding.ChoiceItemBinding
import com.philkes.notallyx.databinding.DialogNotesSortBinding
import com.philkes.notallyx.presentation.activity.LockedActivity
import com.philkes.notallyx.presentation.activity.main.fragment.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.presentation.activity.main.fragment.NotallyFragment
import com.philkes.notallyx.presentation.activity.main.fragment.SearchFragment
import com.philkes.notallyx.presentation.activity.note.EditListActivity
import com.philkes.notallyx.presentation.activity.note.EditNoteActivity
import com.philkes.notallyx.presentation.checkedTag
import com.philkes.notallyx.presentation.dp
import com.philkes.notallyx.presentation.getQuantityString
import com.philkes.notallyx.presentation.movedToResId
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.setupProgressDialog
import com.philkes.notallyx.presentation.view.misc.tristatecheckbox.TriStateCheckBox
import com.philkes.notallyx.presentation.view.misc.tristatecheckbox.setMultiChoiceTriStateItems
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel.Companion.CURRENT_LABEL_NONE
import com.philkes.notallyx.presentation.viewmodel.ExportMimeType
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSort
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection
import com.philkes.notallyx.presentation.viewmodel.preference.isManualSort
import com.philkes.notallyx.presentation.viewmodel.progress.MigrationProgress
import com.philkes.notallyx.utils.LATEST_DATA_SCHEMA
import com.philkes.notallyx.utils.backup.exportNotes
import com.philkes.notallyx.utils.runMigrations
import kotlinx.coroutines.launch

class MainActivity : LockedActivity<ActivityMainBinding>() {

    private lateinit var navController: NavController
    private lateinit var configuration: AppBarConfiguration
    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportNotesActivityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var actionModeBinding: ActionModeBinding

    private var isStartViewFragment = false
    private val actionModeCancelCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                actionModeBinding.close()
            }
        }

    var getCurrentFragmentNotes: (() -> Collection<BaseNote>?)? = null

    override fun onSupportNavigateUp(): Boolean {
        baseModel.keyword = ""
        return navController.navigateUp(configuration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.Toolbar)
        configureEdgeToEdgeInsets()

        setupFAB()
        setupMenu()
        setupActionMode()
        setupNavigation()

        setupActivityResultLaunchers()

        preferences.alwaysShowSearchBar.observe(this) { invalidateOptionsMenu() }

        checkForMigrations(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (actionModeBinding.enabled()) {
                        return
                    }
                    if (
                        !isStartViewFragment &&
                            !intent.getBooleanExtra(EXTRA_SKIP_START_VIEW_ON_BACK, false)
                    ) {
                        actionModeBinding.close()
                        navigateToStartView()
                    } else {
                        finish()
                    }
                }
            },
        )
        onBackPressedDispatcher.addCallback(this, actionModeCancelCallback)

        baseModel.progress.setupProgressDialog(this)
    }

    override fun initViewModel() {}

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop:")
        if (baseModel.keyword.isNotEmpty()) {
            baseModel.keyword = ""
        }
        if (actionModeBinding.enabled()) {
            actionModeBinding.close()
        }
    }

    private fun checkForMigrations(savedInstanceState: Bundle?) {
        // Run migrations first (blocking dialog), then proceed with initial navigation
        val proceed: () -> Unit = {
            baseModel.startObserving()
            val fragmentIdToLoad = intent.getIntExtra(EXTRA_FRAGMENT_TO_OPEN, -1)
            if (fragmentIdToLoad != -1) {
                navController.navigate(fragmentIdToLoad, intent.extras)
            } else if (savedInstanceState == null) {
                navigateToStartView()
            }
        }
        if (preferences.dataSchemaId.value < LATEST_DATA_SCHEMA) {
            val migrationProgress = MutableLiveData<MigrationProgress>()
            migrationProgress.setupProgressDialog(this)
            lifecycleScope.launch {
                // Initial title
                migrationProgress.postValue(
                    MigrationProgress(R.string.migrating_data, indeterminate = true)
                )
                application.runMigrations { titleId ->
                    migrationProgress.postValue(MigrationProgress(titleId, indeterminate = true))
                }
                // Dismiss
                migrationProgress.postValue(
                    MigrationProgress(R.string.migrating_data, inProgress = false)
                )
                proceed()
            }
        } else {
            proceed()
        }
    }

    private fun configureEdgeToEdgeInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val navHostFragment = binding.NavHostFragment
        ViewCompat.setOnApplyWindowInsetsListener(binding.RelativeLayout) { _, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Apply padding to the main content views
            // Top margin for the Toolbar to avoid being under the status bar
            binding.Toolbar.apply {
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
                requestLayout()
            }

            binding.ActionMode.apply {
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
                requestLayout()
            }

            // Apply padding to the navigationview top header
            binding.NavigationView.getHeaderView(0).apply {
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
                requestLayout()
            }

            // TakeNote FAB is at the very bottom
            binding.TakeNote.apply {
                val marginLayoutParams = layoutParams as ViewGroup.MarginLayoutParams
                marginLayoutParams.bottomMargin = 16.dp + systemBarsInsets.bottom + imeInsets.bottom
                marginLayoutParams.marginEnd = 16.dp
                requestLayout()
            }

            // The ActionMode toolbar's position will naturally be below the Toolbar,
            // so its top offset is handled by the Toolbar's adjustment.

            // The main content (NavHostFragment) needs bottom padding to avoid
            // being obscured by the system navigation bar and the keyboard.
            // If NavHostFragment contains a ScrollView/RecyclerView, you might apply
            // this padding to that scrollable view instead for better behavior.
            navHostFragment.apply {
                setPadding(
                    paddingLeft,
                    paddingTop,
                    paddingRight,
                    systemBarsInsets.bottom + imeInsets.bottom,
                )
            }
            insets
        }
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
        navController.navigate(id, bundle)
    }

    private fun setupFAB() {
        binding.TakeNote.setOnClickListener {
            if (actionModeBinding.enabled()) {
                moveNotes(Folder.DELETED)
            } else {
                actionModeBinding.close()
                val intent = Intent(this, EditNoteActivity::class.java)
                startActivity(prepareNewNoteIntent(intent))
            }
        }
        binding.MakeList.setOnClickListener {
            if (actionModeBinding.enabled()) {
                label()
            } else {
                actionModeBinding.close()
                val intent = Intent(this, EditListActivity::class.java)
                startActivity(prepareNewNoteIntent(intent))
            }
        }
    }

    private fun updateFABs() {
        if (actionModeBinding.enabled()) {
            binding.TakeNote.apply {
                setImageResource(R.drawable.delete)
                backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.md_theme_error)
                    )
                contentDescription = getString(R.string.delete)
                show()
            }

            binding.MakeList.apply {
                setImageResource(R.drawable.label)
                contentDescription = getString(R.string.labels)
                show()
            }
        } else {
            binding.TakeNote.apply {
                setImageResource(R.drawable.edit)
                backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.md_theme_primary)
                    )
                contentDescription = getString(R.string.take_note)
            }

            binding.MakeList.apply {
                setImageResource(R.drawable.checkbox)
                backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(this@MainActivity, R.color.md_theme_primary)
                    )
                contentDescription = getString(R.string.make_list)
            }

            if (navController.currentDestination?.id in FRAGMENTS_WITHOUT_NOTES) {
                binding.TakeNote.hide()
                binding.MakeList.hide()
            } else {
                binding.TakeNote.show()
                binding.MakeList.show()
            }
        }
    }

    private fun prepareNewNoteIntent(intent: Intent): Intent {
        return currentFragment()?.prepareNewNoteIntent(intent) ?: intent
    }

    private var labelsMenuItems: List<MenuItem> = listOf()
    private var labelsMoreMenuItem: MenuItem? = null
    private var labels: List<String> = listOf()
    private var labelsLiveData: LiveData<List<String>>? = null

    private fun setupMenu() {
        binding.NavigationView.menu.apply {
            add(0, R.id.Notes, 0, R.string.notes).setCheckable(true).setIcon(R.drawable.home)

            addStaticLabelsMenuItems()
            NotallyDatabase.getDatabase(application).observe(this@MainActivity) { database ->
                labelsLiveData?.removeObservers(this@MainActivity)
                labelsLiveData =
                    database.getLabelDao().getAll().also {
                        it.observe(this@MainActivity) { labels ->
                            this@MainActivity.labels = labels
                            setupLabelsMenuItems(labels, preferences.maxLabels.value)
                        }
                    }
            }

            add(2, R.id.Deleted, CATEGORY_SYSTEM + 1, R.string.deleted)
                .setCheckable(true)
                .setIcon(R.drawable.delete)
            add(2, R.id.Archived, CATEGORY_SYSTEM + 2, R.string.archived)
                .setCheckable(true)
                .setIcon(R.drawable.archive)
            add(3, R.id.Reminders, CATEGORY_SYSTEM + 3, R.string.reminders)
                .setCheckable(true)
                .setIcon(R.drawable.notifications)
            add(3, R.id.Settings, CATEGORY_SYSTEM + 4, R.string.settings)
                .setCheckable(true)
                .setIcon(R.drawable.settings)
        }
        baseModel.preferences.labelsHidden.observe(this) { hiddenLabels ->
            hideLabelsInNavigation(hiddenLabels, baseModel.preferences.maxLabels.value)
        }
        baseModel.preferences.maxLabels.observe(this) { maxLabels ->
            binding.NavigationView.menu.setupLabelsMenuItems(labels, maxLabels)
        }
    }

    private fun Menu.addStaticLabelsMenuItems() {
        add(1, R.id.Unlabeled, CATEGORY_CONTAINER + 1, R.string.unlabeled)
            .setCheckable(true)
            .setChecked(baseModel.currentLabel == CURRENT_LABEL_NONE)
            .setIcon(R.drawable.label_off)
        add(1, R.id.Labels, CATEGORY_CONTAINER + 2, R.string.labels)
            .setCheckable(true)
            .setIcon(R.drawable.label_more)
    }

    private fun Menu.setupLabelsMenuItems(labels: List<String>, maxLabelsToDisplay: Int) {
        removeGroup(1)
        addStaticLabelsMenuItems()
        labelsMenuItems =
            labels
                .mapIndexed { index, label ->
                    add(1, R.id.DisplayLabel, CATEGORY_CONTAINER + index + 3, label)
                        .setCheckable(true)
                        .setChecked(baseModel.currentLabel == label)
                        .setVisible(index < maxLabelsToDisplay)
                        .setIcon(R.drawable.label)
                        .setOnMenuItemClickListener {
                            navigateToLabel(label)
                            false
                        }
                }
                .toList()

        labelsMoreMenuItem =
            if (labelsMenuItems.size > maxLabelsToDisplay) {
                add(
                        1,
                        R.id.Labels,
                        CATEGORY_CONTAINER + labelsMenuItems.size + 2,
                        getString(R.string.more, labelsMenuItems.size - maxLabelsToDisplay),
                    )
                    .setCheckable(true)
                    .setIcon(R.drawable.label)
            } else null
        configuration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, configuration)
        hideLabelsInNavigation(baseModel.preferences.labelsHidden.value, maxLabelsToDisplay)
    }

    private fun navigateToLabel(label: String) {
        actionModeBinding.close()
        val bundle = Bundle().apply { putString(EXTRA_DISPLAYED_LABEL, label) }
        navController.navigate(R.id.DisplayLabel, bundle)
    }

    private fun hideLabelsInNavigation(hiddenLabels: Set<String>, maxLabelsToDisplay: Int) {
        var visibleLabels = 0
        labelsMenuItems.forEach { menuItem ->
            val visible =
                !hiddenLabels.contains(menuItem.title) && visibleLabels < maxLabelsToDisplay
            menuItem.isVisible = visible
            if (visible) {
                visibleLabels++
            }
        }
        labelsMoreMenuItem?.title = getString(R.string.more, labels.size - visibleLabels)
    }

    private fun setupActionMode() {
        actionModeBinding =
            ActionModeBinding(
                baseModel,
                binding,
                this,
                preferences.notesSorting,
                ::currentFragment,
                ::exportSelectedNotes,
            )
        preferences.notesSorting.observe(this) { updateFABs() }
        actionModeBinding.enabled.observe(this) { _ -> updateFABs() }
    }

    private fun exportSelectedNotes(mimeType: ExportMimeType) {
        exportNotes(
            baseModel.actionMode.selectedNotes.values,
            mimeType,
            exportFileActivityResultLauncher,
            exportNotesActivityResultLauncher,
        )
    }

    private fun label() {
        val baseNotes = baseModel.actionMode.selectedNotes.values
        lifecycleScope.launch {
            val labels = baseModel.getAllLabels()
            if (labels.isNotEmpty()) {
                displaySelectLabelsDialog(labels, baseNotes)
            } else {
                baseModel.actionMode.close(true)
                navigateWithAnimation(R.id.Labels)
            }
        }
    }

    private fun displaySelectLabelsDialog(labels: Array<String>, baseNotes: Collection<BaseNote>) {
        val checkedPositions =
            labels
                .map { label ->
                    if (baseNotes.all { it.labels.contains(label) }) {
                        TriStateCheckBox.State.CHECKED
                    } else if (baseNotes.any { it.labels.contains(label) }) {
                        TriStateCheckBox.State.PARTIALLY_CHECKED
                    } else {
                        TriStateCheckBox.State.UNCHECKED
                    }
                }
                .toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.labels)
            .setCancelButton()
            .setMultiChoiceTriStateItems(this, labels, checkedPositions) { idx, state ->
                checkedPositions[idx] = state
            }
            .setPositiveButton(R.string.save) { _, _ ->
                val checkedLabels =
                    checkedPositions.mapIndexedNotNull { index, checked ->
                        if (checked == TriStateCheckBox.State.CHECKED) {
                            labels[index]
                        } else null
                    }
                val uncheckedLabels =
                    checkedPositions.mapIndexedNotNull { index, checked ->
                        if (checked == TriStateCheckBox.State.UNCHECKED) {
                            labels[index]
                        } else null
                    }
                val updatedBaseNotesLabels =
                    baseNotes.map { baseNote ->
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
                    baseModel.updateBaseNoteLabels(updatedLabels, baseNote.id)
                }
            }
            .show()
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.NavHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        configuration = AppBarConfiguration(binding.NavigationView.menu, binding.DrawerLayout)
        setupActionBarWithNavController(navController, configuration)

        var fragmentIdToLoad: Int? = null
        binding.NavigationView.setNavigationItemSelectedListener { item ->
            baseModel.actionMode.close(true)
            fragmentIdToLoad = item.itemId
            binding.DrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        binding.DrawerLayout.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {

                override fun onDrawerClosed(drawerView: View) {
                    if (
                        fragmentIdToLoad != null &&
                            navController.currentDestination?.id != fragmentIdToLoad
                    ) {
                        navigateWithAnimation(
                            requireNotNull(fragmentIdToLoad, { "fragmentIdToLoad is null" })
                        )
                    }
                }
            }
        )

        navController.addOnDestinationChangedListener { _, destination, bundle ->
            fragmentIdToLoad = destination.id
            when (fragmentIdToLoad) {
                R.id.DisplayLabel ->
                    bundle?.getString(EXTRA_DISPLAYED_LABEL)?.let {
                        baseModel.currentLabel = it
                        binding.NavigationView.menu.children
                            .find { menuItem -> menuItem.title == it }
                            ?.let { menuItem -> menuItem.isChecked = true }
                    }
                R.id.Unlabeled -> {
                    baseModel.currentLabel = CURRENT_LABEL_NONE
                    binding.NavigationView.setCheckedItem(destination.id)
                }
                else -> {
                    baseModel.currentLabel = CURRENT_LABEL_EMPTY
                    binding.NavigationView.setCheckedItem(destination.id)
                }
            }
            isStartViewFragment = isStartViewFragment(destination.id, bundle)
            updateFABs()
        }
    }

    private fun isStartViewFragment(id: Int, bundle: Bundle?): Boolean {
        val (startViewId, startViewBundle) = getStartViewNavigation()
        return startViewId == id &&
            startViewBundle.getString(EXTRA_DISPLAYED_LABEL) ==
                bundle?.getString(EXTRA_DISPLAYED_LABEL)
    }

    private fun navigateWithAnimation(id: Int) {
        baseModel.actionMode.close(true)
        val options = navOptions {
            launchSingleTop = true
            anim {
                exit = androidx.navigation.ui.R.anim.nav_default_exit_anim
                enter = androidx.navigation.ui.R.anim.nav_default_enter_anim
                popExit = androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
                popEnter = androidx.navigation.ui.R.anim.nav_default_pop_enter_anim
            }
            popUpTo(navController.graph.startDestination) { inclusive = false }
        }
        navController.navigate(id, null, options)
    }

    private fun setupActivityResultLaunchers() {
        exportFileActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        baseModel.exportSelectedNoteToFile(uri, binding.root)
                    }
                }
            }
        exportNotesActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        baseModel.exportSelectedNotesToFolder(uri, binding.root)
                    }
                }
            }
    }

    private fun currentFragment(): NotallyFragment? =
        supportFragmentManager
            .findFragmentById(R.id.NavHostFragment)
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull() as? NotallyFragment

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only show search icon if preference is not enabled and not in Reminders or Settings
        // fragments
        val currentDestinationId = navController.currentDestination?.id

        if (
            !preferences.alwaysShowSearchBar.value &&
                !FRAGMENTS_WITHOUT_NOTES.contains(currentDestinationId)
        ) {
            // If in Search fragment, show X icon instead of search icon
            val isInSearchFragment = currentDestinationId == R.id.Search
            val iconRes = if (isInSearchFragment) R.drawable.close else R.drawable.search
            val titleRes = if (isInSearchFragment) R.string.cancel else R.string.search

            menu
                .add(Menu.NONE, ACTION_SEARCH, Menu.NONE, titleRes)
                .setIcon(iconRes)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        if (!FRAGMENTS_WITHOUT_NOTES.contains(currentDestinationId)) {
            menu
                .add(Menu.NONE, ACTION_SORT, Menu.NONE, R.string.notes_sorted_by)
                .setIcon(R.drawable.sort)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ACTION_SORT -> {
                val value = preferences.notesSorting.value
                val layout = DialogNotesSortBinding.inflate(layoutInflater, null, false)
                NotesSortBy.entries.forEachIndexed { idx, notesSortBy ->
                    ChoiceItemBinding.inflate(layoutInflater).root.apply {
                        id = idx
                        text = getString(notesSortBy.textResId)
                        tag = notesSortBy
                        layout.NotesSortByRadioGroup.addView(this)
                        setCompoundDrawablesRelativeWithIntrinsicBounds(
                            notesSortBy.iconResId,
                            0,
                            0,
                            0,
                        )
                        if (notesSortBy == value.sortedBy) {
                            layout.NotesSortByRadioGroup.check(this.id)
                        }
                    }
                }

                layout.NotesSortDirectionRadioGroup.isVisible = !value.isManualSort
                layout.NotesSortDirectionRadioGroupLabel.isVisible = !value.isManualSort
                layout.NotesSortByRadioGroup.setOnCheckedChangeListener { group, _ ->
                    val selectedSortByIsManual =
                        (group.checkedTag() as? NotesSortBy)?.isManualSort ?: false
                    layout.NotesSortDirectionRadioGroup.isVisible = !selectedSortByIsManual
                    layout.NotesSortDirectionRadioGroupLabel.isVisible = !selectedSortByIsManual
                }

                SortDirection.entries.forEachIndexed { idx, sortDir ->
                    ChoiceItemBinding.inflate(layoutInflater).root.apply {
                        id = idx
                        text = getString(sortDir.textResId)
                        tag = sortDir
                        setCompoundDrawablesRelativeWithIntrinsicBounds(sortDir.iconResId, 0, 0, 0)
                        layout.NotesSortDirectionRadioGroup.addView(this)
                        if (sortDir == value.sortDirection) {
                            layout.NotesSortDirectionRadioGroup.check(this.id)
                        }
                    }
                }

                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.notes_sorted_by)
                    .setView(layout.root)
                    .setPositiveButton(R.string.save) { dialog, _ ->
                        dialog.cancel()
                        val newSortBy = layout.NotesSortByRadioGroup.checkedTag() as NotesSortBy
                        val newSortDirection =
                            if (newSortBy.isManualSort) {
                                SortDirection.DESC
                            } else {
                                layout.NotesSortDirectionRadioGroup.checkedTag() as SortDirection
                            }

                        baseModel.savePreference(
                            preferences.notesSorting,
                            NotesSort(newSortBy, newSortDirection),
                        )
                        //                        if (newSortBy.isManualSort && !value.isManualSort)
                        // {
                        //                            initializeManualSortIdx()
                        //                        }
                        invalidateOptionsMenu()
                    }
                    .setCancelButton()
                    .show()
                true
            }
            ACTION_SEARCH -> {
                val isInSearchFragment = navController.currentDestination?.id == R.id.Search

                if (isInSearchFragment) {
                    // If in Search fragment, navigate back to cancel search
                    baseModel.keyword = ""
                    navController.popBackStack()
                } else {
                    // Navigate to search fragment
                    if (currentFragment() is NotallyFragment) {
                        baseModel.actionMode.close(true)
                        navController.navigate(
                            R.id.Search,
                            Bundle().apply {
                                putSerializable(
                                    SearchFragment.EXTRA_INITIAL_FOLDER,
                                    baseModel.folder.value,
                                )
                                putSerializable(
                                    SearchFragment.EXTRA_INITIAL_LABEL,
                                    baseModel.currentLabel,
                                )
                            },
                        )
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_FRAGMENT_TO_OPEN = "notallyx.intent.extra.FRAGMENT_TO_OPEN"
        const val EXTRA_SKIP_START_VIEW_ON_BACK = "notallyx.intent.extra.SKIP_START_VIEW_ON_BACK"
        private const val ACTION_SEARCH = 1001
        private const val ACTION_SORT = 1002
        val FRAGMENTS_WITHOUT_NOTES = setOf(R.id.Settings, R.id.Reminders, R.id.Labels)
    }
}

fun MainActivity.moveNotes(folderTo: Folder) {
    if (actionModeBinding.loading || actionModeBinding.mode.isEmpty()) {
        return
    }
    try {
        actionModeBinding.loading = true
        val folderFrom = actionModeBinding.mode.getFirstNote().folder
        val ids = baseModel.moveBaseNotes(folderTo)
        Snackbar.make(
                findViewById(R.id.DrawerLayout),
                getQuantityString(folderTo.movedToResId(), ids.size),
                Snackbar.LENGTH_SHORT,
            )
            .apply { setAction(R.string.undo) { baseModel.moveBaseNotes(ids, folderFrom) } }
            .show()
    } finally {
        actionModeBinding.loading = false
    }
}
