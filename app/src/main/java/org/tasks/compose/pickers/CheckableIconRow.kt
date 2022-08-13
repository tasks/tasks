package org.tasks.compose.pickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun CheckableIconRow(
    icon: Painter,
    tint: Color,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CheckableIconRow(
        icon = icon,
        tint = tint,
        selected = selected,
        onClick = onClick,
        content = {
            Text(
                text = text,
                style = MaterialTheme.typography.body1,
            )
        }
    )
}

@Composable
fun CheckableIconRow(
    icon: Painter,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = tint.copy(alpha = ContentAlpha.medium),
            modifier = Modifier.padding(start = 16.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(56.dp))
        }
    }
}