package org.tasks.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun border() = MaterialTheme.colors.onSurface.copy(alpha = .5f)

@Composable
fun OutlinedBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .height(45.dp)
            .border(1.dp, color = border(), RoundedCornerShape(4.dp))
            .padding(start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
