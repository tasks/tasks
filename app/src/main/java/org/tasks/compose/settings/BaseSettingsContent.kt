package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.tasks.compose.Constants

@Composable
fun BaseSettingsContent(
    color: Color,
    icon: String,
    text: String,
    error: String,
    requestKeyboard: Boolean,
    setText: (String) -> Unit,
    pickColor: () -> Unit,
    clearColor: () -> Unit,
    pickIcon: () -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        TitleInput(
            text = text,
            error = error,
            requestKeyboard = requestKeyboard,
            modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST),
            setText = { setText(it) },
        )
        SelectColorRow(
            color = color,
            selectColor = { pickColor() },
            clearColor = { clearColor() },
        )
        SelectIconRow(
            icon = icon,
            selectIcon = { pickIcon() },
        )
        extensionContent()
    }
}
