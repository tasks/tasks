package org.tasks.compose.pickers

import android.Manifest
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.calendars.AndroidCalendar
import org.tasks.calendars.CalendarPickerViewModel
import org.tasks.compose.collectAsStateLifecycleAware

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
            calendars = viewModel.viewState.collectAsStateLifecycleAware().value.calendars,
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
            icon = painterResource(id = R.drawable.ic_outline_block_24),
            tint = MaterialTheme.colors.onSurface,
            text = stringResource(id = R.string.dont_add_to_calendar),
            selected = selectedCalendar == null,
            onClick = { onSelected(null) },
        )
        calendars.forEach {
            CheckableIconRow(
                icon = painterResource(id = R.drawable.ic_outline_event_24px),
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
    MdcTheme {
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
    MdcTheme {
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