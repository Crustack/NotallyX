package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.contentColorFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopAppBar(
    searchState: SearchState,
    onQueryChange: (String) -> Unit,
    onNextResult: () -> Unit,
    onPrevResult: () -> Unit,
    onCloseSearch: () -> Unit,
    backgroundColor: Color,
) {
    val contentColor = contentColorFor(backgroundColor)
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val searchHint = stringResource(id = R.string.search)
                BasicTextField(
                    value = searchState.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).semantics { contentDescription = searchHint },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                    singleLine = true,
                    cursorBrush = SolidColor(contentColor),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchState.query.isEmpty()) {
                                Text(
                                    text = searchHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = contentColor.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                val countText =
                    if (searchState.query.isNotEmpty()) {
                        if (searchState.resultsCount > 0) {
                            "${searchState.resultPos + 1}/${searchState.resultsCount}"
                        } else {
                            "0/0"
                        }
                    } else {
                        "0/0"
                    }

                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                IconButton(
                    onClick = onPrevResult,
                    enabled = searchState.resultsCount > 0,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_upward),
                        contentDescription = stringResource(id = R.string.previous),
                    )
                }

                IconButton(
                    onClick = onNextResult,
                    enabled = searchState.resultsCount > 0,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_downward),
                        contentDescription = stringResource(id = R.string.next),
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onCloseSearch) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = stringResource(id = R.string.back),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor,
                navigationIconContentColor = contentColor,
                actionIconContentColor = contentColor,
                titleContentColor = contentColor,
            ),
    )
}
