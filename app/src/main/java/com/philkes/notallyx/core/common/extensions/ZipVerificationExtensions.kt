package com.philkes.notallyx.core.common.extensions

import android.content.Context
import android.net.Uri
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import net.lingala.zip4j.ZipFile

const val MIME_TYPE_ZIP = "application/zip"
const val MIME_TYPE_JSON = "application/json"

class ZipVerificationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

fun ZipFile.verify(databaseFile: File) {
    if (!isValidZipFile) {
        throw ZipVerificationException("ZipFile '${file}' is not a valid ZIP!")
    }
    if (isEncrypted) {
        verifyEncryptedArchive()
    } else {
        verifyCrc(databaseFile)
    }
}

private fun ZipFile.verifyCrc(databaseFile: File) {
    val dbEntry =
        fileHeaders.find { it.fileName == DATABASE_NAME }
            ?: throw ZipVerificationException("Database file missing in ZipFile '${file}'")
    // Compare CRC
    val originalCrc = calculateCrc(File(databaseFile.path).inputStream()).value
    if (dbEntry.crc != originalCrc) {
        throw ZipVerificationException(
            "ZipFile '${file}' contains mismatched CRC for '${DATABASE_NAME}'!"
        )
    }

    for (fileHeader in fileHeaders) {
        // Directories have no CRC
        if (fileHeader.isDirectory || fileHeader == dbEntry) continue
        // Get expected CRC from ZIP metadata
        val expected = fileHeader.crc
        // Compute actual CRC
        val actual = calculateCrc(this.getInputStream(fileHeader)).value
        if (expected != actual) {
            throw ZipVerificationException(
                "ZipFile '${file}' contains mismatched CRC for '${fileHeader.fileName}'!"
            )
        }
    }
}

private fun calculateCrc(inputStream: InputStream): CRC32 =
    CRC32().apply {
        inputStream.use { input ->
            val buffer = ByteArray(1024 * 64)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                update(buffer, 0, read)
            }
        }
    }

private fun ZipFile.verifyEncryptedArchive() {
    for (fh in fileHeaders) {
        if (fh.isDirectory) continue
        try {
            getInputStream(fh).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break // fully read → MAC verified
                }
            }
        } catch (e: Exception) {
            throw ZipVerificationException(
                "ZipFile '${file}' contains corrupt encrypted files!",
                e,
            ) // wrong password or corrupted entry
        }
    }
}

fun Context.md5Hash(uri: Uri): ByteArray? {
    return contentResolver.openInputStream(uri)?.md5Hash()
}

fun File.md5Hash(): ByteArray {
    return inputStream().md5Hash()
}

fun InputStream.md5Hash(): ByteArray {
    val digest = MessageDigest.getInstance("MD5")
    use { fis ->
        val buffer = ByteArray(BUFFER_SIZE / 2)
        var bytes = fis.read(buffer)
        while (bytes != -1) {
            digest.update(buffer, 0, bytes)
            bytes = fis.read(buffer)
        }
    }
    return digest.digest()
}
