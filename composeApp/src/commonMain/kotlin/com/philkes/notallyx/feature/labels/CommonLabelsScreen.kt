package com.philkes.notallyx.feature.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonLabelsScreen(
    labelDao: LabelDao,
    onBack: () -> Unit,
    onLabelClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val labels by labelDao.getAll().collectAsState(initial = emptyList())
    var newLabelText by remember { mutableStateOf("") }

    var labelToDelete by remember { mutableStateOf<Label?>(null) }
    var labelToEdit by remember { mutableStateOf<Label?>(null) }

    fun addLabel() {
        val trimmed = newLabelText.trim()
        if (trimmed.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                labelDao.insert(Label(value = trimmed, order = labels.size))
            }
            newLabelText = ""
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_LABELS),
        topBar = {
            TopAppBar(
                title = { Text("Labels", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Add label input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newLabelText,
                    onValueChange = { newLabelText = it },
                    placeholder = { Text("Create new label…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
                    modifier = Modifier.weight(1f).testTag(TestTags.ADD_LABEL_INPUT),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { addLabel() },
                    modifier = Modifier.testTag(TestTags.ADD_LABEL_BUTTON),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add label")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (labels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No labels yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(labels, key = { it.value }) { label ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { onLabelClick(label.value) }
                                    .padding(vertical = 8.dp)
                                    .testTag(TestTags.LABEL_ITEM),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label.value,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { labelToEdit = label }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit label")
                            }
                            IconButton(onClick = { labelToDelete = label }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete label")
                            }
                        }
                    }
                }
            }
        }
    }

    labelToDelete?.let { label ->
        ConfirmDialog(
            title = "Delete Label",
            message = "Are you sure you want to delete the label \"${label.value}\"?",
            onDismissRequest = { labelToDelete = null },
            onConfirm = {
                scope.launch(Dispatchers.IO) { labelDao.delete(label.value) }
                labelToDelete = null
            },
        )
    }

    labelToEdit?.let { label ->
        TextInputDialog(
            title = "Edit Label",
            initialValue = label.value,
            onDismissRequest = { labelToEdit = null },
            onConfirm = { newValue ->
                val trimmed = newValue.trim()
                if (trimmed.isNotEmpty() && trimmed != label.value) {
                    scope.launch(Dispatchers.IO) { labelDao.update(label.value, trimmed) }
                }
                labelToEdit = null
            },
        )
    }
}
