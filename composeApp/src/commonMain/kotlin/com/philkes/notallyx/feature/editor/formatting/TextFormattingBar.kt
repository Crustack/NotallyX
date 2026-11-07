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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.bold
import com.philkes.notallyx.cancel
import com.philkes.notallyx.clear_formatting
import com.philkes.notallyx.close
import com.philkes.notallyx.code
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.format_bold
import com.philkes.notallyx.format_clear
import com.philkes.notallyx.format_italic
import com.philkes.notallyx.format_strikethrough
import com.philkes.notallyx.italic
import com.philkes.notallyx.link
import com.philkes.notallyx.monospace
import com.philkes.notallyx.strikethrough
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
        contentColor = contentColorFor(backgroundColor),
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
                        painter = painterResource(Res.drawable.link),
                        contentDescription = stringResource(Res.string.link),
                        tint =
                            if (formattingState.isLink) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isBold,
                    onCheckedChange = { actions.onBoldClick() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.format_bold),
                        contentDescription = stringResource(Res.string.bold),
                        tint =
                            if (formattingState.isBold) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isItalic,
                    onCheckedChange = { actions.onItalicClick() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.format_italic),
                        contentDescription = stringResource(Res.string.italic),
                        tint =
                            if (formattingState.isItalic) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isStrikethrough,
                    onCheckedChange = { actions.onStrikethroughClick() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.format_strikethrough),
                        contentDescription = stringResource(Res.string.strikethrough),
                        tint =
                            if (formattingState.isStrikethrough) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                    )
                }

                IconToggleButton(
                    checked = formattingState.isMonospace,
                    onCheckedChange = { actions.onMonospaceClick() },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.code),
                        contentDescription = stringResource(Res.string.monospace),
                        tint =
                            if (formattingState.isMonospace) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                    )
                }

                IconButton(onClick = actions.onClearFormattingClick) {
                    Icon(
                        painter = painterResource(Res.drawable.format_clear),
                        contentDescription = stringResource(Res.string.clear_formatting),
                    )
                }
            }

            IconButton(onClick = actions.onCloseClick) {
                Icon(
                    painter = painterResource(Res.drawable.close),
                    contentDescription = stringResource(Res.string.cancel),
                )
            }
        }
    }
}
