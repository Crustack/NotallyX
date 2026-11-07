# Platform Boundary Contracts: Multiplatform Migration Prerequisites

**Feature**: `002-kmp-migration-prep`  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Overview

This document defines the interface contracts that decouple **NotallyX**'s shared Domain/Data layer (`:shared` target packages) and shared Presentation layer (`:composeApp` target packages) from Android-only framework APIs.

---

## 2. Domain & Data Layer Contracts (`:shared/commonMain` target)

### 2.1 `PathProvider` (`PREP-006`)
Resolves application directories across Android (including `dataInPublicFolder` mode) and Desktop OS conventions (`$XDG_DATA_HOME/NotallyX`, `~/Library/Application Support/NotallyX`, `%APPDATA%\NotallyX`).

```kotlin
package com.philkes.notallyx.core.platform

import java.io.File

interface PathProvider {
    fun getDatabaseFile(databaseName: String, dataInPublicFolder: Boolean = false): File
    fun getDatabaseFiles(databaseName: String, dataInPublicFolder: Boolean = false): List<File>
    fun getMediaDirectory(subFolder: String? = null): File
    fun getBackupsDirectory(): File
    fun getCacheDirectory(subFolder: String? = null): File
}
```

### 2.2 `DatabaseDriverFactory` (`PREP-001`, `PREP-010`)
Abstracts Room 3 `SQLiteDriver` and builder creation so `NotallyDatabase` requires no `ContextWrapper`.

```kotlin
package com.philkes.notallyx.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver

interface DatabaseDriverFactory {
    fun createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean = false,
    ): RoomDatabase.Builder<NotallyDatabase>

    fun createDriver(
        databaseName: String,
        dataInPublicFolder: Boolean = false,
    ): SQLiteDriver
}
```

### 2.3 `FileStorage` (`PREP-002`, `PREP-004`)
Provides stream and byte I/O over `PlatformUri` without leaking `ContentResolver` or `DocumentFile`.

```kotlin
package com.philkes.notallyx.core.platform

import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface FileStorage {
    suspend fun openInputStream(uri: PlatformUri): InputStream?
    suspend fun openOutputStream(uri: PlatformUri, append: Boolean = false): OutputStream?
    suspend fun getFileName(uri: PlatformUri): String?
    suspend fun getMimeType(uri: PlatformUri): String?
    suspend fun exists(uri: PlatformUri): Boolean
    suspend fun delete(uri: PlatformUri): Boolean
    suspend fun listChildren(directoryUri: PlatformUri): List<PlatformUri>
    suspend fun createChildFile(
        directoryUri: PlatformUri,
        mimeType: String,
        displayName: String,
    ): PlatformUri?
    suspend fun copyToInternalFile(source: PlatformUri, destination: File): Boolean
}
```

### 2.4 `PdfExporter` (`PREP-005`)
Replaces `android.webkit.WebView` + `android.print.PdfPrintListener` in shared export flows.

```kotlin
package com.philkes.notallyx.feature.migration.backup

import com.philkes.notallyx.core.platform.PlatformUri

interface PdfExporter {
    suspend fun exportHtmlToPdf(
        htmlContent: String,
        targetUri: PlatformUri,
    ): Result<PlatformUri>
}
```

### 2.5 `SecurePreferencesStore` (`PREP-011`)
Isolates Google Tink AEAD + Android KeyStore (`datastore-tink`) to Android while providing a clean key-value abstraction for `NotallyXPreferences`.

```kotlin
package com.philkes.notallyx.core.preference

import kotlinx.coroutines.flow.StateFlow

interface SecurePreferencesStore {
    val biometricLockPassphraseIv: StateFlow<ByteArray?>
    val fallbackPin: StateFlow<String?>
    suspend fun setBiometricLockPassphraseIv(iv: ByteArray?)
    suspend fun setFallbackPin(pin: String?)
    suspend fun clear()
}
```

### 2.6 `ReminderScheduler` (`PREP-013`)
Encapsulates `AlarmManager`, `NotificationManager`, and `PinnedNotificationManager`.

```kotlin
package com.philkes.notallyx.feature.reminder

import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder

interface ReminderScheduler {
    fun scheduleReminder(noteId: Long, reminder: Reminder)
    fun cancelReminder(noteId: Long, reminderId: Long)
    fun cancelAllReminders(noteId: Long, reminders: List<Reminder>)
    fun updatePinnedNotification(note: BaseNote)
    fun cancelPinnedNotification(noteId: Long)
    fun canScheduleExactAlarms(): Boolean
}
```

### 2.7 `TaskScheduler` (`PREP-015`)
Encapsulates `WorkManager` periodic jobs (`AutoBackupWorker`, `AutoRemoveDeletedNotesWorker`, `CleanupMissingAttachmentsWorker`).

```kotlin
package com.philkes.notallyx.core.platform

interface TaskScheduler {
    fun scheduleAutoBackup(periodInDays: Long)
    fun cancelAutoBackup()
    fun scheduleAutoRemoveDeletedNotes()
    fun cancelAutoRemoveDeletedNotes()
    fun scheduleCleanupMissingAttachments()
}
```

### 2.8 `AudioPlayer` & `AudioRecorder` (`PREP-014`)
Encapsulates Media3 `ExoPlayer` (`AudioPlayService`) and Android `MediaRecorder` (`AudioRecordService`).

```kotlin
package com.philkes.notallyx.feature.media.audio

import java.io.File
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val errorCode: StateFlow<Int?>

    suspend fun loadAudio(file: File)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun release()
}

interface AudioRecorder {
    val isSupported: Boolean
    val isRecording: StateFlow<Boolean>
    val elapsedDurationMs: StateFlow<Long>

    fun startRecording()
    fun stopRecording(): File?
    fun release()
}
```

---

## 3. Presentation Layer Contracts (`:composeApp/commonMain` target)

### 3.1 `FilePicker` (`PREP-004`)
UI-level contract for selecting files/folders via SAF on Android or Swing/AWT on Desktop.

```kotlin
package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri

interface FilePicker {
    suspend fun pickFile(mimeTypes: List<String> = listOf("*/*")): PlatformUri?
    suspend fun pickMultipleFiles(mimeTypes: List<String> = listOf("*/*")): List<PlatformUri>
    suspend fun pickDirectory(): PlatformUri?
    suspend fun createDocument(fileName: String, mimeType: String): PlatformUri?
}
```

### 3.2 `ShareHandler` & `PermissionChecker` (`PREP-022`)
Abstracts `Intent.ACTION_SEND`, `FileProvider`, external URI opening, and `PackageManager` checks.

```kotlin
package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri

interface ShareHandler {
    fun shareText(title: String, body: String)
    fun shareFile(uri: PlatformUri, mimeType: String)
    fun openFileExternally(uri: PlatformUri, mimeType: String): Boolean
    fun openUrl(url: String): Boolean
    fun sendBugReportEmail(recipient: String, subject: String, body: String)
}

enum class AppPermission {
    POST_NOTIFICATIONS,
    SCHEDULE_EXACT_ALARM,
    RECORD_AUDIO,
}

interface PermissionChecker {
    fun isGranted(permission: AppPermission): Boolean
}
```

---

## 4. Navigation 3 Serialization Contract (`PREP-021`)

All `NotallyNavKey` subtypes MUST be registered polymorphically in `NotallyNavSavedStateConfig` so `rememberNavBackStack` works on both Android and Desktop without reflection:

```kotlin
package com.philkes.notallyx.navigation

import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val NotallyNavSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic( androidx.navigation3.runtime.NavKey::class ) {
            subclass(NotallyNavKey.Notes::class)
            subclass(NotallyNavKey.Unlabeled::class)
            subclass(NotallyNavKey.Labels::class)
            subclass(NotallyNavKey.Deleted::class)
            subclass(NotallyNavKey.Archived::class)
            subclass(NotallyNavKey.Reminders::class)
            subclass(NotallyNavKey.Settings::class)
            subclass(NotallyNavKey.DisplayLabel::class)
            subclass(NotallyNavKey.Search::class)
            subclass(NotallyNavKey.EditNote::class)
            subclass(NotallyNavKey.EditList::class)
            subclass(NotallyNavKey.NoteReminders::class)
            subclass(NotallyNavKey.PlayAudio::class)
            subclass(NotallyNavKey.RecordAudio::class)
            subclass(NotallyNavKey.ViewImage::class)
            subclass(NotallyNavKey.PickNote::class)
            subclass(NotallyNavKey.SelectLabels::class)
            subclass(NotallyNavKey.AddLabel::class)
            subclass(NotallyNavKey.EditLabel::class)
        }
    }
}
```
