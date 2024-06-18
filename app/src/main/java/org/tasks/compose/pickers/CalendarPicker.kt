package org.tasks.compose.pickers

import android.Manifest
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.tasks.R
import org.tasks.calendars.AndroidCalendar
import org.tasks.calendars.CalendarPickerViewModel
import org.tasks.themes.TasksTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarPicker(
    viewModel: CalendarPickerViewModel = viewModel(),
    selected: String?,
    onSelected: (AndroidCalendar?) -> Unit,
) {
    val hasPermissions = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
        onPermissionsResult = { result ->
            if (result.values.all { it }) {
                viewModel.loadCalendars()
            }
        }
    )
    if (hasPermissions.allPermissionsGranted) {
        CalendarPickerList(
            calendars = viewModel.viewState.collectAsStateWithLifecycle().value.calendars,
            selected = selected,
            onSelected = onSelected,
        )
    }
    LaunchedEffect(hasPermissions) {
        if (!hasPermissions.allPermissionsGranted) {
            hasPermissions.launchMultiplePermissionRequest()
        }
    }
}

@Composable
fun CalendarPickerList(
    calendars: List<AndroidCalendar>,
    selected: String?,
    onSelected: (AndroidCalendar?) -> Unit,
) {
    val selectedCalendar = calendars.find { it.id == selected }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        CheckableIconRow(
            icon = Icons.Outlined.Block,
            tint = MaterialTheme.colorScheme.onSurface,
            text = stringResource(id = R.string.dont_add_to_calendar),
            selected = selectedCalendar == null,
            onClick = { onSelected(null) },
        )
        calendars.forEach {
            CheckableIconRow(
                icon = Icons.Outlined.Event,
                tint = Color(it.color),
                text = it.name,
                selected = selectedCalendar == it,
                onClick = { onSelected(it) }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun CalendarPickerPreview() {
    TasksTheme {
        CalendarPickerList(
            calendars = listOf(
                AndroidCalendar("1", "Home", -765666),
                AndroidCalendar("2", "Work", -5434281),
                AndroidCalendar("3", "Personal", -10395295),
            ),
            selected = "2",
            onSelected = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun CalendarPickerNoneSelected() {
    TasksTheme {
        CalendarPickerList(
            calendars = listOf(
                AndroidCalendar("1", "Home", -765666),
                AndroidCalendar("2", "Work", -5434281),
                AndroidCalendar("3", "Personal", -10395295),
            ),
            selected = null,
            onSelected = {},
        )
    }
}