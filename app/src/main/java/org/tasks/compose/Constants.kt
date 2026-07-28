package org.tasks.compose

import androidx.compose.material.ContentAlpha
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object Constants {
    const val ICON_ALPHA = 0.54f
    val KEYLINE_FIRST = 16.dp
    val HALF_KEYLINE = 8.dp

    @Composable
    fun textFieldColors() = OutlinedTextFieldDefaults.colors(
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.high),
    )
}