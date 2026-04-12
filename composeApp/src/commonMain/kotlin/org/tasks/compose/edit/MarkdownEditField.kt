package org.tasks.compose.edit

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations

/**
 * A text field that renders its value as markdown when it has content and isn't focused.
 * Tapping the preview focuses the field for editing. While focused (or when empty), the
 * raw text is editable via a [BasicTextField].
 *
 * @param maxPreviewLines When set, the markdown preview truncates to this many lines and
 *   shows a "Show more" / "Show less" toggle.
 * @param contentPadding Horizontal padding applied to the text content and markdown preview
 *   individually (not the outer container), so elements like the "Show more" button can
 *   bleed into the gutter for proper highlight alignment.
 */
@Composable
fun MarkdownEditField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxPreviewLines: Int = Int.MAX_VALUE,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: ((Boolean) -> Unit)? = null,
    contentPadding: Dp = 0.dp,
) {
    var focused by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    // Sync when the external value changes (e.g. task loaded, undo).
    if (textFieldValue.text != value) {
        textFieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
    }
    val mdColors = markdownColor(
        text = textStyle.color.takeIf { it.isSpecified } ?: MaterialTheme.colorScheme.onSurface,
    )
    val mdTypography = markdownTypography(
        paragraph = textStyle,
    )

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onValueChange(newValue.text)
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                val wasFocused = focused
                focused = state.isFocused
                onFocusChanged?.invoke(state.isFocused)
                if (!wasFocused && state.isFocused) {
                    // Entering edit mode from preview — place cursor at end.
                    textFieldValue = textFieldValue.copy(
                        selection = TextRange(textFieldValue.text.length)
                    )
                }
            },
        decorationBox = { inner ->
            val showPreview = !focused && value.isNotEmpty()
            val horizontalPadding = Modifier.padding(horizontal = contentPadding)
            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                // Raw text field — always in the tree so focus/IME works.
                // When preview is active, collapse to zero height so the Box
                // sizes to the (possibly truncated) markdown content instead.
                Box(
                    modifier = if (showPreview) {
                        Modifier.height(0.dp).alpha(0f)
                    } else {
                        horizontalPadding
                    },
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                    inner()
                }
                // Markdown preview overlay — shown when not focused.
                if (showPreview) {
                    val lines = value.lines()
                    val hasMore = maxPreviewLines < Int.MAX_VALUE && lines.size > maxPreviewLines
                    val displayContent = if (hasMore && !expanded) {
                        lines.take(maxPreviewLines).joinToString("\n")
                    } else {
                        value
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                            ) { focusRequester.requestFocus() },
                    ) {
                        Markdown(
                            content = displayContent,
                            colors = mdColors,
                            typography = mdTypography,
                            animations = markdownAnimations(
                                animateTextSize = { this },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(horizontalPadding),
                        )
                        if (hasMore) {
                            TextButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.padding(
                                    start = (contentPadding - 12.dp).coerceAtLeast(0.dp),
                                ),
                            ) {
                                Text(
                                    text = if (expanded) "Show less" else "Show more",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
