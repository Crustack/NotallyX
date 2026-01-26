package com.philkes.notallyx.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.remote.NotallyApi
import com.philkes.notallyx.data.remote.RegisterDeviceRequest
import com.philkes.notallyx.data.remote.SyncListItem
import com.philkes.notallyx.data.remote.SyncNote
import com.philkes.notallyx.data.remote.SyncRequest
import com.philkes.notallyx.data.remote.SyncSpan
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import androidx.work.OneTimeWorkRequest
import androidx.work.Data
import androidx.work.BackoffPolicy

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "SyncWorker"
        const val KEY_BACKEND_URL = "backend_url"

        fun getSyncWorkRequest(backendUrl: String): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(SyncWorker::class.java)
                .addTag(TAG)
                // We can pass URL via Data if needed, but easier to read from Prefs
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = NotallyXPreferences.getInstance(applicationContext)
        val backendUrl = prefs.backendUrl.value

        if (backendUrl.isEmpty()) {
            return@withContext Result.failure()
        }

        try {
            val database = NotallyDatabase.getDatabase(applicationContext, false).value
                ?: return@withContext Result.failure()

            // Setup API
            val json = Json { 
                ignoreUnknownKeys = true 
                coerceInputValues = true
            }
            val loggingInterceptor = HttpLoggingInterceptor().apply { 
                level = HttpLoggingInterceptor.Level.BODY 
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(if (backendUrl.endsWith("/")) backendUrl else "$backendUrl/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()

            val api = retrofit.create(NotallyApi::class.java)

            // Register device (Simple check: store device ID in prefs)
            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            )
            // Ideally check if registered, for MVP assume server handles duplicate registration gracefully
             try {
                 api.registerDevice(RegisterDeviceRequest(deviceId, android.os.Build.MODEL))
             } catch (e: Exception) {
                 Log.e(TAG, "Registration failed (maybe offline or already registered): ${e.message}")
                 // Continue to sync attempt
             }

            // 1. Pull
            val lastSync = prefs.lastSync.value
            Log.d(TAG, "Pulling changes since $lastSync")
            
            val pullResponse = api.pullChanges(lastSync)
            
            // Apply changes
            pullResponse.notes.forEach { syncNote ->
                val note = syncNote.toBaseNote()
                // Check if exists
                val localNote = database.getBaseNoteDao().get(note.id)
                if (localNote != null) {
                    // Update only if remote is newer? Or just overwrite (last write wins)
                    // Server handles last write wins logic on pull.
                   // Since we use ID match:
                   // Note: Backend IDs are auto-increment. Android IDs are auto-increment.
                   // CRITICAL CONFLICT:
                   // Android generates ID 1. Backend generates ID 1 for another user/device?
                   // If clean start: Android pushes ID 1. Server maps to ID 100. Server returns ID 100.
                   // Android needs to update local ID? Data migration is hard.
                   // SIMPLIFICATION:
                   // Server trust client IDs? No, unreliable.
                   // Client must use GUIDs? BaseNote.id is Long (AutoInc).
                   // FOR MVP:
                   // We rely on simple "push all" and "pull all".
                   // If IDs collide, it's a mess.
                   // Proper way: Client generates UUID. Server maps UUID.
                   // But BaseNote uses Long ID.
                   // Maybe we treat SERVER IDs as source of truth?
                   // When pulling:
                   // If local ID exists, overwrite.
                   // If not, insert.
                   // BUT: If I create Note A (ID 1) locally.
                   // Server has Note B (ID 1).
                   // I pull Note B. My Note A is overwritten? Yes.
                   // This is a known limitation of using AutoInc IDs with multi-master sync without UUIDs.
                   // Since this is a "Clone" project, and I can't easily change database schema to UUID...
                   // I will assume for now:
                   // If user uses ONE device primarily + Web.
                   // Web uses ID.
                   // If I create on Web -> ID 10.
                   // Pull on Android -> Insert with ID 10. (Room allows inserting with set ID).
                   // If Android inserts ID 10 (auto-generated) -> Collision.
                   // Solution: Android AutoInc starts high? Or randomize?
                   // No easy solution without UUID.
                   // I'll proceed with direct mapping. User beware of ID collisions.
                   database.getBaseNoteDao().insert(note) 
                } else {
                    database.getBaseNoteDao().insert(note)
                }
            }
            
            if (pullResponse.timestamp > lastSync) {
                prefs.lastSync.save(pullResponse.timestamp)
            }

            // 2. Push
            // Find modified notes. For MVP, push ALL notes modified > lastSync?
            // Or just push ALL notes. API handles diff?
            // Pushing all is heavy but safe.
            val allNotes = database.getBaseNoteDao().getAll() // This is blocking? getAll() is defined as non-suspend in DAO?
            // BaseNoteDao: getAll(): List<BaseNote> -> Yes.
            
            // Filter notes updated since lastSync
            val timestampToPush = lastSync // or slightly older to be safe
            val notesToPush = allNotes.filter { 
                 // If modifiedTimestamp is missing (old DB), use timestamp
                 // BaseNote has: timestamp (created), modifiedTimestamp.
                 // We push if modifiedTimestamp > lastSync OR (timestamp > lastSync).
                 it.modifiedTimestamp > timestampToPush || it.timestamp > timestampToPush
            }

            if (notesToPush.isNotEmpty()) {
                 Log.d(TAG, "Pushing ${notesToPush.size} notes")
                 val syncNotes = notesToPush.map { it.toSyncNote() }
                 val pushResponse = api.pushChanges(SyncRequest(deviceId, syncNotes, emptyList()))
                 
                 // Update lastSync again?
                 // Be careful not to create loop if server timestamp changes.
                 if (pushResponse.timestamp > prefs.lastSync.value) {
                     prefs.lastSync.save(pushResponse.timestamp)
                 }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    // Mapping Functions
    private fun SyncNote.toBaseNote(): BaseNote {
        return BaseNote(
            id = id,
            type = try { Type.valueOf(type) } catch (e: Exception) { Type.TEXT },
            folder = try { Folder.valueOf(folder) } catch (e: Exception) { Folder.NOTES },
            color = color,
            title = title,
            pinned = pinned,
            timestamp = timestamp,
            modifiedTimestamp = modifiedTimestamp,
            labels = labels,
            body = body,
            spans = spans.map { it.toSpan() },
            items = items.map { it.toItem() },
            images = emptyList(), // Not synced yet
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(), // Sycning reminders logic if needed
            viewMode = try { NoteViewMode.valueOf(viewMode) } catch (e: Exception) { NoteViewMode.EDIT }
        )
    }

    private fun BaseNote.toSyncNote(): SyncNote {
        return SyncNote(
            id = id,
            type = type.name,
            folder = folder.name,
            color = color,
            title = title,
            pinned = pinned,
            timestamp = timestamp,
            modifiedTimestamp = modifiedTimestamp,
            labels = labels,
            body = body,
            spans = spans.map { it.toSyncSpan() },
            items = items.map { it.toSyncItem() },
            viewMode = viewMode.name
        )
    }

    private fun SyncSpan.toSpan(): SpanRepresentation {
        return SpanRepresentation(start, end, bold, link, linkData, italic, monospace, strikethrough)
    }

    private fun SpanRepresentation.toSyncSpan(): SyncSpan {
        return SyncSpan(start, end, bold, link, linkData, italic, monospace, strikethrough)
    }

    private fun SyncListItem.toItem(): ListItem {
        return ListItem(
            body = body,
            checked = checked,
            isChild = isChild,
            order = order,
            children = children.map { it.toItem() }.toMutableList(),
            id = id
        )
    }

    private fun ListItem.toSyncItem(): SyncListItem {
        return SyncListItem(
            body = body,
            checked = checked,
            isChild = isChild,
            order = order,
            children = children.map { it.toSyncItem() },
            id = id
        )
    }
}
