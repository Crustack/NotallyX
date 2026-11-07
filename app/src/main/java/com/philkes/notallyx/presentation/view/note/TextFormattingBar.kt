package com.philkes.notallyx.presentation.view.note

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.theme.NotallyXTheme

data class TextFormattingState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isMonospace: Boolean = false,
    val isLink: Boolean = false,
)

@Composable
fun TextFormattingBar(
    formattingState: TextFormattingState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onBoldClick: () -> Unit = {},
    onItalicClick: () -> Unit = {},
    onStrikethroughClick: () -> Unit = {},
    onMonospaceClick: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    onClearFormattingClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconToggleButton(
                    checked = formattingState.isLink,
                    onCheckedChange = { onLinkClick() },
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
                    onCheckedChange = { onBoldClick() },
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
                    onCheckedChange = { onItalicClick() },
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
                    onCheckedChange = { onStrikethroughClick() },
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
                    onCheckedChange = { onMonospaceClick() },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.code),
                        contentDescription = stringResource(id = R.string.monospace),
                        tint =
                            if (formattingState.isMonospace) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    )
                }

                IconButton(onClick = onClearFormattingClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.format_clear),
                        contentDescription = stringResource(id = R.string.clear_formatting),
                    )
                }
            }

            IconButton(onClick = onCloseClick) {
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
    NotallyXTheme {
        TextFormattingBar(formattingState = TextFormattingState(isBold = true, isItalic = false))
    }
}
