package com.philkes.notallyx.test

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import io.mockk.every
import io.mockk.mockkStatic
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.koin.core.context.GlobalContext

val context: ContextWrapper
    get() = ApplicationProvider.getApplicationContext<ContextWrapper>()

val database: NotallyDatabase
    get() =
        NotallyDatabase.getFreshDatabase(
            context,
            preferences.dataInPublicFolder.value,
            preferences.biometricLock.value,
        )

val preferences: NotallyXPreferences
    get() =
        GlobalContext.getOrNull()?.get<NotallyXPreferences>()
            ?: NotallyXPreferences.getInstance(ContextWrapper(context))

fun createListItem(
    body: String,
    checked: Boolean = false,
    isChild: Boolean = false,
    order: Int? = null,
    children: MutableList<ListItem> = mutableListOf(),
    id: Int = -1,
): ListItem {
    return ListItem(body, checked, isChild, order, children, id)
}

fun mockAndroidLog() {
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.w(any<String>(), any<String>()) } returns 0
    every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    every { Log.e(any(), any()) } returns 0
}

fun createBaseNote(
    id: Long = 0L,
    type: Type = Type.NOTE,
    folder: Folder = Folder.NOTES,
    color: String = BaseNote.COLOR_DEFAULT,
    title: String = "Note",
    pinned: Boolean = false,
    isPinnedToStatus: Boolean = false,
    timestamp: Long = System.currentTimeMillis(),
    modifiedTimestamp: Long = System.currentTimeMillis(),
    labels: List<String> = listOf(),
    body: String = "",
    spans: List<SpanRepresentation> = listOf(),
    items: List<ListItem> = listOf(),
    images: List<FileAttachment> = listOf(),
    files: List<FileAttachment> = listOf(),
    audios: List<Audio> = listOf(),
    reminders: List<Reminder> = listOf(),
    viewMode: com.philkes.notallyx.data.model.NoteViewMode =
        com.philkes.notallyx.data.model.NoteViewMode.EDIT,
): BaseNote {
    return BaseNote(
        id,
        type,
        folder,
        color,
        title,
        pinned,
        timestamp,
        modifiedTimestamp,
        labels,
        body,
        spans,
        items,
        images,
        files,
        audios,
        reminders,
        viewMode,
        isPinnedToStatus,
    )
}

fun getRandomString(length: Int, seed: Long = 42L): String {
    if (length <= 0) return ""
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    val random = Random(seed)
    return (1..length).map { allowedChars[random.nextInt(allowedChars.size)] }.joinToString("")
}

fun createDummyImageFile(
    context: ContextWrapper,
    fileName: String = "dummy_image.jpg",
    width: Int = 400,
    height: Int = 400,
): File {
    val mediaDir = context.getCurrentImagesDirectory().apply { if (!exists()) mkdirs() }
    val imageFile = File(mediaDir, fileName)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundColor = android.graphics.Color.rgb(100, 150, 200)
    canvas.drawColor(backgroundColor)

    val paint =
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
    canvas.drawText("Dummy Image", 50f, (height / 2).toFloat(), paint)

    FileOutputStream(imageFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }

    bitmap.recycle()
    return imageFile
}

fun String.toMillis(
    pattern: String = "yyyy-MM-dd HH:mm",
    zoneId: ZoneId = ZoneId.of("UTC"),
): Long {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return LocalDateTime.parse(this, formatter).atZone(zoneId).toInstant().toEpochMilli()
}
