package com.philkes.notallyx.feature.editor.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.data.model.ListItem

data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val resultsCount: Int = 0,
    val resultPos: Int = 0,
)

internal class SearchHighlightVisualTransformation(
    private val query: String,
    private val selectedIndex: Int,
    private val highlightColor: Color = Color(0xFFFFF59D),
    private val selectedHighlightColor: Color = Color(0xFFFFC107),
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (query.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val rawText = text.text
        val occurrences = rawText.findAllOccurrences(query)
        if (occurrences.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(text)
        occurrences.forEachIndexed { index, (start, end) ->
            val color = if (index == selectedIndex) selectedHighlightColor else highlightColor
            builder.addStyle(
                style = SpanStyle(background = color),
                start = start,
                end = end,
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
internal fun createSearchHighlightListItemTransformation(
    noteState: EditNoteState,
    index: Int,
    searchState: SearchState,
    item: ListItem,
): SearchHighlightVisualTransformation {
    val priorCount =
        noteState.items.take(index).sumOf { it.body.findAllOccurrences(searchState.query).size }
    val countInThisItem = item.body.findAllOccurrences(searchState.query).size
    val selectedInThisItem =
        if (
            searchState.resultPos >= priorCount &&
                searchState.resultPos < priorCount + countInThisItem
        ) {
            searchState.resultPos - priorCount
        } else {
            -1
        }
    return SearchHighlightVisualTransformation(
        query = searchState.query,
        selectedIndex = selectedInThisItem,
    )
}
