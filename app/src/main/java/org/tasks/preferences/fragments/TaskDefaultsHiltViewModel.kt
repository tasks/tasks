package org.tasks.preferences.fragments

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.PlatformConfiguration
import org.tasks.R
import org.tasks.calendars.CalendarProvider
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.injection.ApplicationScope
import org.tasks.location.LocationService
import org.tasks.preferences.Preferences
import org.tasks.repeats.RepeatRuleToString
import org.tasks.viewmodel.TaskDefaultsViewModel
import javax.inject.Inject

@HiltViewModel
class TaskDefaultsHiltViewModel @Inject constructor(
    @ApplicationContext context: Context,
    preferences: Preferences,
    platformConfiguration: PlatformConfiguration,
    @ApplicationScope private val persistenceScope: CoroutineScope,
    caldavDao: CaldavDao,
    tagDataDao: TagDataDao,
    locationDao: LocationDao,
    repeatRuleToString: RepeatRuleToString,
    calendarProvider: CalendarProvider,
    private val locationService: dagger.Lazy<LocationService>,
) : TaskDefaultsViewModel(
    appPreferences = preferences,
    platformConfiguration = platformConfiguration,
    persistenceScope = persistenceScope,
    caldavDao = caldavDao,
    tagDataDao = tagDataDao,
    locationDao = locationDao,
    repeatRuleToString = repeatRuleToString,
    calendarNamer = { calendarId ->
        withContext(Dispatchers.IO) {
            calendarProvider.getCalendar(calendarId)?.name
                ?: context.getString(R.string.dont_add_to_calendar)
        }
    },
) {

    override suspend fun onLocationUpdateIntervalChanged() {
        locationService.get().refreshBackgroundLocationUpdates()
    }
}
