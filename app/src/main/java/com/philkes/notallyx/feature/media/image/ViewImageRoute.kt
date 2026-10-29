package com.philkes.notallyx.feature.media.image

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun ViewImageRoute(
    key: NotallyNavKey.ViewImage,
    onDeletedImages: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { requireNotNull(context.findActivity()) }
    val scope = rememberCoroutineScope()

    val databaseStateFlow =
        NotallyDatabase.getDatabase(context.applicationContext as ContextWrapper)
    val database by databaseStateFlow.collectAsStateWithLifecycle()

    var images by remember { mutableStateOf<List<FileAttachment>>(emptyList()) }
    val deletedImages = remember { mutableStateListOf<FileAttachment>() }
    var imageToDeletePosition by remember { mutableStateOf<Int?>(null) }
    var currentImageToExport by remember { mutableStateOf<FileAttachment?>(null) }

    val handleBack: () -> Unit = {
        if (deletedImages.isNotEmpty()) {
            onDeletedImages(deletedImages.map { it.localName })
        }
        onBackClick()
    }

    BackHandler(onBack = handleBack)

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val image = currentImageToExport ?: return@let
                    val file =
                        activity.application.resolveAttachmentFile(
                            SUBFOLDER_IMAGES,
                            image.localName,
                        ) ?: return@let
                    scope.launch(Dispatchers.IO) {
                        try {
                            activity.contentResolver.openFileDescriptor(uri, "w")?.use { pfd ->
                                FileOutputStream(pfd.fileDescriptor).use { output ->
                                    FileInputStream(file).use { input -> input.copyTo(output) }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                        activity,
                                        R.string.saved_to_device,
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

    LaunchedEffect(key, database) {
        val noteId = key.noteId
        if (noteId != null && noteId != 0L) {
            val db = database ?: return@LaunchedEffect
            val json = withContext(Dispatchers.IO) { db.getBaseNoteDao().getImages(noteId) }
            val original = Converters.jsonToFiles(json)
            images = original.filterNot { deletedImages.contains(it) }
        } else if (key.imageUris.isNotEmpty()) {
            images =
                key.imageUris
                    .map { uriString ->
                        FileAttachment(
                            localName = uriString,
                            originalName = uriString,
                            mimeType = "image/*",
                        )
                    }
                    .filterNot { deletedImages.contains(it) }
        }
    }

    fun shareImage(image: FileAttachment) {
        val file = activity.application.resolveAttachmentFile(SUBFOLDER_IMAGES, image.localName)
        if (file != null && file.exists()) {
            val uri = activity.getUriForFile(file)
            val intent =
                Intent(Intent.ACTION_SEND)
                    .apply {
                        type = image.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(null, uri)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    .wrapWithChooser(activity)
            activity.startActivity(intent)
        }
    }

    fun saveToDevice(image: FileAttachment) {
        currentImageToExport = image
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .apply {
                    type = image.mimeType
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_TITLE, image.originalName)
                }
                .wrapWithChooser(activity)
        exportLauncher.launch(intent)
    }

    ViewImageScreen(
        images = images,
        initialPage = key.initialIndex,
        imageToDeletePosition = imageToDeletePosition,
        onBackClick = handleBack,
        onShareClick = { pos ->
            if (pos in images.indices) {
                shareImage(images[pos])
            }
        },
        onSaveClick = { pos ->
            if (pos in images.indices) {
                saveToDevice(images[pos])
            }
        },
        onDeleteClick = { pos -> imageToDeletePosition = pos },
        onDismissDeleteConfirm = { imageToDeletePosition = null },
        onConfirmDelete = { pos ->
            imageToDeletePosition = null
            if (pos in images.indices) {
                val currentList = images.toMutableList()
                val deleted = currentList.removeAt(pos)
                deletedImages.add(deleted)
                images = currentList
                onDeletedImages(deletedImages.map { it.localName })
                if (currentList.isEmpty()) {
                    handleBack()
                }
            }
        },
        modifier = modifier,
    )
}

const val EXTRA_POSITION = "notallyx.intent.extra.POSITION"
const val CURRENT_IMAGE = "CURRENT_IMAGE"
const val EXTRA_DELETED_IMAGES = "notallyx.intent.extra.DELETED_IMAGES"
