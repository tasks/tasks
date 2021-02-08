package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.activities.ListPicker
import org.tasks.calendars.CalendarPicker
import org.tasks.calendars.CalendarPicker.newCalendarPicker
import org.tasks.calendars.CalendarProvider
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.location.LocationPickerActivity
import org.tasks.location.LocationPickerActivity.Companion.EXTRA_PLACE
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.IconPreference
import org.tasks.preferences.Preferences
import org.tasks.repeats.BasicRecurrenceDialog
import org.tasks.repeats.BasicRecurrenceDialog.EXTRA_RRULE
import org.tasks.repeats.RepeatRuleToString
import org.tasks.tags.TagPickerActivity
import org.tasks.tags.TagPickerActivity.Companion.EXTRA_SELECTED
import javax.inject.Inject

private const val FRAG_TAG_DEFAULT_LIST_SELECTION = "frag_tag_default_list_selection"
private const val FRAG_TAG_CALENDAR_PICKER = "frag_tag_calendar_picker"
private const val REQUEST_DEFAULT_LIST = 10010
private const val REQUEST_CALENDAR_SELECTION = 10011

@AndroidEntryPoint
class TaskDefaults : InjectingPreferenceFragment() {

    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var calendarProvider: CalendarProvider
    @Inject lateinit var repeatRuleToString: RepeatRuleToString
    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var tagDataDao: TagDataDao

    private lateinit var defaultCalendarPref: Preference

    override fun getPreferenceXml() = R.xml.preferences_task_defaults

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        defaultCalendarPref = findPreference(R.string.gcal_p_default)
        defaultCalendarPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            newCalendarPicker(this, REQUEST_CALENDAR_SELECTION, getDefaultCalendarName())
                .show(parentFragmentManager, FRAG_TAG_CALENDAR_PICKER)
            false
        }
        val defaultCalendarName: String? = getDefaultCalendarName()
        defaultCalendarPref.summary = defaultCalendarName
            ?: getString(R.string.dont_add_to_calendar)

        findPreference(R.string.p_default_list)
            .setOnPreferenceClickListener {
                lifecycleScope.launch {
                    ListPicker.newListPicker(
                            defaultFilterProvider.getDefaultList(),
                            this@TaskDefaults,
                            REQUEST_DEFAULT_LIST)
                            .show(parentFragmentManager, FRAG_TAG_DEFAULT_LIST_SELECTION)
                }
                false
            }

        findPreference(R.string.p_default_recurrence)
                .setOnPreferenceClickListener {
                    val rrule = preferences
                            .getStringValue(R.string.p_default_recurrence)
                            ?.takeIf { it.isNotBlank() }
                    BasicRecurrenceDialog
                            .newBasicRecurrenceDialog(this, REQUEST_RECURRENCE, rrule, -1)
                            .show(parentFragmentManager, FRAG_TAG_BASIC_RECURRENCE)
                    false
                }

        findPreference(R.string.p_default_location)
                .setOnPreferenceClickListener {
                    startActivityForResult(
                            Intent(context, LocationPickerActivity::class.java),
                            REQUEST_LOCATION
                    )
                    false
                }

        findPreference(R.string.p_default_tags)
                .setOnPreferenceClickListener {
                    lifecycleScope.launch {
                        val intent = Intent(context, TagPickerActivity::class.java)
                                .putParcelableArrayListExtra(
                                        EXTRA_SELECTED,
                                        ArrayList(defaultTags())
                                )
                        startActivityForResult(intent, REQUEST_TAGS)
                    }
                    false
                }

        updateRemoteListSummary()
        updateRecurrence()
        updateDefaultLocation()
        updateTags()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_DEFAULT_LIST -> {
                val list: Filter? = data!!.getParcelableExtra(ListPicker.EXTRA_SELECTED_FILTER)
                if (list is GtasksFilter || list is CaldavFilter) {
                    defaultFilterProvider.defaultList = list
                } else {
                    throw RuntimeException("Unhandled filter type")
                }
                updateRemoteListSummary()
            }
            REQUEST_CALENDAR_SELECTION -> if (resultCode == RESULT_OK) {
                preferences.setString(
                        R.string.gcal_p_default,
                        data!!.getStringExtra(CalendarPicker.EXTRA_CALENDAR_ID)
                )
                defaultCalendarPref.summary =
                        data.getStringExtra(CalendarPicker.EXTRA_CALENDAR_NAME)
            }
            REQUEST_RECURRENCE -> if (resultCode == RESULT_OK) {
                preferences.setString(
                        R.string.p_default_recurrence,
                        data?.getStringExtra(EXTRA_RRULE)
                )
                updateRecurrence()
            }
            REQUEST_LOCATION ->
                if (resultCode == RESULT_OK)
                    setDefaultLocation(data?.getParcelableExtra(EXTRA_PLACE))
            REQUEST_TAGS -> if (resultCode == RESULT_OK) {
                preferences.setString(
                        R.string.p_default_tags,
                        data?.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
                                ?.mapNotNull { it.remoteId }
                                ?.joinToString(",")
                )
                updateTags()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setDefaultLocation(place: Place?) {
        preferences.setString(R.string.p_default_location, place?.uid)
        updateDefaultLocation()
    }

    override fun onResume() {
        super.onResume()

        updateRemoteListSummary()
        updateRecurrence()
        updateDefaultLocation()
        updateTags()
    }

    private fun getDefaultCalendarName(): String? {
        val calendarId = preferences.getStringValue(R.string.gcal_p_default)
        return calendarProvider.getCalendar(calendarId)?.name
    }

    private fun updateRemoteListSummary() = lifecycleScope.launch {
        val defaultFilter = defaultFilterProvider.getDefaultList()
        findPreference(R.string.p_default_list).summary = defaultFilter.listingTitle
    }

    private fun updateDefaultLocation() = lifecycleScope.launch {
        val place = preferences
                .getStringValue(R.string.p_default_location)
                ?.let { locationDao.getByUid(it) }
        val defaultLocation = findPreference(R.string.p_default_location) as IconPreference
        if (place == null) {
            defaultLocation.iconVisible = false
            defaultLocation.summary = requireContext().getString(R.string.none)
        } else {
            defaultLocation.drawable =
                    context?.getDrawable(R.drawable.ic_outline_delete_24px)?.mutate()
            defaultLocation.tint = context?.getColor(R.color.icon_tint_with_alpha)
            defaultLocation.iconClickListener = View.OnClickListener { setDefaultLocation(null) }
            defaultLocation.iconVisible = true
            defaultLocation.summary = place.displayName
        }
    }

    private suspend fun defaultTags(): List<TagData> =
            preferences.getStringValue(R.string.p_default_tags)
                    ?.split(",")
                    ?.let { tagDataDao.getByUuid(it) }
                    ?.sortedBy { it.name }
                    ?: emptyList()

    private fun updateTags() = lifecycleScope.launch {
        findPreference(R.string.p_default_tags).summary =
                defaultTags()
                        .mapNotNull { it.name }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?: requireContext().getString(R.string.none)
    }

    private fun updateRecurrence() {
        val rrule = preferences.getStringValue(R.string.p_default_recurrence)
        findPreference(R.string.p_default_recurrence).summary =
                rrule
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            try {
                                repeatRuleToString.toString(it)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        ?: requireContext().getString(R.string.repeat_option_does_not_repeat)
        findPreference(R.string.p_default_recurrence_from).isVisible = rrule?.isNotBlank() == true
    }

    companion object {
        const val REQUEST_RECURRENCE = 10000
        const val REQUEST_LOCATION = 10001
        const val REQUEST_TAGS = 10002
        const val FRAG_TAG_BASIC_RECURRENCE = "frag_tag_basic_recurrence"
    }
}