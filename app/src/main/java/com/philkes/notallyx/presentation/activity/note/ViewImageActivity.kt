package com.philkes.notallyx.presentation.activity.note

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.presentation.activity.note.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.presentation.collectIn
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.screens.ViewImageScreen
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import com.philkes.notallyx.utils.getUriForFile
import com.philkes.notallyx.utils.resolveAttachmentFile
import com.philkes.notallyx.utils.wrapWithChooser
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewImageActivity : AppCompatActivity() {

    private var currentImage: FileAttachment? = null
    private lateinit var deletedImages: ArrayList<FileAttachment>
    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private val imagesState = mutableStateOf<List<FileAttachment>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = NotallyXPreferences.getInstance(application)

        val savedList = savedInstanceState?.let {
            BundleCompat.getParcelableArrayList(
                it,
                EXTRA_DELETED_IMAGES,
                FileAttachment::class.java,
            )
        }
        deletedImages = savedList ?: ArrayList()

        val resultIntent = Intent()
        resultIntent.putExtra(EXTRA_DELETED_IMAGES, deletedImages)
        setResult(RESULT_OK, resultIntent)

        val savedImage = savedInstanceState?.let {
            BundleCompat.getParcelable(it, CURRENT_IMAGE, FileAttachment::class.java)
        }
        if (savedImage != null) {
            currentImage = savedImage
        }

        val initial = intent.getIntExtra(EXTRA_POSITION, 0)
        val id = intent.getLongExtra(EXTRA_SELECTED_BASE_NOTE, 0)

        setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                ViewImageScreen(
                    images = imagesState.value,
                    initialPage = initial,
                    onBackClick = { finish() },
                    onShareClick = { pos ->
                        if (pos in imagesState.value.indices) {
                            share(imagesState.value[pos])
                        }
                    },
                    onSaveClick = { pos ->
                        if (pos in imagesState.value.indices) {
                            saveToDevice(imagesState.value[pos])
                        }
                    },
                    onDeleteClick = { pos -> delete(pos) },
                )
            }
        }

        NotallyDatabase.getDatabase(this.application).collectIn(this@ViewImageActivity) { database
            ->
            val json =
                database?.let {
                    withContext(Dispatchers.IO) { database.getBaseNoteDao().getImages(id) }
                } ?: return@collectIn
            val original = Converters.jsonToFiles(json)
            val images = ArrayList<FileAttachment>(original.size)
            original.filterNotTo(images) { image -> deletedImages.contains(image) }
            imagesState.value = images
        }

        exportFileActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri -> writeImageToUri(uri) }
                }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putParcelable(CURRENT_IMAGE, currentImage)
            putParcelableArrayList(EXTRA_DELETED_IMAGES, deletedImages)
        }
    }

    private fun share(image: FileAttachment) {
        val file = application.resolveAttachmentFile(SUBFOLDER_IMAGES, image.localName)
        if (file != null && file.exists()) {
            val uri = getUriForFile(file)
            val intent =
                Intent(Intent.ACTION_SEND)
                    .apply {
                        type = image.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri(null, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    .wrapWithChooser(this@ViewImageActivity)
            startActivity(intent)
        }
    }

    private fun saveToDevice(image: FileAttachment) {
        val file = application.resolveAttachmentFile(SUBFOLDER_IMAGES, image.localName)
        if (file != null && file.exists()) {
            val intent =
                Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .apply {
                        type = image.mimeType
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(Intent.EXTRA_TITLE, "NotallyX Image")
                    }
                    .wrapWithChooser(this)
            currentImage = image
            exportFileActivityResultLauncher.launch(intent)
        }
    }

    private fun writeImageToUri(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val ci = requireNotNull(currentImage) { "currentImage is null" }
                val file = application.resolveAttachmentFile(SUBFOLDER_IMAGES, ci.localName)
                if (file != null && file.exists()) {
                    val output = contentResolver.openOutputStream(uri) as FileOutputStream
                    output.channel.truncate(0)
                    val input = FileInputStream(file)
                    input.copyTo(output)
                    input.close()
                    output.close()
                }
            }
            Toast.makeText(this@ViewImageActivity, R.string.saved_to_device, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun delete(position: Int) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_image_forever)
            .setCancelButton()
            .setPositiveButton(R.string.delete) { _, _ ->
                val currentList = imagesState.value.toMutableList()
                if (position in currentList.indices) {
                    val image = currentList.removeAt(position)
                    deletedImages.add(image)
                    imagesState.value = currentList
                    if (currentList.isEmpty()) {
                        finish()
                    }
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_POSITION = "notallyx.intent.extra.POSITION"
        const val CURRENT_IMAGE = "CURRENT_IMAGE"
        const val EXTRA_DELETED_IMAGES = "notallyx.intent.extra.DELETED_IMAGES"
    }
}
