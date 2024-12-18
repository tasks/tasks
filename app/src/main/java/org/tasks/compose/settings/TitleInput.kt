package org.tasks.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.tasks.R

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
}
