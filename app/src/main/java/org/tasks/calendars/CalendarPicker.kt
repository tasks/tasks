package org.tasks.calendars

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.compose.pickers.CalendarPicker
import org.tasks.dialogs.DialogBuilder
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class CalendarPicker : DialogFragment() {
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var theme: Theme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return dialogBuilder
            .newDialog()
            .setContent {
                TasksTheme(
                    theme = theme.themeBase.index,
                    primary = theme.themeColor.primaryColor,
                ) {
                    CalendarPicker(
                        selected = arguments?.getString(EXTRA_SELECTED),
                        onSelected = {
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(EXTRA_CALENDAR_ID to it?.id)
                            )
                            dismiss()
                        },
                    )
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_CALENDAR_ID = "extra_calendar_id"
        const val REQUEST_KEY = "calendar_picker_result"
        private const val EXTRA_SELECTED = "extra_selected"

        fun newCalendarPicker(selected: String?) =
            CalendarPicker().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_SELECTED, selected)
                }
            }
    }
}
