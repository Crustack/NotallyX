package com.philkes.notallyx.feature.media.image

import android.app.Dialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewImageActivity : AppCompatActivity() {

    private var currentImage: FileAttachment? = null
    private lateinit var deletedImages: ArrayList<FileAttachment>
    private lateinit var exportFileActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var preferences: NotallyXPreferences
    private val imagesState = mutableStateOf<List<FileAttachment>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = NotallyXPreferences.getInstance(application)

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
        val composeView = ComposeView(this)
        val dialog =
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setContentView(composeView)
            }

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                ConfirmDialog(
                    message = stringResource(R.string.delete_image_forever),
                    positiveButtonText = stringResource(R.string.delete),
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = {
                        val currentList = imagesState.value.toMutableList()
                        if (position in currentList.indices) {
                            val image = currentList.removeAt(position)
                            deletedImages.add(image)
                            imagesState.value = currentList
                            if (currentList.isEmpty()) {
                                finish()
                            }
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_POSITION = "notallyx.intent.extra.POSITION"
        const val CURRENT_IMAGE = "CURRENT_IMAGE"
        const val EXTRA_DELETED_IMAGES = "notallyx.intent.extra.DELETED_IMAGES"
    }
}
