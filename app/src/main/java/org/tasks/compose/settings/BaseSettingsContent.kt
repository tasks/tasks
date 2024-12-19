package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.compose.Constants

@Composable
fun BaseSettingsContent(
    title: String,
    color: Color,
    icon: String,
    text: String,
    error: String,
    requestKeyboard: Boolean,
    promptDiscard: Boolean,
    showProgress: Boolean,
    dismissDiscardPrompt: () -> Unit,
    setText: (String) -> Unit,
    save: () -> Unit,
    pickColor: () -> Unit,
    clearColor: () -> Unit,
    pickIcon: () -> Unit,
    discard: () -> Unit,
    optionButton: @Composable () -> Unit,
    extensionContent: @Composable ColumnScope.() -> Unit,
) {
    SettingsSurface {
        Toolbar(
            title = title,
            save = { save() },
            optionButton = optionButton
        )
        ProgressBar(showProgress)
        TitleInput(
            text = text,
            error = error,
            requestKeyboard = requestKeyboard,
            modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST),
            setText = { setText(it) },
        )
        Column(modifier = Modifier.fillMaxWidth()) {
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

            PromptAction(
                showDialog = promptDiscard,
                title = stringResource(id = R.string.discard_changes),
                onAction = { discard() },
                onCancel = { dismissDiscardPrompt() },
            )
        }
    }
}
