package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarPicker.Companion.newCalendarPicker
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForListPickerResult
import org.tasks.compose.settings.DefaultRemindersDialog
import org.tasks.compose.settings.TaskDefaultsContent
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.location.LocationPickerActivity
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.BasePreferences
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.tags.TagPickerActivity
import org.tasks.tags.TagPickerActivity.Companion.EXTRA_SELECTED
import org.tasks.themes.TasksSettingsTheme
import org.tasks.themes.Theme
import javax.inject.Inject

private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"

@AndroidEntryPoint
class TaskDefaults : Fragment() {

    @Inject lateinit var theme: Theme

    private val viewModel: TaskDefaultsHiltViewModel by viewModels()

    private val listPickerLauncher = registerForListPickerResult {
        viewModel.setDefaultList(it)
    }

    private val locationLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val place = result.data?.getParcelableExtra<Place>(EXTRA_PLACE)
            viewModel.setDefaultLocation(place)
        }
    }

    private val tagsLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val tags = result.data?.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
            viewModel.setDefaultTags(tags.orEmpty())
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            CalendarPicker.REQUEST_KEY, this
        ) { _, bundle ->
            viewModel.setDefaultCalendar(bundle.getString(CalendarPicker.EXTRA_CALENDAR_ID))
        }
        parentFragmentManager.setFragmentResultListener(
            BasicRecurrenceDialog.REQUEST_KEY, this
        ) { _, bundle ->
            viewModel.setRecurrence(bundle.getString(BasicRecurrenceDialog.EXTRA_RRULE))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ) = content {
        TasksSettingsTheme(
            theme = theme.themeBase.index,
            primary = theme.themeColor.primaryColor,
        ) {
            var showDefaultReminders by rememberSaveable { mutableStateOf(false) }
            val is24HourFormat = LocalContext.current.is24HourFormat

            TaskDefaultsContent(
                viewModel = viewModel,
                is24HourFormat = is24HourFormat,
                onDefaultList = {
                    listPickerLauncher.launch(
                        context = requireContext(),
                        selectedFilter = viewModel.defaultListFilter,
                        listsOnly = true,
                    )
                },
                onDefaultTags = {
                    lifecycleScope.launch {
                        val intent = Intent(context, TagPickerActivity::class.java)
                            .putParcelableArrayListExtra(
                                EXTRA_SELECTED,
                                ArrayList(viewModel.defaultTags())
                            )
                        tagsLauncher.launch(intent)
                    }
                },
                onCalendar = {
                    newCalendarPicker(viewModel.settings.defaultCalendar)
                        .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
                },
                onRecurrence = {
                    BasicRecurrenceDialog
                        .newBasicRecurrenceDialog(
                            rrule = viewModel.settings.defaultRecurrence,
                            dueDate = 0,
                            accountType = CaldavAccount.TYPE_LOCAL
                        )
                        .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
                },
                onReminders = { showDefaultReminders = true },
                onLocation = {
                    locationLauncher.launch(
                        Intent(context, LocationPickerActivity::class.java),
                    )
                },
                bottomInsets = {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                },
            )

            if (showDefaultReminders && viewModel.loaded) {
                DefaultRemindersDialog(
                    vm = viewModel(),
                    initialAlarms = viewModel.settings.defaultAlarms,
                    is24HourFormat = is24HourFormat,
                    onAlarmsChanged = { viewModel.setDefaultAlarms(it) },
                    onDismiss = { showDefaultReminders = false },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        val surfaceColor = theme.themeBase.getSettingsSurfaceColor(requireActivity())
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(surfaceColor)
            (toolbar.parent as? View)?.setBackgroundColor(surfaceColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.content_background)
        (activity as? BasePreferences)?.toolbar?.let { toolbar ->
            toolbar.setBackgroundColor(defaultColor)
            (toolbar.parent as? View)?.setBackgroundColor(defaultColor)
        }
    }

    companion object {
        const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
    }
}
