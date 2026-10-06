package com.philkes.notallyx.test

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.recyclerview.widget.SortedList
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.imports.google.GoogleKeepImporterTest.Companion.createBaseNote
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.view.note.listitem.ListItemDragCallback
import com.philkes.notallyx.presentation.view.note.listitem.toReadableString
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.MIME_TYPE_JSON
import com.philkes.notallyx.utils.changehistory.ListIsChildChange
import com.philkes.notallyx.utils.find
import com.philkes.notallyx.utils.getCurrentImagesDirectory
import io.mockk.every
import io.mockk.mockkStatic
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.koin.core.context.GlobalContext
import org.mockito.Mockito

val context
    get() = ApplicationProvider.getApplicationContext<ContextWrapper>()

val app
    get() = ApplicationProvider.getApplicationContext<NotallyXApplication>()

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

fun ListItemDragCallback.simulateDrag(positionFrom: Int, positionTo: Int, itemCount: Int) {
    this.reset()
    var from = positionFrom
    if (positionFrom < positionTo) {
        for (i in positionFrom until positionTo) {
            val to = i + 1 + itemCount - 1
            val moved = this.move(from, to)
            if (moved) {
                from = i + 1
            }
        }
    } else {
        for (i in positionFrom downTo positionTo + 1) {
            val moved = this.move(from, i - 1)
            if (moved) {
                from = i - 1
            }
        }
    }
    this.onDragEnd()
}

fun SortedList<ListItem>.find(vararg bodies: String): List<ListItem> {
    return bodies.map { body -> this.find { it.body == body }!! }
}

fun SortedList<ListItem>.assertOrder(vararg itemBodies: String) {
    itemBodies.forEachIndexed { position, s ->
        assertEquals("${this.toReadableString()}\nAt position: $position", s, get(position).body)
    }
}

fun List<ListItem>.assertOrder(vararg itemBodies: String) {
    itemBodies.forEachIndexed { position, s ->
        assertEquals("${this.toReadableString()}\nAt position: $position", s, this[position].body)
    }
}

fun List<ListItem>.assertOrderValues(vararg orders: Int) {
    orders.forEachIndexed { position, s ->
        assertEquals("${this.toReadableString()}\nAt position: $position", s, this[position].order)
    }
}

fun <E> SortedList<E>.assertSize(expected: Int) {
    assertEquals("size", expected, this.size())
}

fun <E> List<E>.assertSize(expected: Int) {
    assertEquals("size", expected, this.size)
}

fun SortedList<ListItem>.assertIds(vararg itemIds: Int) {
    itemIds.forEachIndexed { position, s -> assertEquals("id", s, get(position).id) }
}

fun List<ListItem>.assertIds(vararg itemIds: Int) {
    itemIds.forEachIndexed { position, s -> assertEquals("id", s, get(position).id) }
}

fun SortedList<ListItem>.assertChecked(vararg checked: Boolean) {
    checked.forEachIndexed { index, expected ->
        assertEquals("checked at position: $index", expected, get(index).checked)
    }
}

fun List<ListItem>.assertChecked(vararg checked: Boolean) {
    checked.forEachIndexed { index, expected ->
        assertEquals("checked at position: $index", expected, get(index).checked)
    }
}

fun ListItem.assertChildren(vararg childrenBodies: String) {
    assertFalse("isChild", this.isChild)
    if (childrenBodies.isNotEmpty()) {
        childrenBodies.forEachIndexed { index, s ->
            assertEquals("Child at position $index", s, children[index].body)
            assertTrue(children[index].isChild)
            assertTrue(children[index].children.isEmpty())
        }
    } else {
        assertTrue(
            "'${body}' expected empty children\t actual: ${
                children.joinToString(",") { "'${it.body}'" }
            } ",
            children.isEmpty(),
        )
    }
}

// fun ListMoveChange.assert(from: Int, itemsBeforeMove: List<ListItem>) {
//    assertEquals("from", from, position)
//    assertEquals("itemsBeforeMove", itemsBeforeMove, this.itemsBefore)
// }

// fun ListCheckedChange.assert(newValue: Boolean, itemId: Int) {
//    assertEquals("checked", newValue, this.newValue)
//    assertEquals("itemId", itemId, this.itemId)
// }

fun ListIsChildChange.assert(newValue: Boolean, position: Int) {
    //    assertEquals("isChild", newValue, this.newValue)
    //    assertEquals("position", position, this.position)
}

// fun ListAddChange.assert(position: Int, newItem: ListItem) {
//    assertEquals("position", position, this.position)
//    assertEquals("newItem", newItem, this.itemBeforeInsert)
// }
//
// fun ListDeleteChange.assert(order: Int, deletedItem: ListItem?) {
//    assertEquals("order", order, this.itemOrder)
//    assertEquals("deletedItem", deletedItem, this.deletedItem)
// }

// fun ChangeCheckedForAllChange.assert(checked: Boolean, changedPositions: Collection<Int>) {
//    assertEquals("checked", checked, this.checked)
//    assertEquals("changedIds", changedPositions, this.changedIds)
// }

// fun DeleteCheckedChange.assert(itemsBeforeDelete: List<ListItem>) {
//    assertEquals("itemsBeforeDelete", itemsBeforeDelete, this.deletedItems)
// }

object MockitoHelper {
    fun <T> anyObject(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST") fun <T> uninitialized(): T = null as T
}

fun generateNotes(batchIdx: Int, date: Date): List<BaseNote> =
    (0..1).map { idx ->
        val isList = idx % 2 == 0
        val timestamp = date.time - batchIdx * 60 * 60 * 1000 - idx * 10
        createBaseNote(
            title = if (isList) "List $batchIdx" else "Note $batchIdx",
            type = if (isList) Type.LIST else Type.NOTE,
            pinned = batchIdx % 5 == 0,
            body = if (isList) getRandomString(idx * 10, (batchIdx * 10 + idx).toLong()) else "",
            items =
                if (isList) (0..batchIdx).map { createListItem(body = "Item$it") } else listOf(),
            color = Color.entries[batchIdx % Color.entries.size].toColorString(),
            files =
                if ((batchIdx + 1) % 5 == 0)
                    (0..batchIdx).map {
                        FileAttachment("File ${batchIdx}", "file${batchIdx}", MIME_TYPE_JSON)
                    }
                else mutableListOf(),
            images =
                if ((batchIdx + 2) % 6 == 0)
                    (0..batchIdx).map {
                        FileAttachment(
                            createDummyImageFile(context, fileName = "dummy_${batchIdx}_${it}.jpg")
                                .name,
                            "file${batchIdx}",
                            "image/jpeg",
                        )
                    }
                else mutableListOf(),
            reminders =
                if ((batchIdx + 2) % 5 == 0)
                    mutableListOf(
                        Reminder(
                            1,
                            Date(date.time + (batchIdx + 1) * 10 * 60 * 1000L),
                            Repetition(1, RepetitionTimeUnit.DAYS),
                        )
                    )
                else mutableListOf(),
            timestamp = timestamp,
            modifiedTimestamp = timestamp,
            labels = (0..(batchIdx / 2)).map { "label$it" },
        )
    }

fun getRandomString(length: Int, seed: Long = 42L): String {
    if (length <= 0) return ""
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    val random = Random(seed)
    return (1..length).map { allowedChars[random.nextInt(allowedChars.size)] }.joinToString("")
}

/**
 * Generates a dummy image with a stable background color and text, then saves it to the specified
 * internal storage directory as a JPEG.
 */
fun createDummyImageFile(
    context: ContextWrapper,
    fileName: String = "dummy_image.jpg",
    width: Int = 400,
    height: Int = 400,
): File {
    // 1. Resolve internal folder: /data/user/0/<package>/files/media
    val mediaDir = context.getCurrentImagesDirectory().apply { if (!exists()) mkdirs() }
    val imageFile = File(mediaDir, fileName)

    // 2. Create a Bitmap with stable RGB color
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundColor = android.graphics.Color.rgb(100, 150, 200)
    canvas.drawColor(backgroundColor)

    // Optional: Draw text or shapes to make images visually distinct in screenshots
    val paint =
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
    canvas.drawText("Dummy Image", 50f, (height / 2).toFloat(), paint)

    // 3. Save Bitmap to internal file stream as JPEG
    FileOutputStream(imageFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) // 90% quality compression
    }

    bitmap.recycle() // Clean up memory
    return imageFile
}

fun String.toMillis(
    pattern: String = "yyyy-MM-dd HH:mm",
    zoneId: ZoneId = ZoneId.of("UTC"), // Locking to UTC ensures determinism in Roborazzi
): Long {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return LocalDateTime.parse(this, formatter).atZone(zoneId).toInstant().toEpochMilli()
}
