package com.philkes.notallyx.di

import com.philkes.notallyx.core.common.error.ErrorActivityViewModel
import com.philkes.notallyx.core.platform.AndroidAppInfo
import com.philkes.notallyx.core.platform.AndroidFileStorage
import com.philkes.notallyx.core.platform.AndroidLogger
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.AndroidTaskScheduler
import com.philkes.notallyx.core.platform.AppInfo
import com.philkes.notallyx.core.platform.FileStorage
import com.philkes.notallyx.core.platform.Logger
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.platform.TaskScheduler
import com.philkes.notallyx.core.security.BiometricAuthenticator
import com.philkes.notallyx.core.security.SystemBiometricAuthenticator
import com.philkes.notallyx.core.ui.platform.AndroidFilePicker
import com.philkes.notallyx.core.ui.platform.AndroidPermissionChecker
import com.philkes.notallyx.core.ui.platform.AndroidShareHandler
import com.philkes.notallyx.core.ui.platform.FilePicker
import com.philkes.notallyx.core.ui.platform.PermissionChecker
import com.philkes.notallyx.core.ui.platform.ShareHandler
import com.philkes.notallyx.feature.editor.EditActivityViewModel
import com.philkes.notallyx.feature.main.MainActivityViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.settings.AutoRemoveDeletedNotesWorker
import com.philkes.notallyx.feature.media.audio.AndroidServiceAudioPlayer
import com.philkes.notallyx.feature.media.audio.AndroidServiceAudioRecorder
import com.philkes.notallyx.feature.media.audio.AudioPlayer
import com.philkes.notallyx.feature.media.audio.AudioRecorder
import com.philkes.notallyx.feature.migration.backup.AndroidPdfExporter
import com.philkes.notallyx.feature.migration.backup.PdfExporter
import com.philkes.notallyx.feature.reminder.AndroidReminderScheduler
import com.philkes.notallyx.feature.reminder.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val platformModule = module {
    single<AppInfo> { AndroidAppInfo() }
    single<FileStorage> { AndroidFileStorage(androidContext()) }
    single<PathProvider> { AndroidPathProvider(androidContext()) }
    single<FilePicker> { AndroidFilePicker() }
    single<PdfExporter> { AndroidPdfExporter(androidContext()) }
    single<AudioPlayer> { AndroidServiceAudioPlayer(androidContext()) }
    single<AudioRecorder> { AndroidServiceAudioRecorder(androidContext()) }
    single<ShareHandler> { AndroidShareHandler(androidContext()) }
    single<PermissionChecker> { AndroidPermissionChecker(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<TaskScheduler> { AndroidTaskScheduler(androidContext()) }
    single<Logger> { AndroidLogger() }
    single<BiometricAuthenticator> { SystemBiometricAuthenticator() }
}

val androidHostModule = module {
    workerOf(::AutoRemoveDeletedNotesWorker)
    viewModelOf(::MainActivityViewModel)
    viewModelOf(::EditActivityViewModel)
    viewModelOf(::ErrorActivityViewModel)
    viewModelOf(::NotesRouteViewModel)
}

val appModules =
    listOf(
        platformModule,
        preferencesModule,
        databaseModule,
        composeFeatureModule,
        androidHostModule,
    )
