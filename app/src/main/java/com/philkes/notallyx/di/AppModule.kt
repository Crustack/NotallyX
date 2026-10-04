package com.philkes.notallyx.di

import android.content.ContextWrapper
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.presentation.viewmodel.edit.EditActivityViewModel
import com.philkes.notallyx.presentation.viewmodel.edit.NoteModel
import com.philkes.notallyx.presentation.viewmodel.main.MainActivityViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.ArchivedViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.DeletedViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.DisplayLabelViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.LabelsViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.NotesFragmentViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.NotesViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.RemindersViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.SettingsViewModel
import com.philkes.notallyx.presentation.viewmodel.main.fragment.UnlabeledViewModel
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val storageModule = module {
    single { NotallyXPreferences.getInstance(androidContext() as ContextWrapper) }
    single { NotallyDatabase.getDatabase(androidContext() as ContextWrapper) }
}

val viewModelModule = module {
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
}

val appModules = listOf(storageModule, viewModelModule)
