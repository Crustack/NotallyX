package com.philkes.notallyx.feature.editor.formatting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

data class TextFormattingState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isMonospace: Boolean = false,
    val isLink: Boolean = false,
)

fun AnnotatedString.getFormattingState(start: Int, end: Int): TextFormattingState {
    if (start >= end) return TextFormattingState()
    var isBold = false
    var isItalic = false
    var isStrikethrough = false
    var isMonospace = false
    var isLink = false

    spanStyles.forEach { range ->
        if (range.start < end && range.end > start) {
            val style = range.item
            if (
                style.fontWeight == FontWeight.Bold ||
                    (style.fontWeight != null && style.fontWeight!!.weight >= 700)
            ) {
                isBold = true
            }
            if (style.fontStyle == FontStyle.Italic) {
                isItalic = true
            }
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) {
                isStrikethrough = true
            }
            if (style.fontFamily == FontFamily.Monospace) {
                isMonospace = true
            }
        }
    }
    getStringAnnotations("URL", start, end).forEach { range ->
        if (range.start < end && range.end > start) {
            isLink = true
        }
    }
    return TextFormattingState(
        isBold = isBold,
        isItalic = isItalic,
        isStrikethrough = isStrikethrough,
        isMonospace = isMonospace,
        isLink = isLink,
    )
}

data class TextFormattingBarActions(
    val onBoldClick: () -> Unit = {},
    val onItalicClick: () -> Unit = {},
    val onStrikethroughClick: () -> Unit = {},
    val onMonospaceClick: () -> Unit = {},
    val onLinkClick: () -> Unit = {},
    val onClearFormattingClick: () -> Unit = {},
    val onCloseClick: () -> Unit = {},
)

@Composable
fun TextFormattingBar(
    formattingState: TextFormattingState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    actions: TextFormattingBarActions = TextFormattingBarActions(),
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.ime
                            .union(WindowInsets.navigationBars)
                            .only(WindowInsetsSides.Bottom)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconToggleButton(
                    checked = formattingState.isLink,
                    onCheckedChange = { actions.onLinkClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.link),
                        contentDescription = stringResource(id = R.string.link),
                        tint =
                            if (formattingState.isLink) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isBold,
                    onCheckedChange = { actions.onBoldClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_bold),
                        contentDescription = stringResource(id = R.string.bold),
                        tint =
                            if (formattingState.isBold) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isItalic,
                    onCheckedChange = { actions.onItalicClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_italic),
                        contentDescription = stringResource(id = R.string.italic),
                        tint =
                            if (formattingState.isItalic) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isStrikethrough,
                    onCheckedChange = { actions.onStrikethroughClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_strikethrough),
                        contentDescription = stringResource(id = R.string.strikethrough),
                        tint =
                            if (formattingState.isStrikethrough) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isMonospace,
                    onCheckedChange = { actions.onMonospaceClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.code),
                        contentDescription = stringResource(id = R.string.monospace),
                        tint =
                            if (formattingState.isMonospace) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(onClick = actions.onClearFormattingClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_clear),
                        contentDescription = stringResource(id = R.string.clear_formatting),
                    )
                }
            }

            IconButton(onClick = actions.onCloseClick) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = stringResource(id = R.string.cancel),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextFormattingBarPreview() {
    NotallyXTheme { TextFormattingBar(formattingState = TextFormattingState(isBold = true)) }
}
