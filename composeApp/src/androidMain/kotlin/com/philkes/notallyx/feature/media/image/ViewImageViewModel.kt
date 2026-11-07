package com.philkes.notallyx.feature.media.image

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_IMAGES
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.AndroidFileStorage
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.FileStorage
import com.philkes.notallyx.core.platform.JvmFileStorage
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.platform.PlatformUri
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewImageViewModel(
    preferences: NotallyXPreferences,
    databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    pathProvider: PathProvider = DesktopPathProvider(),
    private val fileStorage: FileStorage = JvmFileStorage(),
) : BaseViewModel(preferences, databaseStateFlow, pathProvider) {

    constructor(
        app: Application,
        preferences: NotallyXPreferences,
    ) : this(
        preferences = preferences,
        databaseStateFlow = NotallyDatabase.getDatabase(app),
        pathProvider = AndroidPathProvider(app),
        fileStorage = AndroidFileStorage(app),
    ) {
        this.app = app
    }

    private val _sideEffects = Channel<ViewImageSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ViewImageSideEffect> = _sideEffects.receiveAsFlow()

    private val _images = MutableStateFlow<List<FileAttachment>>(emptyList())
    private val _deletedImages = mutableListOf<FileAttachment>()
    private val _initialPage = MutableStateFlow(0)
    private val _activeDialog = MutableStateFlow<ViewImageDialogState?>(null)
    private val _currentImageToExport = MutableStateFlow<FileAttachment?>(null)

    val uiState: StateFlow<ViewImageUiState> =
        combine(_images, _initialPage, _activeDialog) { images, initialPage, activeDialog ->
                ViewImageUiState(
                    images = images,
                    initialPage = initialPage,
                    activeDialog = activeDialog,
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ViewImageUiState(isLoading = true),
            )

    fun onAction(action: ViewImageAction) {
        when (action) {
            is ViewImageAction.Init -> loadImages(action.key)
            is ViewImageAction.ShareClicked -> shareImage(action.position)
            is ViewImageAction.SaveClicked -> requestExport(action.position)
            is ViewImageAction.ExportConfirmed -> exportImage(action.destinationUri)
            is ViewImageAction.DeleteClicked -> {
                _activeDialog.update { ViewImageDialogState.ConfirmDelete(action.position) }
            }
            is ViewImageAction.DismissDialog -> {
                _activeDialog.update { null }
            }
            is ViewImageAction.ConfirmDelete -> deleteImage(action.position)
            is ViewImageAction.BackClicked -> handleBack()
        }
    }

    private fun loadImages(key: NotallyNavKey.ViewImage) {
        _deletedImages.clear()
        _initialPage.value = key.initialIndex
        val noteId = key.noteId
        if (noteId != null && noteId != 0L) {
            viewModelScope.launch {
                val db = databaseStateFlow.filterNotNull().first()
                val json = withContext(Dispatchers.IO) { db.getBaseNoteDao().getImages(noteId) }
                val original = Converters.jsonToFiles(json)
                _images.value = original.filterNot { _deletedImages.contains(it) }
            }
        } else if (key.imageUris.isNotEmpty()) {
            _images.value =
                key.imageUris
                    .map { uriString ->
                        FileAttachment(
                            localName = uriString,
                            originalName = uriString,
                            mimeType = "image/*",
                        )
                    }
                    .filterNot { _deletedImages.contains(it) }
        }
    }

    private fun shareImage(position: Int) {
        val currentImages = _images.value
        if (position !in currentImages.indices) return
        val image = currentImages[position]
        val file =
            pathProvider.resolveAttachmentFile(
                SUBFOLDER_IMAGES,
                image.localName,
                preferences.dataInPublicFolder.value,
            )
        if (file != null && file.exists() && hasApp) {
            val uri = app.getUriForFile(file)
            viewModelScope.launch {
                _sideEffects.send(
                    ViewImageSideEffect.ShareImage(PlatformUri(uri.toString()), image.mimeType)
                )
            }
        }
    }

    private fun requestExport(position: Int) {
        val currentImages = _images.value
        if (position !in currentImages.indices) return
        val image = currentImages[position]
        _currentImageToExport.value = image
        viewModelScope.launch {
            _sideEffects.send(ViewImageSideEffect.LaunchExport(image.originalName, image.mimeType))
        }
    }

    private fun exportImage(destinationUri: PlatformUri) {
        val image = _currentImageToExport.value ?: return
        val file =
            pathProvider.resolveAttachmentFile(
                SUBFOLDER_IMAGES,
                image.localName,
                preferences.dataInPublicFolder.value,
            ) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fileStorage.openOutputStream(destinationUri)?.use { output ->
                    FileInputStream(file).use { input -> input.copyTo(output) }
                }
                _sideEffects.send(ViewImageSideEffect.ShowToast(R.string.saved_to_device))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun deleteImage(position: Int) {
        _activeDialog.update { null }
        val currentList = _images.value.toMutableList()
        if (position in currentList.indices) {
            val deleted = currentList.removeAt(position)
            _deletedImages.add(deleted)
            _images.value = currentList
            viewModelScope.launch {
                _sideEffects.send(
                    ViewImageSideEffect.NotifyImagesDeleted(_deletedImages.map { it.localName })
                )
                if (currentList.isEmpty()) {
                    _sideEffects.send(ViewImageSideEffect.NavigateBack)
                }
            }
        }
    }

    private fun handleBack() {
        viewModelScope.launch {
            if (_deletedImages.isNotEmpty()) {
                _sideEffects.send(
                    ViewImageSideEffect.NotifyImagesDeleted(_deletedImages.map { it.localName })
                )
            }
            _sideEffects.send(ViewImageSideEffect.NavigateBack)
        }
    }
}
