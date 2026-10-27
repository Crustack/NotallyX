package com.philkes.notallyx.utils.backup

import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.utils.backup.BackupHistory.HISTORY_FOLDER
import com.philkes.notallyx.utils.backup.BackupHistory.MEDIA_FOLDER
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Plain JVM tests for [BackupHistory]. Database files are faked as text files with the content
 * `<noteCount>|<attachment>,<attachment>` which [FakeInspector] parses; "corrupt" can't be read.
 */
class BackupHistoryTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var root: File
    private var backupCounter = 0

    private object FakeInspector : BackupHistory.SnapshotInspector {
        override fun inspect(databaseFile: File): BackupHistory.SnapshotInfo {
            val content = databaseFile.readText()
            require(content != "corrupt") { "Can not read $databaseFile" }
            val (count, attachments) = content.split("|")
            return BackupHistory.SnapshotInfo(
                count.toInt(),
                attachments.split(",").filter { it.isNotBlank() }.toSet(),
            )
        }
    }

    @Before
    fun setUp() {
        root = tempFolder.newFolder("backups")
        backupCounter = 0
    }

    private fun day(date: String): Long =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).parse("$date 12:00")!!.time

    /** Creates a backup directory like the current state would be exported. */
    private fun backupDir(noteCount: Int, vararg attachments: String): File {
        val dir = File(root, "backup_${backupCounter++}").apply { mkdirs() }
        File(dir, DATABASE_NAME).writeText("$noteCount|${attachments.joinToString(",")}")
        attachments.forEach { path ->
            File(dir, path).apply {
                parentFile!!.mkdirs()
                writeText("content of $path")
            }
        }
        return dir
    }

    private fun merge(previous: File?, new: File, timestamp: Long, maxSnapshots: Int = 3) =
        BackupHistory.merge(previous, new, timestamp, maxSnapshots, FakeInspector)

    private fun File.snapshots(): List<String> =
        File(this, HISTORY_FOLDER)
            .listFiles { file -> file.isFile }
            ?.map { it.name }
            ?.sortedDescending() ?: emptyList()

    private fun File.media(): List<String> =
        BackupHistory.ATTACHMENT_FOLDERS.flatMap { folder ->
                File(this, "$HISTORY_FOLDER/$MEDIA_FOLDER/$folder").listFiles()?.map {
                    "$folder/${it.name}"
                } ?: emptyList()
            }
            .sorted()

    @Test
    fun firstBackup_hasNoHistory() {
        val new = backupDir(2, "Images/a.jpg")

        val result = merge(null, new, day("2026-10-27"))

        assertEquals(emptyList<String>(), result.snapshots)
        assertFalse(File(new, HISTORY_FOLDER).exists())
    }

    @Test
    fun previousDatabase_isStoredAsSnapshotOfItsDay() {
        val previous = backupDir(2)
        val new = backupDir(3)

        val result = merge(previous, new, day("2026-10-26"))

        assertEquals(listOf("NotallyDatabase_2026-10-26"), result.snapshots)
        assertEquals("2|", File(new, "$HISTORY_FOLDER/NotallyDatabase_2026-10-26").readText())
        assertEquals("3|", File(new, DATABASE_NAME).readText())
    }

    @Test
    fun sameDay_replacesSnapshotOfThatDay() {
        val first = backupDir(1)
        val second = backupDir(2)
        merge(first, second, day("2026-10-27"))
        val third = backupDir(3)

        val result = merge(second, third, day("2026-10-27"))

        assertEquals(listOf("NotallyDatabase_2026-10-27"), result.snapshots)
        assertEquals("2|", File(third, "$HISTORY_FOLDER/NotallyDatabase_2026-10-27").readText())
    }

    @Test
    fun history_isCarriedOverAndLimitedToMaxSnapshots() {
        var previous = backupDir(1)
        listOf("2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23").forEachIndexed { index, date
            ->
            val new = backupDir(index + 2)
            merge(previous, new, day(date), maxSnapshots = 3)
            previous = new
        }

        assertEquals(
            listOf(
                "NotallyDatabase_2026-10-23",
                "NotallyDatabase_2026-10-22",
                "NotallyDatabase_2026-10-21",
            ),
            previous.snapshots(),
        )
    }

    @Test
    fun emptyDatabase_isNeverStoredAsSnapshot() {
        val good = backupDir(5)
        val emptied = backupDir(0)
        merge(good, emptied, day("2026-10-26"))

        // Further backups of the emptied database on the same and following days
        val emptiedAgain = backupDir(0)
        val sameDay = merge(emptied, emptiedAgain, day("2026-10-26"))
        val emptiedNextDay = backupDir(0)
        val nextDay = merge(emptiedAgain, emptiedNextDay, day("2026-10-27"))

        assertTrue(sameDay.skippedEmptySnapshot)
        assertTrue(nextDay.skippedEmptySnapshot)
        assertEquals(listOf("NotallyDatabase_2026-10-26"), emptiedNextDay.snapshots())
        assertEquals(
            "5|",
            File(emptiedNextDay, "$HISTORY_FOLDER/NotallyDatabase_2026-10-26").readText(),
        )
    }

    @Test
    fun unreadableDatabase_doesNotReplaceGoodSnapshotOfSameDay() {
        val good = backupDir(5)
        val corrupt = backupDir(0).apply { File(this, DATABASE_NAME).writeText("corrupt") }
        merge(good, corrupt, day("2026-10-27"))
        val new = backupDir(1)

        merge(corrupt, new, day("2026-10-27"))

        assertEquals("5|", File(new, "$HISTORY_FOLDER/NotallyDatabase_2026-10-27").readText())
    }

    @Test
    fun unreadableDatabase_isKeptAsSnapshotOfNewDay() {
        val corrupt = backupDir(0).apply { File(this, DATABASE_NAME).writeText("corrupt") }
        val new = backupDir(1)

        merge(corrupt, new, day("2026-10-27"))

        assertEquals("corrupt", File(new, "$HISTORY_FOLDER/NotallyDatabase_2026-10-27").readText())
    }

    @Test
    fun newestSnapshotWithNotes_survivesRotation() {
        // Day 1: good state is stored, then only unreadable states follow on later days
        val good = backupDir(5)
        var previous = backupDir(0).apply { File(this, DATABASE_NAME).writeText("corrupt") }
        merge(good, previous, day("2026-10-20"), maxSnapshots = 2)
        listOf("2026-10-21", "2026-10-22", "2026-10-23").forEach { date ->
            val new = backupDir(0).apply { File(this, DATABASE_NAME).writeText("corrupt") }
            merge(previous, new, day(date), maxSnapshots = 2)
            previous = new
        }

        assertEquals(
            listOf(
                "NotallyDatabase_2026-10-23",
                "NotallyDatabase_2026-10-22",
                "NotallyDatabase_2026-10-20",
            ),
            previous.snapshots(),
        )
    }

    @Test
    fun removedAttachment_isMovedToMediaWhileReferencedBySnapshot() {
        val previous = backupDir(2, "Images/a.jpg", "Audios/b.m4a")
        val new = backupDir(1, "Images/a.jpg")

        val result = merge(previous, new, day("2026-10-26"))

        assertEquals(listOf("Audios/b.m4a"), result.movedToMedia)
        assertEquals(listOf("Audios/b.m4a"), new.media())
        assertEquals(
            "content of Audios/b.m4a",
            File(new, "$HISTORY_FOLDER/$MEDIA_FOLDER/Audios/b.m4a").readText(),
        )
    }

    @Test
    fun media_isRemovedWhenNoSnapshotReferencesItAnymore() {
        // 10-20: state with image x, 10-21: x removed
        var previous = backupDir(1, "Images/x.jpg")
        var new = backupDir(1)
        merge(previous, new, day("2026-10-20"), maxSnapshots = 2)
        assertEquals(listOf("Images/x.jpg"), new.media())
        // Two more days push the 10-20 snapshot (the only one referencing x) out
        listOf("2026-10-21", "2026-10-22").forEach { date ->
            previous = new
            new = backupDir(1)
            merge(previous, new, day(date), maxSnapshots = 2)
        }

        assertEquals(emptyList<String>(), new.media())
        assertFalse(File(new, "$HISTORY_FOLDER/$MEDIA_FOLDER").exists())
    }

    @Test
    fun media_isKeptIfAnySnapshotCanNotBeRead() {
        val previous = backupDir(1, "Images/x.jpg")
        val new = backupDir(1)
        merge(previous, new, day("2026-10-20"), maxSnapshots = 5)
        // The only snapshot referencing x becomes unreadable, so its references are unknown
        File(new, "$HISTORY_FOLDER/NotallyDatabase_2026-10-20").writeText("corrupt")
        val next = backupDir(1)

        merge(new, next, day("2026-10-21"), maxSnapshots = 5)

        assertEquals(listOf("Images/x.jpg"), next.media())
    }

    @Test
    fun attachmentBackInCurrentState_isRemovedFromMedia() {
        val previous = backupDir(1, "Images/x.jpg")
        val new = backupDir(1)
        merge(previous, new, day("2026-10-20"))
        val restored = backupDir(1, "Images/x.jpg")

        merge(new, restored, day("2026-10-21"))

        assertEquals(emptyList<String>(), restored.media())
        assertTrue(File(restored, "Images/x.jpg").exists())
    }

    @Test
    fun partialBackup_withHistoryAlreadyCopied_isHandled() {
        val previous = backupDir(2)
        merge(backupDir(1), previous, day("2026-10-25"))
        // Partial backups start from a copy of the previous content including History
        val new = backupDir(3)
        File(previous, HISTORY_FOLDER).copyRecursively(File(new, HISTORY_FOLDER))

        val result = merge(previous, new, day("2026-10-26"))

        assertEquals(
            listOf("NotallyDatabase_2026-10-26", "NotallyDatabase_2026-10-25"),
            result.snapshots,
        )
    }

    @Test
    fun snapshots_areOnlyInspectedWhenNeeded() {
        val inspected = mutableListOf<String>()
        val countingInspector = BackupHistory.SnapshotInspector { file ->
            inspected.add(file.name)
            FakeInspector.inspect(file)
        }
        var previous = backupDir(1)
        listOf("2026-10-20", "2026-10-21", "2026-10-22").forEach { date ->
            val new = backupDir(1)
            BackupHistory.merge(previous, new, day(date), 2, countingInspector)
            previous = new
        }
        inspected.clear()
        val new = backupDir(1)

        BackupHistory.merge(previous, new, day("2026-10-23"), 2, countingInspector)

        // Previous database + newest kept snapshot (has notes). No media, so the other snapshots
        // and the one being removed are never read.
        assertEquals(listOf(DATABASE_NAME, "NotallyDatabase_2026-10-23"), inspected)
    }
}
