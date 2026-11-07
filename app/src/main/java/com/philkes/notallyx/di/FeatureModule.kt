package com.philkes.notallyx.di

import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.editor.EditActivityViewModel
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.labels.DisplayLabelViewModel
import com.philkes.notallyx.feature.labels.LabelsViewModel
import com.philkes.notallyx.feature.main.MainActivityViewModel
import com.philkes.notallyx.feature.main.tab.ArchivedViewModel
import com.philkes.notallyx.feature.main.tab.AutoRemoveDeletedNotesWorker
import com.philkes.notallyx.feature.main.tab.DeletedViewModel
import com.philkes.notallyx.feature.main.tab.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.tab.NotesViewModel
import com.philkes.notallyx.feature.main.tab.UnlabeledViewModel
import com.philkes.notallyx.feature.reminder.RemindersViewModel
import com.philkes.notallyx.feature.settings.SettingsViewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureModule = module {
    workerOf(::AutoRemoveDeletedNotesWorker)
    viewModelOf(::MainActivityViewModel)
    viewModelOf(::NotesFragmentViewModel)
    viewModelOf(::NotesViewModel)
    viewModelOf(::ArchivedViewModel)
    viewModelOf(::DeletedViewModel)
    viewModelOf(::DisplayLabelViewModel)
    viewModelOf(::LabelsViewModel)
    viewModelOf(::RemindersViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UnlabeledViewModel)
    viewModelOf(::NoteModel)
    viewModelOf(::EditActivityViewModel)
    viewModelOf(::ErrorActivityViewModel)
}
