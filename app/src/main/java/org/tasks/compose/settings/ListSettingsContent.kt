package org.tasks.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.tasks.compose.Constants

@Composable
fun ColumnScope.ListSettingsContent(
    color: Int,
    icon: String,
    text: String,
    error: String,
    requestKeyboard: Boolean,
    isNew: Boolean,
    setText: (String) -> Unit,
    pickColor: () -> Unit,
    clearColor: () -> Unit,
    pickIcon: () -> Unit,
    addShortcutToHome: () -> Unit,
    addWidgetToHome: () -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
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
    if (!isNew) {
        // TODO: support this for new filters too
        AddShortcutToHomeRow(
            onClick = { addShortcutToHome() },
        )
        AddWidgetToHomeRow(
            onClick = { addWidgetToHome() }
        )
    }
    extensionContent()
}
