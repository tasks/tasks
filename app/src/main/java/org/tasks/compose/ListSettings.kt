package org.tasks.compose

/**
 * Composables for BaseListSettingActivity
*/
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.components.TasksIcon
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme

@Composable
@Preview (showBackground = true)
private fun TitleBarPreview() {
    TasksTheme {
        ListSettings.Toolbar(
            title = "Tollbar title",
            save = { /*TODO*/ }, optionButton = { DeleteButton {} }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun PromptActionPreview() {
    TasksTheme {
        ListSettings.PromptAction(
            showDialog = remember { mutableStateOf(true) },
            title = "Delete list?",
            onAction = { /*TODO*/ })
    }
}

@Composable
@Preview (showBackground = true)
private fun IconSelectPreview () {
    TasksTheme {
        ListSettings.SelectIconRow(
            icon = TasksIcons.FILTER_LIST,
            selectIcon = {}
        )
    }
}

@Composable
@Preview (showBackground = true)
private fun ColorSelectPreview () {
    TasksTheme {
        ListSettings.SelectColorRow(
            color = remember { mutableStateOf(Color.Red) },
            selectColor = {},
            clearColor = {}
        )
    }
}

object ListSettings {

    @Composable
    fun Toolbar(
        title: String,
        save: () -> Unit,
        optionButton: @Composable () -> Unit,
    ) {

/*  Hady: reminder for the future
    val activity = LocalView.current.context as Activity
    activity.window.statusBarColor = colorResource(id = R.color.drawer_color_selected).toArgb()
*/

        Surface(
            shadowElevation = 4.dp,
            color = colorResource(id = R.color.content_background),
            contentColor = colorResource(id = R.color.text_primary),
            modifier = Modifier.requiredHeight(56.dp)
        )
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = save, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                        contentDescription = stringResource(id = R.string.save),
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
    } /* ToolBar */

    @Composable
    fun ProgressBar(showProgress: State<Boolean>) {
        Box(modifier = Modifier.fillMaxWidth().requiredHeight(3.dp))
        {
            if (showProgress.value) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    trackColor = LocalContentColor.current.copy(alpha = 0.3f),  //Color.LightGray,
                    color = colorResource(org.tasks.kmp.R.color.red_a400)
                )
            }
        }
    }

    @Composable
    fun TitleInput(
        text: MutableState<String>,
        error: MutableState<String>,
        requestKeyboard: Boolean,
        modifier: Modifier = Modifier,
        label: String = stringResource(R.string.display_name),
        errorState: Color = MaterialTheme.colorScheme.secondary,
        activeState: Color = LocalContentColor.current.copy(alpha = 0.75f),
        inactiveState: Color = LocalContentColor.current.copy(alpha = 0.3f),
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val requester = remember { FocusRequester() }
        val focused = remember { mutableStateOf(false) }
        val labelColor = when {
            (error.value != "") -> errorState
            (focused.value) -> activeState
            else -> inactiveState
        }
        val dividerColor = if (focused.value) errorState else labelColor
        val labelText = if (error.value != "") error.value else label

        Row (modifier = modifier)
        {
            Column {
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
                    cursorBrush = SolidColor(errorState), // SolidColor(LocalContentColor.current),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                        .focusRequester(requester)
                        .onFocusChanged { focused.value = (it.isFocused) }
                )
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (requestKeyboard) {
            LaunchedEffect(null) {
                requester.requestFocus()
                delay(30) // Workaround. Otherwise keyboard don't show in 4/5 tries
                keyboardController?.show()
            }
        }
    } /* TextInput */

    @Composable
    fun SelectColorRow(color: State<Color>, selectColor: () -> Unit, clearColor: () -> Unit) =
        SettingRow(
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
                        val borderColor = colorResource(R.color.icon_tint_with_alpha)  // colorResource(R.color.text_tertiary)
                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                drawCircle(color = color.value)
                                drawCircle(color = borderColor, style = Stroke(width = 4.0f)
                                )
                            }
                        }
                    }
                }
            },
            center = {
                Text(
                    text = LocalContext.current.getString(R.string.color),
                    modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
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
    fun SelectIconRow(icon: String, selectIcon: () -> Unit) =
        SettingRow(
            modifier = Modifier.clickable(onClick =  selectIcon),
            left = {
                IconButton(onClick = selectIcon) {
                    TasksIcon(
                        label = icon,
                        tint = colorResource(R.color.icon_tint_with_alpha)
                    )
                }
            },
            center = {
                Text(
                    text = LocalContext.current.getString(R.string.icon),
                    modifier = Modifier.padding(start = Constants.KEYLINE_FIRST)
                )
            }
        )


    @Composable
    fun SettingRow(
        left: @Composable () -> Unit,
        center: @Composable () -> Unit,
        right: @Composable (() -> Unit)? = null,
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
    fun SettingsSurface(content: @Composable ColumnScope.() -> Unit) {
        ProvideTextStyle(LocalTextStyle.current.copy(fontSize = 18.sp)) {
            Surface(
                color = colorResource(id = R.color.window_background),
                contentColor = colorResource(id = R.color.text_primary)
            ) {
                Column(modifier = Modifier.fillMaxSize()) { content() }
            }
        }
    }

    @Composable
    fun Toaster(state: SnackbarHostState) {
        SnackbarHost(state) { data ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Snackbar(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(10.dp),
                    containerColor = colorResource(id = R.color.snackbar_background),
                    contentColor = colorResource(id = R.color.snackbar_text_color),
                ) {
                    Text(text = data.visuals.message, fontSize = 18.sp)
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
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                confirmButton = { Constants.TextButton(text = R.string.ok, onClick = onAction) },
                dismissButton = { Constants.TextButton(text = R.string.cancel, onClick = onCancel) }
            )
        }
    }

}
