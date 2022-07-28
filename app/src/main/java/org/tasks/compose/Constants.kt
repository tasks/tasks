package org.tasks.compose

import androidx.annotation.StringRes
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.*

object Constants {
    const val ICON_ALPHA = 0.54f
    val KEYLINE_FIRST = 16.dp
    val HALF_KEYLINE = 8.dp

    @Composable
    fun TextButton(@StringRes text: Int, onClick: () -> Unit) {
        androidx.compose.material.TextButton(
            onClick = onClick,
            colors = textButtonColors()
        ) {
            Text(
                stringResource(text).uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.button
            )
        }
    }

    @Composable
    fun textButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colors.secondary
    )

    @Composable
    fun textFieldColors() = TextFieldDefaults.textFieldColors(
        cursorColor = MaterialTheme.colors.secondary,
        focusedLabelColor = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.high),
        focusedIndicatorColor = MaterialTheme.colors.secondary.copy(alpha = ContentAlpha.high),
    )
}