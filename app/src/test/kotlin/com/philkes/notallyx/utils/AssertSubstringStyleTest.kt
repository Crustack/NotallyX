package com.philkes.notallyx.utils

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.philkes.notallyx.test.assertSubtextHasLink
import com.philkes.notallyx.test.assertSubtextHasNoFormatting
import com.philkes.notallyx.test.assertSubtextHasStrikethrough
import com.philkes.notallyx.test.assertSubtextIsBold
import com.philkes.notallyx.test.assertSubtextIsItalic
import com.philkes.notallyx.test.assertSubtextIsMonospace
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AssertSubstringStyleTest {

    @Test
    fun testAssertSubtextIsBold() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("This is ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bold") }
            append(" text.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("This is bold text.", substring = true)
            .assertSubtextIsBold("bold")
            .assertSubtextHasNoFormatting("This is ")
    }

    @Test
    fun testAssertSubtextIsItalic() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("This is ")
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("italic") }
            append(" text.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("This is italic text.", substring = true)
            .assertSubtextIsItalic("italic")
            .assertSubtextHasNoFormatting(" text.")
    }

    @Test
    fun testAssertSubtextHasStrikethrough() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("This is ")
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append("strikethrough")
            }
            append(" text.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("This is strikethrough text.", substring = true)
            .assertSubtextHasStrikethrough("strikethrough")
    }

    @Test
    fun testAssertSubtextIsMonospace() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("This is ")
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append("monospace") }
            append(" text.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("This is monospace text.", substring = true)
            .assertSubtextIsMonospace("monospace")
    }

    @Test
    fun testAssertSubtextHasLink() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("Visit ")
            withAnnotation("URL", "https://example.com") { append("Example") }
            append(" website.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("Visit Example website.", substring = true)
            .assertSubtextHasLink("Example", "https://example.com")
            .assertSubtextHasLink("Example")
    }

    @Test
    fun testEditableTextFieldWithFormatting() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("Editable ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("field") }
        }

        setContent {
            BasicTextField(
                value = TextFieldValue(annotatedString),
                onValueChange = {},
            )
        }

        onNodeWithText("Editable field")
            .assertSubtextIsBold("field")
            .assertSubtextHasNoFormatting("Editable")
    }

    @Test
    fun testContiguousSpansCoveringSubstring() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("bo") }
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("ld") }
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("bold").assertSubtextIsBold("bold")
    }

    @Test(expected = AssertionError::class)
    fun testAssertSubtextIsBoldFailsWhenNotBold() = runComposeUiTest {
        setContent { Text(text = "Plain unformatted text") }

        onNodeWithText("Plain unformatted text").assertSubtextIsBold("Plain")
    }

    @Test(expected = AssertionError::class)
    fun testAssertSubtextHasLinkFailsWithWrongUrl() = runComposeUiTest {
        val annotatedString = buildAnnotatedString {
            append("Visit ")
            withAnnotation("URL", "https://example.com") { append("Example") }
            append(" website.")
        }

        setContent { Text(text = annotatedString) }

        onNodeWithText("Visit Example website.", substring = true)
            .assertSubtextHasLink("Example", "https://wrong.com")
    }
}
