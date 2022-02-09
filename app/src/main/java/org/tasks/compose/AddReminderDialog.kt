package org.tasks.compose

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.android.awaitFrame
import org.tasks.R

@ExperimentalComposeUiApi
object AddReminderDialog {
    @Composable
    fun AddReminderDialog(
        visible: MutableState<Boolean> = mutableStateOf(true),
        interval: MutableState<Long?>,
        selected: MutableState<Int>,
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(resId = R.string.custom_notification)
            val focusRequester = remember { FocusRequester() }
            OutlinedLongInput(interval, focusRequester)
            Spacer(modifier = Modifier.height(16.dp))
            val options = listOf(
                R.plurals.reminder_minutes,
                R.plurals.reminder_hours,
                R.plurals.reminder_days,
                R.plurals.reminder_week,
            )
            options.forEachIndexed { index, option ->
                RadioRow(index, option, interval, selected)
            }
            ShowKeyboard(visible, focusRequester)
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun ShowKeyboard(visible: MutableState<Boolean>, focusRequester: FocusRequester) {
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(visible) {
        focusRequester.freeFocus()
        awaitFrame()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
fun OutlinedLongInput(
    interval: MutableState<Long?>,
    focusRequester: FocusRequester
) {
    val value = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = interval.value.toString()
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(0, text.length)))
    }
    OutlinedTextField(
        value = value.value,
        onValueChange = {
            value.value = it.copy(text = it.text.filter { t -> t.isDigit() })
            interval.value = value.value.text.toLongOrNull()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .focusRequester(focusRequester),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = MaterialTheme.colors.onSurface,
            focusedBorderColor = MaterialTheme.colors.onSurface
        ),
        isError = value.value.text.toLongOrNull() == null,
    )
}

@Composable
fun CenteredH6(@StringRes resId: Int) {
    Text(
        text = stringResource(id = resId),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h6
    )
}

@Composable
fun RadioRow(index: Int, option: Int, interval: MutableState<Long?>, selected: MutableState<Int>) {
    val number = interval.value?.toInt() ?: 1
    val optionString = LocalContext.current.resources.getQuantityString(option, number)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                selected.value = index
            }
    ) {
        RadioButton(
            selected = index == selected.value,
            onClick = {
                selected.value = index
            },
            modifier = Modifier
                .padding(16.dp, 8.dp)
                .align(CenterVertically)
        )
        Text(
            text = if (index == selected.value) {
                stringResource(id = R.string.alarm_before_due, optionString)
            } else {
                optionString
            },
            modifier = Modifier.align(CenterVertically),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1,
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddReminderOne() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddReminderDialog(
            interval = mutableStateOf(1L),
            selected = mutableStateOf(0)
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddReminderMultiple() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddReminderDialog(
            interval = mutableStateOf(15L),
            selected = mutableStateOf(1)
        )
    }