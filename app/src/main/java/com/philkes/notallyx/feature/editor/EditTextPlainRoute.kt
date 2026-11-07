package com.philkes.notallyx.feature.editor

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.feature.editor.screens.EditTextPlainScreen
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.NotallyNavKey
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditTextPlainRoute(
    key: NotallyNavKey.EditTextPlain,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditTextPlainViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val saveAsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.onAction(EditTextPlainAction.SaveToUri(uri))
                }
            }
        }

    LaunchedEffect(key.fileUri) { viewModel.onAction(EditTextPlainAction.Init(key.fileUri)) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is EditTextPlainSideEffect.ShowToast -> {
                    context.showToast(effect.messageResId)
                }
                is EditTextPlainSideEffect.LaunchSaveAs -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "text/*"
                            putExtra(Intent.EXTRA_TITLE, effect.defaultFileName)
                        }
                    saveAsLauncher.launch(intent)
                }
                is EditTextPlainSideEffect.NavigateToEditNote -> {
                    navigator?.let { nav ->
                        nav.navigate(NotallyNavKey.EditNote(noteId = effect.noteId))
                        nav.currentStack.remove(key)
                    }
                }
                is EditTextPlainSideEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    BackHandler { viewModel.onAction(EditTextPlainAction.BackClicked) }

    EditTextPlainScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = { viewModel.onAction(EditTextPlainAction.BackClicked) },
        modifier = modifier,
    )
}
