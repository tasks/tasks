package org.tasks.calendars

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.compose.collectAsStateLifecycleAware
import org.tasks.dialogs.DialogBuilder
import javax.inject.Inject

@AndroidEntryPoint
class CalendarPicker : DialogFragment() {
    private val viewModel: CalendarPickerViewModel by viewModels()

    @Inject lateinit var dialogBuilder: DialogBuilder

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setContent {
                val hasPermissions = rememberMultiplePermissionsState(
                    permissions = listOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR),
                    onPermissionsResult = { result ->
                        if (result.values.all { it }) {
                            viewModel.loadCalendars()
                        }
                    }
                )
                if (hasPermissions.allPermissionsGranted) {
                    CalendarList(
                        calendars = viewModel.viewState.collectAsStateLifecycleAware().value.calendars,
                        selected = arguments?.getString(EXTRA_SELECTED),
                        onClick = { selectEntry(it) },
                    )
                }
                LaunchedEffect(hasPermissions) {
                    if (!hasPermissions.allPermissionsGranted) {
                        hasPermissions.launchMultiplePermissionRequest()
                    }
                }
            }
            .show()
    }

    @Composable
    fun CalendarList(
        calendars: List<AndroidCalendar>,
        selected: String?,
        onClick: (AndroidCalendar?) -> Unit,
    ) {
        MdcTheme {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                CalendarRow(
                    icon = Icons.Outlined.Block,
                    tint = MaterialTheme.colors.onSurface,
                    text = stringResource(id = R.string.dont_add_to_calendar),
                    selected = selected.isNullOrBlank(),
                    onClick = { onClick(null) },
                )
                calendars.forEach {
                    CalendarRow(
                        icon = Icons.Outlined.Event,
                        tint = Color(it.color),
                        text = it.name,
                        selected = selected == it.name,
                        onClick = { onClick(it) }
                    )
                }
            }
        }
    }

    @Composable
    fun CalendarRow(
        icon: ImageVector,
        tint: Color,
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = ContentAlpha.medium),
                modifier = Modifier.padding(start = 16.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    private fun selectEntry(calendar: AndroidCalendar?) {
        val data = Intent()
        data.putExtra(EXTRA_CALENDAR_ID, calendar?.id)
        data.putExtra(EXTRA_CALENDAR_NAME, calendar?.name)
        targetFragment!!.onActivityResult(targetRequestCode, Activity.RESULT_OK, data)
        dismiss()
    }

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val EXTRA_CALENDAR_NAME = "extra_calendar_name"
        private const val EXTRA_SELECTED = "extra_selected"

        fun newCalendarPicker(target: Fragment?, rc: Int, selected: String?): CalendarPicker {
            val arguments = Bundle()
            arguments.putString(EXTRA_SELECTED, selected)
            val fragment = CalendarPicker()
            fragment.arguments = arguments
            fragment.setTargetFragment(target, rc)
            return fragment
        }
    }
}