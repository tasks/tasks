package org.tasks.compose.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DescriptionRow(
    description: String,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val showPreview = !focused && description.isNotEmpty()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (showPreview) Modifier.clickable { focusRequester.requestFocus() }
                    else Modifier
                )
                .padding(top = 20.dp, bottom = if (showPreview) 8.dp else 20.dp),
        ) {
            Text(
                text = "DESCRIPTION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                ),
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            val descriptionStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp,
            )
            MarkdownEditField(
                value = description,
                onValueChange = onDescriptionChange,
                textStyle = descriptionStyle,
                placeholder = "Add notes, links, or context…",
                maxPreviewLines = 4,
                focusRequester = focusRequester,
                onFocusChanged = { focused = it },
                contentPadding = 20.dp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
