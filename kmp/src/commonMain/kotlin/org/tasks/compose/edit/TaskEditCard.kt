package org.tasks.compose.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardRadius = 16.dp
private val LabelLetterSpacing = 0.8.sp

@Composable
fun TaskEditCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    val shape = RoundedCornerShape(CardRadius)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            color = color,
            shape = shape,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = color,
            shape = shape,
            content = content,
        )
    }
}

@Composable
fun TaskEditSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val base = MaterialTheme.typography.labelMedium
    val style = remember(base, color) {
        base.copy(
            color = color,
            fontWeight = FontWeight.Medium,
            letterSpacing = LabelLetterSpacing,
        )
    }
    Text(text = text.toUpperCase(Locale.current), style = style, modifier = modifier)
}
