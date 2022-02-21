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
import androidx.compose.material.AlertDialog
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
import org.tasks.data.Alarm
import java.util.concurrent.TimeUnit

@ExperimentalComposeUiApi
object AddReminderDialog {
    @Composable
    fun AddRandomReminderDialog(
        openDialog: MutableState<Boolean>,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val interval = rememberSaveable { mutableStateOf(15L as Long?) }
        val multiplier = rememberSaveable { mutableStateOf(0) }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = { AddRandomReminder(openDialog, interval, multiplier) },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        interval.value?.let { i ->
                            addAlarm(Alarm(0, i * multiplier.millis, Alarm.TYPE_RANDOM))
                            closeDialog()
                        }
                    })
                },
                dismissButton = {
                    Constants.TextButton(
                        text = R.string.cancel,
                        onClick = closeDialog
                    )
                },
            )
        } else {
            interval.value = 15
            multiplier.value = 0
        }
    }

    @Composable
    fun AddCustomReminderDialog(
        openDialog: MutableState<Boolean>,
        addAlarm: (Alarm) -> Unit,
        closeDialog: () -> Unit,
    ) {
        val interval = rememberSaveable { mutableStateOf(15L as Long?) }
        val multiplier = rememberSaveable { mutableStateOf(0) }
        if (openDialog.value) {
            AlertDialog(
                onDismissRequest = closeDialog,
                text = { AddCustomReminder(openDialog, interval, multiplier) },
                confirmButton = {
                    Constants.TextButton(text = R.string.ok, onClick = {
                        interval.value?.let { i ->
                            addAlarm(Alarm(0, -1 * i * multiplier.millis, Alarm.TYPE_REL_END))
                            closeDialog()
                        }
                    })
                },
                dismissButton = {
                    Constants.TextButton(
                        text = R.string.cancel,
                        onClick = closeDialog
                    )
                },
            )
        } else {
            interval.value = 15
            multiplier.value = 0
        }
    }

    @Composable
    fun AddRandomReminder(
        visible: MutableState<Boolean>,
        interval: MutableState<Long?>,
        selected: MutableState<Int>
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            CenteredH6(text = stringResource(id = R.string.randomly_every, "").trim())
            val focusRequester = remember { FocusRequester() }
            OutlinedLongInput(interval, focusRequester)
            Spacer(modifier = Modifier.height(16.dp))
            options.forEachIndexed { index, option ->
                RadioRow(index, option, interval, selected)
            }
            ShowKeyboard(visible, focusRequester)
        }
    }

    @Composable
    fun AddCustomReminder(
        visible: MutableState<Boolean>,
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
            options.forEachIndexed { index, option ->
                RadioRow(index, option, interval, selected, R.string.alarm_before_due)
            }
            ShowKeyboard(visible, focusRequester)
        }
    }

    private val options = listOf(
        R.plurals.reminder_minutes,
        R.plurals.reminder_hours,
        R.plurals.reminder_days,
        R.plurals.reminder_week,
    )

    private val MutableState<Int>.millis: Long
        get() = when (value) {
            1 -> TimeUnit.HOURS.toMillis(1)
            2 -> TimeUnit.DAYS.toMillis(1)
            3 -> TimeUnit.DAYS.toMillis(7)
            else -> TimeUnit.MINUTES.toMillis(1)
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
    CenteredH6(text = stringResource(id = resId))
}

@Composable
fun CenteredH6(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.h6
    )
}

@Composable
fun RadioRow(
    index: Int,
    option: Int,
    interval: MutableState<Long?>,
    selected: MutableState<Int>,
    formatString: Int? = null,
) {
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
                formatString
                    ?.let { stringResource(id = formatString, optionString) }
                    ?: optionString

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
fun AddCustomReminderOne() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddCustomReminder(
            visible = remember { mutableStateOf(true) },
            interval = remember { mutableStateOf(1L) },
            selected = remember { mutableStateOf(0) }
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddCustomReminder() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddCustomReminder(
            visible = remember { mutableStateOf(true) },
            interval = remember { mutableStateOf(15L) },
            selected = remember { mutableStateOf(1) }
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminderOne() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddRandomReminder(
            visible = remember { mutableStateOf(true) },
            interval = remember { mutableStateOf(1L) },
            selected = remember { mutableStateOf(0) }
        )
    }

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRandomReminder() =
    MaterialTheme(if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        AddReminderDialog.AddRandomReminder(
            visible = remember { mutableStateOf(true) },
            interval = remember { mutableStateOf(15L) },
            selected = remember { mutableStateOf(1) }
        )
    }