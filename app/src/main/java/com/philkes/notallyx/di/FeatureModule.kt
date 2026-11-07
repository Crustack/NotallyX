package com.philkes.notallyx.di

import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.feature.editor.EditActivityViewModel
import com.philkes.notallyx.feature.editor.EditListViewModel
import com.philkes.notallyx.feature.editor.EditNoteViewModel
import com.philkes.notallyx.feature.editor.EditTextPlainViewModel
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.labels.DisplayLabelViewModel
import com.philkes.notallyx.feature.labels.LabelsViewModel
import com.philkes.notallyx.feature.main.MainActivityViewModel
import com.philkes.notallyx.feature.main.routes.archived.ArchivedViewModel
import com.philkes.notallyx.feature.main.routes.deleted.DeletedViewModel
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesViewModel
import com.philkes.notallyx.feature.main.routes.settings.AutoRemoveDeletedNotesWorker
import com.philkes.notallyx.feature.reminder.NoteRemindersViewModel
import com.philkes.notallyx.feature.reminder.RemindersViewModel
import com.philkes.notallyx.feature.settings.SettingsViewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val featureModule = module {
    single { ActionMode() }
    workerOf(::AutoRemoveDeletedNotesWorker)
    viewModelOf(::MainActivityViewModel)
    viewModelOf(::NotesRouteViewModel)
    viewModelOf(::NotesViewModel)
    viewModelOf(::ArchivedViewModel)
    viewModelOf(::DeletedViewModel)
    viewModelOf(::DisplayLabelViewModel)
    viewModelOf(::LabelsViewModel)
    viewModelOf(::RemindersViewModel)
    viewModelOf(::NoteRemindersViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UnlabeledViewModel)
    viewModelOf(::NoteModel)
    viewModelOf(::EditActivityViewModel)
    viewModelOf(::EditNoteViewModel)
    viewModelOf(::EditListViewModel)
    viewModelOf(::EditTextPlainViewModel)
    viewModelOf(::ErrorActivityViewModel)
}
