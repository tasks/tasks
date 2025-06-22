package org.tasks.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.tasks.themes.ThemeColor

@Composable
fun ColorPicker(
    hasPro: Boolean,
    colors: List<ThemeColor>,
    onSelected: (ThemeColor) -> Unit,
    onColorWheelSelected: () -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 48.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ColorWheelCircle(
                onClick = onColorWheelSelected,
                hasPro = hasPro,
            )
        }
        items(colors) { color ->
            ColorCircle(
                color = color,
                locked = !(hasPro || color.isFree),
                onClick = { onSelected(color) }
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: ThemeColor,
    locked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(color.primaryColor))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (locked) {
            LockIcon(tint = Color(color.colorOnPrimary))
        }
    }
}

@Composable
private fun ColorWheelCircle(
    onClick: () -> Unit,
    hasPro: Boolean,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .size(48.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Magenta,
                        Color.Blue,
                        Color.Cyan,
                        Color.Green,
                        Color.Yellow,
                        Color.Red
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!hasPro) {
            LockIcon(tint = Color.Black)
        }
    }
}

@Composable
private fun LockIcon(tint: Color) {
    Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}
