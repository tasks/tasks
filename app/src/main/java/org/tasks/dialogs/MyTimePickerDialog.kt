package org.tasks.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.remember
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.TimePickerDialog
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import org.tasks.time.withMillisOfDay
import javax.inject.Inject

@AndroidEntryPoint
class MyTimePickerDialog : DialogFragment() {

    @Inject lateinit var preferences: Preferences
    @Inject lateinit var theme: Theme

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        TasksTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            TimePickerDialog(
                state = rememberTimePickerState(
                    initialHour = initial.millisOfDay / (60 * 60_000),
                    initialMinute = (initial.millisOfDay / (60_000)) % 60,
                    is24Hour = requireContext().is24HourFormat
                ),
                initialDisplayMode = remember { preferences.timeDisplayMode },
                setDisplayMode = { preferences.timeDisplayMode = it },
                selected = {
                    targetFragment?.onActivityResult(
                        targetRequestCode,
                        Activity.RESULT_OK,
                        Intent().putExtra(EXTRA_TIMESTAMP, initial.withMillisOfDay(it))
                    )
                    dismiss()
                },
                dismiss = { dismiss() },
            )
        }
    }

    private val initial: Long
        get() = arguments?.getLong(EXTRA_TIMESTAMP) ?: currentTimeMillis().startOfDay()

    companion object {
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        fun newTimePicker(
            target: Fragment,
            rc: Int,
            initial: Long,
        ) =
            MyTimePickerDialog().apply {
                arguments = Bundle().apply {
                    putLong(EXTRA_TIMESTAMP, initial)
                }
                setTargetFragment(target, rc)
            }
    }
}
