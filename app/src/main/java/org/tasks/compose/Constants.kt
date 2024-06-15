package org.tasks.compose

import androidx.annotation.StringRes
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

object Constants {
    const val ICON_ALPHA = 0.54f
    val KEYLINE_FIRST = 16.dp
    val HALF_KEYLINE = 8.dp

    @Composable
    fun TextButton(@StringRes text: Int, onClick: () -> Unit) {
        androidx.compose.material3.TextButton(
            onClick = onClick,
            colors = textButtonColors()
        ) {
            Text(
                stringResource(text).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    @Composable
    fun textButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun textFieldColors() = OutlinedTextFieldDefaults.colors(
        cursorColor = MaterialTheme.colorScheme.onSurface,
        focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.high),
    )
}