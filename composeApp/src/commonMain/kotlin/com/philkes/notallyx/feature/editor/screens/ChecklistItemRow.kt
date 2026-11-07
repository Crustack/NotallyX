package com.philkes.notallyx.feature.editor.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.contentColorFor

fun createSwipeModifier(onSwipeRight: (() -> Unit), onSwipeLeft: (() -> Unit)): Modifier {
    return Modifier.pointerInput(onSwipeRight, onSwipeLeft) {
        var horizontalDragOffset = 0f
        detectHorizontalDragGestures(
            onDragStart = { horizontalDragOffset = 0f },
            onDragEnd = {
                if (horizontalDragOffset > 80f) {
                    onSwipeRight.invoke()
                } else if (horizontalDragOffset < -80f) {
                    onSwipeLeft.invoke()
                }
                horizontalDragOffset = 0f
            },
            onDragCancel = { horizontalDragOffset = 0f },
            onHorizontalDrag = { change, dragAmount ->
                if ((dragAmount > 0) || (dragAmount < 0) || horizontalDragOffset != 0f) {
                    change.consume()
                    horizontalDragOffset += dragAmount
                }
            },
        )
    }
}

enum class DragState {
    NONE,
    PARENT,
    CHILD,
    LAST_CHILD,
}

/** Composable replacing recycler_list_item.xml layout. */
@Composable
fun ChecklistItemRow(
    value: String,
    onValueChange: (String) -> Unit,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    isChild: Boolean = false,
    showDelete: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
    onEnterKey: (() -> Unit)? = null,
    onBackspaceOnEmpty: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    onSwipeRight: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    dragState: DragState = DragState.NONE,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    dragHandleContentDescription: String = "Drag Handle",
    placeholderText: String = "Item",
    deleteContentDescription: String = "Delete",
) {
    val swipeModifier =
        if (onSwipeRight != null && onSwipeLeft != null) {
            createSwipeModifier(onSwipeRight, onSwipeLeft)
        } else Modifier

    val shadowElevation by
        animateDpAsState(
            targetValue = if (dragState != DragState.NONE) 4.dp else 0.dp,
            label = "shadowElevation",
        )

    val primaryColor = MaterialTheme.colorScheme.primary
    val borderModifier =
        if (dragState != DragState.NONE) {
            Modifier.drawWithContent {
                drawContent()
                drawDragHighlights(dragState, primaryColor)
            }
        } else Modifier

    val contentColor = contentColorFor(backgroundColor)
    Surface(
        modifier = modifier.fillMaxWidth().then(borderModifier),
        shape = RoundedCornerShape(0.dp),
        shadowElevation = shadowElevation,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.fillMaxWidth().then(swipeModifier).padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (isChild) {
                    Spacer(modifier = Modifier.width(20.dp))
                }

                if (showDragHandle) {
                    Box(
                        modifier = Modifier.height(48.dp).padding(start = 12.dp, end = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = dragHandleContentDescription,
                        )
                    }
                }

                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.semantics { contentDescription = value },
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = contentColor,
                            uncheckedColor = contentColor,
                            checkmarkColor = backgroundColor,
                        ),
                )

                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    val textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration =
                                if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                            color =
                                if (isChecked) contentColor.copy(alpha = 0.75f) else contentColor,
                        )

                    val textFieldModifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (focusRequester != null) Modifier.focusRequester(focusRequester)
                                else Modifier
                            )
                            .onKeyEvent { keyEvent ->
                                keyEvent.type == KeyEventType.KeyDown &&
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
                            }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = textFieldModifier,
                        textStyle = textStyle,
                        singleLine = false,
                        visualTransformation = visualTransformation,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { onEnterKey?.invoke() }),
                        decorationBox = { innerTextField ->
                            TextFieldDefaults.DecorationBox(
                                value = value,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = false,
                                visualTransformation = visualTransformation,
                                interactionSource = remember { MutableInteractionSource() },
                                placeholder = { Text(placeholderText) },
                                container = {},
                                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                            )
                        },
                    )
                }

                if (showDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = deleteContentDescription,
                        )
                    }
                }
            }
        }
    }
}

private fun ContentDrawScope.drawDragHighlights(
    isDragging: DragState,
    primaryColor: Color,
) {
    val strokeWidth = 1.5.dp.toPx()
    val halfStroke = strokeWidth / 2f
    val width = size.width
    val height = size.height

    val showTop = isDragging == DragState.PARENT
    val showBottom = isDragging == DragState.LAST_CHILD
    val showLeft = true
    val showRight = true

    if (showTop) {
        drawLine(
            color = primaryColor,
            start = Offset(0f, halfStroke),
            end = Offset(width, halfStroke),
            strokeWidth = strokeWidth,
        )
    }
    if (showBottom) {
        drawLine(
            color = primaryColor,
            start = Offset(0f, height - halfStroke),
            end = Offset(width, height - halfStroke),
            strokeWidth = strokeWidth,
        )
    }
    if (showLeft) {
        drawLine(
            color = primaryColor,
            start = Offset(halfStroke, 0f),
            end = Offset(halfStroke, height),
            strokeWidth = strokeWidth,
        )
    }
    if (showRight) {
        drawLine(
            color = primaryColor,
            start = Offset(width - halfStroke, 0f),
            end = Offset(width - halfStroke, height),
            strokeWidth = strokeWidth,
        )
    }
}
