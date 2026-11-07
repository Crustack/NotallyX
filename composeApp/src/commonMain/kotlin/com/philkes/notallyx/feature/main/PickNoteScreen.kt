package com.philkes.notallyx.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.back
import com.philkes.notallyx.close
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.empty_list
import com.philkes.notallyx.feature.main.routes.picknote.PickNoteAction
import com.philkes.notallyx.feature.main.routes.picknote.PickNoteUiState
import com.philkes.notallyx.select_note
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PickNoteScreen(
    state: PickNoteUiState,
    onAction: (PickNoteAction) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = { onAction(PickNoteAction.BackClicked) },
) {
    PickNoteScreen(
        notes = state.notes,
        isGrid = state.isGrid,
        imageRoot = state.imageRoot,
        onNoteClick = { onAction(PickNoteAction.NoteClicked(it)) },
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/** Stateless composable displaying notes for selection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickNoteScreen(
    notes: List<BaseNote>,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    imageRoot: File? = null,
    onNoteClick: (BaseNote) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.select_note)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (notes.isEmpty()) {
                Text(
                    text = stringResource(Res.string.empty_list),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (isGrid) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { _, note ->
                        BaseNoteCard(
                            baseNote = note,
                            imageRoot = imageRoot,
                            onClick = { onNoteClick(note) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { _, note ->
                        BaseNoteCard(
                            baseNote = note,
                            imageRoot = imageRoot,
                            onClick = { onNoteClick(note) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PickNoteScreenPreview() {
    NotallyXTheme { PickNoteScreen(notes = emptyList()) }
}
