package com.philkes.notallyx.desktop.di

import com.philkes.notallyx.core.database.DatabaseDriverFactory
import com.philkes.notallyx.core.database.DesktopDatabaseDriverFactory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AppInfo
import com.philkes.notallyx.core.platform.DesktopFileStorage
import com.philkes.notallyx.core.platform.DesktopLogger
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.FileStorage
import com.philkes.notallyx.core.platform.Logger
import com.philkes.notallyx.core.platform.NoOpTaskScheduler
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.platform.TaskScheduler
import com.philkes.notallyx.core.ui.platform.DesktopFilePicker
import com.philkes.notallyx.core.ui.platform.DesktopPermissionChecker
import com.philkes.notallyx.core.ui.platform.DesktopShareHandler
import com.philkes.notallyx.core.ui.platform.FilePicker
import com.philkes.notallyx.core.ui.platform.PermissionChecker
import com.philkes.notallyx.core.ui.platform.ShareHandler
import com.philkes.notallyx.desktop.DesktopAppInfo
import com.philkes.notallyx.desktop.DesktopPreferencesStore
import com.philkes.notallyx.feature.media.audio.AudioPlayer
import com.philkes.notallyx.feature.media.audio.AudioRecorder
import com.philkes.notallyx.feature.media.audio.NoOpAudioPlayer
import com.philkes.notallyx.feature.media.audio.NoOpAudioRecorder
import com.philkes.notallyx.feature.migration.backup.NoOpPdfExporter
import com.philkes.notallyx.feature.migration.backup.PdfExporter
import com.philkes.notallyx.feature.reminder.NoOpReminderScheduler
import com.philkes.notallyx.feature.reminder.ReminderScheduler
import kotlinx.coroutines.flow.StateFlow
import org.koin.dsl.module

val desktopPlatformModule = module {
    single<AppInfo> { DesktopAppInfo() }
    single<FileStorage> { DesktopFileStorage() }
    single<PathProvider> { DesktopPathProvider() }
    single<Logger> { DesktopLogger() }
    single<FilePicker> { DesktopFilePicker() }
    single<ShareHandler> { DesktopShareHandler() }
    single<PermissionChecker> { DesktopPermissionChecker() }
    single<TaskScheduler> { NoOpTaskScheduler() }
    single<ReminderScheduler> { NoOpReminderScheduler() }
    single<AudioPlayer> { NoOpAudioPlayer() }
    single<AudioRecorder> { NoOpAudioRecorder() }
    single<PdfExporter> { NoOpPdfExporter() }
    single<DesktopPreferencesStore> { DesktopPreferencesStore(get()) }
}

val desktopDatabaseModule = module {
    single<DatabaseDriverFactory> { DesktopDatabaseDriverFactory(get()) }
    single<NotallyDatabase> {
        val driverFactory: DatabaseDriverFactory = get()
        NotallyDatabase.createDatabase(driverFactory).also { db ->
            NotallyDatabase.postInstance(db)
        }
    }
    single<StateFlow<NotallyDatabase?>> {
        get<NotallyDatabase>()
        NotallyDatabase.instance
    }
}

val desktopAppModules = listOf(desktopPlatformModule, desktopDatabaseModule)
