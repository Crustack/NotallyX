package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

private fun createSwipeModifier(onSwipeRight: (() -> Unit), onSwipeLeft: (() -> Unit)): Modifier {
    return Modifier.pointerInput(onSwipeRight, onSwipeLeft) {
        var horizontalDragOffset = 0f
        detectHorizontalDragGestures(
            onDragStart = { horizontalDragOffset = 0f },
            onDragEnd = {
                if (horizontalDragOffset > 80f) {
                    onSwipeRight?.invoke()
                } else if (horizontalDragOffset < -80f) {
                    onSwipeLeft?.invoke()
                }
                horizontalDragOffset = 0f
            },
            onDragCancel = { horizontalDragOffset = 0f },
            onHorizontalDrag = { change, dragAmount ->
                if (
                    (dragAmount > 0 && onSwipeRight != null) ||
                        (dragAmount < 0 && onSwipeLeft != null) ||
                        horizontalDragOffset != 0f
                ) {
                    change.consume()
                    horizontalDragOffset += dragAmount
                }
            },
        )
    }
}

/** Composable replacing recycler_list_item.xml layout. */
@Composable
internal fun ChecklistItemRow(
    value: String,
    onValueChange: (String) -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    isChild: Boolean = false,
    showDelete: Boolean = true,
    focusRequester: FocusRequester? = null,
    onEnterKey: (() -> Unit)? = null,
    onBackspaceOnEmpty: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    onSwipeRight: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
) {
    val swipeModifier =
        if (onSwipeRight != null && onSwipeLeft != null) {
            createSwipeModifier(onSwipeRight, onSwipeLeft)
        } else Modifier

    Row(
        modifier = modifier.fillMaxWidth().then(swipeModifier).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isChild) {
            Spacer(modifier = Modifier.width(20.dp))
        }

        if (showDragHandle) {
            Icon(
                painter = painterResource(id = R.drawable.drag_handle),
                contentDescription = stringResource(R.string.drag_handle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = value },
            colors =
                CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.onSurface,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = MaterialTheme.colorScheme.surface,
                ),
        )

        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            val textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    textDecoration =
                        if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color =
                        if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                )

            val textFieldModifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                        else Modifier
                    )
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if (keyEvent.key == Key.Enter && onEnterKey != null) {
                                onEnterKey()
                                true
                            } else if (
                                keyEvent.key == Key.Backspace &&
                                    value.isEmpty() &&
                                    onBackspaceOnEmpty != null
                            ) {
                                onBackspaceOnEmpty()
                                true
                            } else false
                        } else false
                    }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = textFieldModifier,
                textStyle = textStyle,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onEnterKey?.invoke() }),
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = value,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = remember { MutableInteractionSource() },
                        placeholder = { Text(stringResource(R.string.item)) },
                        container = {},
                        contentPadding = PaddingValues(0.dp),
                    )
                },
            )
        }

        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            value = "Buy groceries",
            onValueChange = {},
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowUncheckedPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            value = "Buy groceries",
            onValueChange = {},
            isChecked = false,
            onCheckedChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistItemRowEmptyPreview() {
    NotallyXTheme {
        ChecklistItemRow(
            value = "",
            onValueChange = {},
            isChecked = false,
            onCheckedChange = {},
        )
    }
}
