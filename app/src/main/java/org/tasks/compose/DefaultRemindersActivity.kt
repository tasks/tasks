package org.tasks.compose

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.todoroo.astrid.ui.ReminderControlSetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.toPersistentSet
import org.tasks.R
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.preferences.Preferences
import org.tasks.reminders.AlarmToString
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class DefaultRemindersActivity : AppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var preferences: Preferences

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                val vm: ReminderControlSetViewModel = viewModel()
                val viewState = vm.viewState.collectAsStateWithLifecycle().value
                var alarms by remember {
                    mutableStateOf(preferences.defaultAlarms.toSet())
                }

                BasicAlertDialog(onDismissRequest = { finish() }) {
                    DefaultRemindersList(
                        alarms = alarms,
                        onAlarmClick = { alarm ->
                            vm.setReplace(alarm)
                            vm.showAddAlarm(visible = true)
                        },
                        onAlarmRemove = { alarm ->
                            alarms = alarms - alarm
                            preferences.setDefaultAlarms(alarms.toList())
                        },
                        onAddClick = { vm.showAddAlarm(visible = true) },
                    )
                }

                AddAlarmDialog(
                    viewState = viewState,
                    existingAlarms = alarms.toPersistentSet(),
                    showRandom = false,
                    showDateTimePicker = false,
                    addAlarm = {
                        viewState.replace?.let { old -> alarms = alarms - old }
                        alarms = alarms + it
                        preferences.setDefaultAlarms(alarms.toList())
                    },
                    addRandom = { },
                    addCustom = { vm.showCustomDialog(visible = true) },
                    pickDateAndTime = { },
                    dismiss = { vm.showAddAlarm(visible = false) },
                )

                if (viewState.showCustomDialog) {
                    AddReminderDialog.AddCustomReminderDialog(
                        alarm = viewState.replace,
                        updateAlarm = {
                            viewState.replace?.let { old -> alarms = alarms - old }
                            alarms = alarms + it
                            preferences.setDefaultAlarms(alarms.toList())
                        },
                        closeDialog = { vm.showCustomDialog(visible = false) }
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultRemindersList(
    alarms: Set<Alarm>,
    onAlarmClick: (Alarm) -> Unit,
    onAlarmRemove: (Alarm) -> Unit,
    onAddClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.EPr_default_reminders_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            alarms.forEach { alarm ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlarmClick(alarm) }
                ) {
                    Text(
                        text = AlarmToString(LocalContext.current).toString(alarm),
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .weight(weight = 1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ClearButton(onClick = { onAlarmRemove(alarm) })
                }
            }
            Text(
                text = stringResource(R.string.add_reminder),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = ContentAlpha.disabled
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clickable(onClick = onAddClick)
            )
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
fun DefaultRemindersPreview() {
    TasksTheme {
        Surface {
            DefaultRemindersList(
                alarms = linkedSetOf(
                    Alarm(time = 0, type = Alarm.TYPE_REL_START),
                    Alarm(time = 0, type = Alarm.TYPE_REL_END),
                    whenOverdue(0),
                ),
                onAlarmClick = {},
                onAlarmRemove = {},
                onAddClick = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, widthDp = 320, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultRemindersCustomPreview() {
    TasksTheme {
        DefaultRemindersList(
            alarms = linkedSetOf(
                Alarm(
                    time = -TimeUnit.MINUTES.toMillis(15),
                    type = Alarm.TYPE_REL_START,
                ),
                Alarm(time = 0, type = Alarm.TYPE_REL_END),
                Alarm(
                    time = -TimeUnit.HOURS.toMillis(1),
                    type = Alarm.TYPE_REL_END,
                ),
                Alarm(
                    time = TimeUnit.DAYS.toMillis(1),
                    type = Alarm.TYPE_REL_END,
                    repeat = 6,
                    interval = TimeUnit.DAYS.toMillis(1),
                ),
            ),
            onAlarmClick = {},
            onAlarmRemove = {},
            onAddClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultRemindersEmptyPreview() {
    TasksTheme {
        DefaultRemindersList(
            alarms = emptySet(),
            onAlarmClick = {},
            onAlarmRemove = {},
            onAddClick = {},
        )
    }
}
