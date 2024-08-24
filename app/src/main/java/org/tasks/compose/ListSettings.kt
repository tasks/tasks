package org.tasks.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.components.TasksIcon

object ListSettings {
    @Composable
    fun ListSettings(
        title: String,
        requestKeyboard: Boolean,
        text: MutableState<String>,
        error: MutableState<String>,
        color: State<Color>,
        icon: State<String>,
        save: () -> Unit = {},
        selectIcon: () -> Unit = {},
        clearColor: () -> Unit = {},
        selectColor: () -> Unit = {},
        optionButton: @Composable () -> Unit,
        /** right button on toolbar, mostly, DeleteButton */
        showProgress: State<Boolean> = remember { mutableStateOf(false) },
        extensionContent: @Composable ColumnScope.() -> Unit = {}
    ) {
        ListSettingsSurface {

            ListSettingsToolbar(
                title = title,
                save = save,
                optionButton = optionButton
            )

            ListSettingsProgressBar(showProgress)

            ListSettingsTitleInput(
                text = text, error = error, requestKeyboard = requestKeyboard,
                modifier = Modifier.padding(horizontal = Constants.KEYLINE_FIRST)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                SelectColorRow(color = color, selectColor = selectColor, clearColor = clearColor)
                SelectIconRow(icon = icon, selectIcon = selectIcon)
                extensionContent()
            }
        }
    }

    @Composable
    fun ListSettingsToolbar(
        title: String,
        save: () -> Unit,
        optionButton: @Composable () -> Unit,
    ) {

        /*
    val activity = LocalView.current.context as Activity
    activity.window.statusBarColor = colorResource(id = R.color.drawer_color_selected).toArgb()
*/

        Surface(
            elevation = 4.dp,
            color = colorResource(id = org.tasks.R.color.content_background),
            contentColor = colorResource(id = R.color.text_primary),
            modifier = Modifier.requiredHeight(56.dp)
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = save) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                        contentDescription = stringResource(id = R.string.save),
                        modifier = Modifier.padding(Constants.KEYLINE_FIRST)
                    )
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(start = Constants.KEYLINE_FIRST)
                )
                optionButton()
            }
        }
    } /* ListSettingsToolBar */

    @Composable
    fun ListSettingsProgressBar(showProgress: State<Boolean>) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(3.dp)
        ) {
            if (showProgress.value) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = LocalContentColor.current.copy(alpha = 0.3f),  //Color.LightGray,
                    color = colorResource(org.tasks.kmp.R.color.red_a400)
                )
            }
        }
    }

    @Composable
    fun ListSettingsTitleInput(
        text: MutableState<String>,
        error: MutableState<String>,
        requestKeyboard: Boolean,
        modifier: Modifier = Modifier,
        label: String = stringResource(R.string.display_name),
        errorState: Color = MaterialTheme.colors.secondary,
        activeState: Color = LocalContentColor.current.copy(alpha = 0.75f),
        inactiveState: Color = LocalContentColor.current.copy(alpha = 0.5f),
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val requester = remember { FocusRequester() }

        Row(
            modifier = modifier
        ) {
            Column {
                val focused = remember { mutableStateOf(false) }
                val labelColor = when {
                    (error.value != "") -> errorState
                    (focused.value) -> activeState
                    else -> inactiveState
                }
                val labelText = if (error.value != "") error.value else label
                Text(
                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                    text = labelText,
                    fontSize = 12.sp,
                    letterSpacing = 0.sp,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )

                BasicTextField(
                    value = text.value,
                    textStyle = TextStyle(
                        fontSize = LocalTextStyle.current.fontSize,
                        color = LocalContentColor.current
                    ),
                    onValueChange = {
                        text.value = it
                        if (error.value != "") error.value = ""
                    },
                    cursorBrush = SolidColor(LocalContentColor.current),
                    modifier = Modifier
                        .padding(bottom = 3.dp)
                        .focusRequester(requester)
                        .onFocusChanged { focused.value = (it.isFocused) }
                )
                Divider(
                    color = labelColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (requestKeyboard) {
            LaunchedEffect(null) {
                requester.requestFocus()

                /* Part of requester.requestFocus logic is performed in separate coroutine,
            so the actual view may not be really focused right upon return
            from it, what makes the subsequent "IME.show" call to be ignored by the system.
            The delay below is a workaround trick for it.
            30ms period is not the guarantee but makes it working almost always */
                delay(30)

                keyboardController?.show()
            }
        }
    } /* TextInput */

    @Composable
    fun SelectColorRow(color: State<Color>, selectColor: () -> Unit, clearColor: () -> Unit) =
        ListSettingRow(
            modifier = Modifier.clickable(onClick =  selectColor),
            left = {
                IconButton(onClick = { selectColor() }) {
                    if (color.value == Color.Unspecified) {
                        Icon(
                            modifier = Modifier.padding(Constants.KEYLINE_FIRST),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_not_interested_24px),
                            tint = colorResource(R.color.icon_tint_with_alpha),
                            contentDescription = null
                        )
                    } else {
                        val borderColor =
                            colorResource(R.color.icon_tint_with_alpha)  // colorResource(R.color.text_tertiary)
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                drawCircle(color = color.value)
                                drawCircle(
                                    color = borderColor, style = Stroke(width = 4.0f)
                                )
                            }
                        }
                    }
                }
            },
            center = {
                Text(
                    text = LocalContext.current.getString(R.string.color),
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(start = Constants.KEYLINE_FIRST)
                )
            },
            right = {
                if (color.value != Color.Unspecified) {
                    IconButton(onClick = clearColor) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_clear_24px),
                            contentDescription = null
                        )
                    }
                }
            }
        )

    @Composable
    fun SelectIconRow(icon: State<String>, selectIcon: () -> Unit) =
        ListSettingRow(
            modifier = Modifier.clickable(onClick =  selectIcon),
            left = {
                IconButton(onClick = selectIcon) {
                    TasksIcon(
                        label = icon.value,
                        modifier = Modifier.padding(Constants.KEYLINE_FIRST),
                        tint = colorResource(R.color.icon_tint_with_alpha)
                    )
                }
            },
            center = {
                Text(
                    text = LocalContext.current.getString(R.string.icon),
                    modifier = Modifier
                        .weight(0.8f)
                        .padding(start = Constants.KEYLINE_FIRST)
                )
            }
        )


    @Composable
    fun ListSettingRow(
        left: @Composable RowScope.() -> Unit,
        center: @Composable RowScope.() -> Unit,
        right: @Composable (RowScope.() -> Unit)? = null,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.requiredHeight(56.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            left()
            center()
            right?.invoke(this)
        }
    }


    @Composable
    fun ListSettingsSurface(content: @Composable ColumnScope.() -> Unit) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 18.sp)) {
            Surface(
                color = colorResource(id = R.color.window_background),
                contentColor = colorResource(id = R.color.text_primary)
            ) {
                Column(modifier = Modifier.fillMaxSize())
                { content() }
            }
        }
    }

    @Composable
    fun ListSettingsSnackBar(state: SnackbarHostState) {
        SnackbarHost(state) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Snackbar(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = colorResource(id = R.color.snackbar_background),
                    contentColor = colorResource(id = R.color.snackbar_text_color),
                    elevation = 8.dp
                ) {
                    Text(text = it.message, fontSize = 18.sp)
                }
            }
        }
    }

    @Composable
    fun PromptAction(
        showDialog: MutableState<Boolean>,
        title: String,
        onAction: () -> Unit,
        onCancel: () -> Unit = { showDialog.value = false }
    ) {
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = onCancel,
                title = { Text(title, style = MaterialTheme.typography.h6) },
                confirmButton = { Constants.TextButton(R.string.ok, onClick = onAction) },
                dismissButton = { Constants.TextButton(text = R.string.cancel, onCancel) }
            )
        }
    }

}
