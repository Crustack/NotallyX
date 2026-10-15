package com.philkes.notallyx.core.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
internal fun TriStateMultiChoiceContent(
    items: Array<String>,
    initialStates: Array<ToggleableState>,
    onItemClick: (index: Int, state: ToggleableState) -> Unit,
) {
    val states = remember { initialStates.clone() }

    Column(
        modifier =
            Modifier.fillMaxWidth().padding(top = 8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items.forEachIndexed { index, item ->
            var currentState by remember { mutableStateOf(states[index]) }
            TriStateChoiceItemRow(
                text = item,
                state = currentState,
                onClick = {
                    val nextState =
                        when (currentState) {
                            ToggleableState.Off -> ToggleableState.On
                            ToggleableState.Indeterminate -> ToggleableState.Off
                            ToggleableState.On -> ToggleableState.Off
                        }
                    currentState = nextState
                    states[index] = nextState
                    onItemClick(index, nextState)
                },
            )
        }
    }
}

@Composable
internal fun TriStateChoiceItemRow(
    text: String,
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("TriStateItem_$text")
                .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = state,
            onClick = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TriStateChoiceItemRowPreview() {
    NotallyXTheme {
        TriStateChoiceItemRow(text = "Test", state = ToggleableState.Indeterminate, onClick = {})
    }
}
