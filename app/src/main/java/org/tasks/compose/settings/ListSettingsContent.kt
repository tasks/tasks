package org.tasks.compose.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.tasks.compose.Constants
import org.tasks.themes.ThemeColor

@Composable
fun ColumnScope.ListSettingsContent(
    hasPro: Boolean,
    color: Int,
    colors: List<ThemeColor>,
    icon: String,
    text: String,
    error: String,
    requestKeyboard: Boolean,
    isNew: Boolean,
    setText: (String) -> Unit,
    setColor: (Int) -> Unit,
    pickIcon: () -> Unit,
    addShortcutToHome: () -> Unit,
    addWidgetToHome: () -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
    purchase: () -> Unit,
) {
    TitleInput(
        text = text,
        error = error,
        requestKeyboard = requestKeyboard,
        modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST),
        setText = { setText(it) },
    )
    SelectColorRow(
        hasPro = hasPro,
        color = color,
        colors = colors,
        selectColor = { setColor(it) },
        purchase = { purchase() },
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
