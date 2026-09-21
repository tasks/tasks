package org.tasks.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.tasks.PlatformConfiguration
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.getOrCreateDefaultListFilter
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.TaskDefaultSettings
import org.tasks.preferences.alarmOrder
import org.tasks.repeats.RepeatRuleToString
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.dont_add_to_calendar
import tasks.kmp.generated.resources.none
import tasks.kmp.generated.resources.repeat_option_does_not_repeat

open class TaskDefaultsViewModel(
    private val appPreferences: AppPreferences,
    platformConfiguration: PlatformConfiguration,
    private val persistenceScope: CoroutineScope,
    caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val locationDao: LocationDao,
    private val repeatRuleToString: RepeatRuleToString,
    private val listResolver: suspend (String?) -> CaldavFilter? = {
        caldavDao.getOrCreateDefaultListFilter(it)
    },
    private val calendarNamer: suspend (String?) -> String = {
        getString(Res.string.dont_add_to_calendar)
    },
) : ViewModel() {

    enum class ListPickerTarget {
        PRIORITY,
        START_DATE,
        DUE_DATE,
        REPEAT_FROM,
        RING_MODE,
        LOCATION_REMINDER,
        LOCATION_UPDATE_INTERVAL,
    }

    val showCalendar: Boolean = platformConfiguration.supportsCalendarEvents
    val showRingMode: Boolean = platformConfiguration.supportsRingMode
    val showLocation: Boolean = platformConfiguration.supportsGeofences

    var settings by mutableStateOf(TaskDefaultSettings())
        private set

    var loaded by mutableStateOf(false)
        private set

    var defaultListFilter by mutableStateOf<CaldavFilter?>(null)
        private set

    val defaultListName: String
        get() = defaultListFilter?.title ?: noneLabel

    var defaultTagsSummary by mutableStateOf("")
        private set

    var recurrenceSummary by mutableStateOf("")
        private set

    var locationName by mutableStateOf("")
        private set

    var hasDefaultLocation by mutableStateOf(false)
        private set

    var calendarName by mutableStateOf("")
        private set

    var listPickerTarget by mutableStateOf<ListPickerTarget?>(null)
        private set

    private var noneLabel by mutableStateOf("")

    private val writes = PreferenceWriteQueue(
        viewModelScope = viewModelScope,
        persistenceScope = persistenceScope,
        tag = TAG,
        reload = { reloadSafely() },
    )

    private var mutationSequence = 0

    private var reloadSequence = 0
    private var appliedReload = 0

    private var tagsSummaryJob: Job? = null
    private var recurrenceSummaryJob: Job? = null
    private var calendarNameJob: Job? = null
    private var locationNameJob: Job? = null

    private suspend fun reload() {
        val generation = mutationSequence
        val reloadGeneration = ++reloadSequence
        val loadedSettings = appPreferences.taskDefaults()
        coroutineScope {
            val list = async { resolve("default list") { listResolver(loadedSettings.defaultList) } }
            val tags = async { resolve("default tags") { describeTags(loadedSettings.defaultTags) } }
            val recurrence = async {
                resolve("default recurrence") { describeRecurrence(loadedSettings.defaultRecurrence) }
            }
            val calendar = async {
                resolve("default calendar") { calendarNamer(loadedSettings.defaultCalendar) }
            }
            val place = async {
                resolve("default location") {
                    loadedSettings.defaultLocation?.let { locationDao.getByUid(it) }
                }
            }
            val noPlace = async { resolve("none") { getString(Res.string.none) } }
            val resolvedList = list.await()
            val resolvedTags = tags.await()
            val resolvedRecurrence = recurrence.await()
            val resolvedCalendar = calendar.await()
            val resolvedPlace = place.await()
            val none = noPlace.await().orEmpty()
            if (generation != mutationSequence) {
                if (!loaded) {
                    refreshState()
                }
                return@coroutineScope
            }
            if (reloadGeneration < appliedReload) {
                return@coroutineScope
            }
            appliedReload = reloadGeneration
            noneLabel = none
            settings = loadedSettings
            defaultListFilter = resolvedList
            defaultTagsSummary = resolvedTags ?: none
            recurrenceSummary = resolvedRecurrence.orEmpty()
            calendarName = resolvedCalendar.orEmpty()
            hasDefaultLocation = resolvedPlace != null
            locationName = resolvedPlace?.displayName ?: none
            loaded = true
        }
    }

    private suspend fun reloadSafely() {
        resolve("task defaults") { reload() }
    }

    private suspend fun <T> resolve(what: String, block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, tag = TAG) { "Failed to resolve $what" }
            null
        }

    private fun updateSummary(current: Job?, what: String, block: suspend () -> Unit): Job {
        current?.cancel()
        return viewModelScope.launch { resolve(what) { block() } }
    }

    fun refreshState() = writes.refresh()

    private fun persist(block: suspend () -> Unit) {
        mutationSequence++
        writes.write(block)
    }

    protected open suspend fun onLocationUpdateIntervalChanged() {}

    suspend fun defaultTags(): List<TagData> = tagsFor(settings.defaultTags)

    fun updateAddToTop(enabled: Boolean) {
        settings = settings.copy(addTasksToTop = enabled)
        persist { appPreferences.setAddTasksToTop(enabled) }
    }

    fun setDefaultList(filter: CaldavFilter) {
        val uuid = filter.calendar.uuid
        settings = settings.copy(defaultList = uuid)
        defaultListFilter = filter
        persist { appPreferences.setDefaultList(uuid) }
    }

    fun setDefaultTags(tags: List<TagData>) {
        val uuids = tags.mapNotNull { it.remoteId }
        settings = settings.copy(defaultTags = uuids)
        tagsSummaryJob = updateSummary(tagsSummaryJob, "default tags") {
            defaultTagsSummary = describeTags(uuids)
        }
        persist { appPreferences.setDefaultTags(uuids) }
    }

    fun setDefaultCalendar(calendarId: String?) {
        settings = settings.copy(defaultCalendar = calendarId)
        calendarNameJob = updateSummary(calendarNameJob, "default calendar") {
            calendarName = calendarNamer(calendarId)
        }
        persist { appPreferences.setDefaultCalendar(calendarId) }
    }

    fun setRecurrence(rrule: String?) {
        val recurrence = rrule?.takeIf { it.isNotBlank() }
        settings = settings.copy(defaultRecurrence = recurrence)
        recurrenceSummaryJob = updateSummary(recurrenceSummaryJob, "default recurrence") {
            recurrenceSummary = describeRecurrence(recurrence)
        }
        persist { appPreferences.setDefaultRecurrence(recurrence) }
    }

    fun setDefaultAlarms(alarms: List<Alarm>) {
        settings = settings.copy(defaultAlarms = alarms.sortedWith(alarmOrder))
        persist { appPreferences.setDefaultAlarms(alarms) }
    }

    fun setDefaultLocation(place: Place?) {
        settings = settings.copy(defaultLocation = place?.uid)
        hasDefaultLocation = place != null
        locationNameJob = updateSummary(locationNameJob, "default location") {
            locationName = place?.displayName ?: getString(Res.string.none)
        }
        persist { appPreferences.setDefaultLocation(place?.uid) }
    }

    fun openListPicker(target: ListPickerTarget) {
        listPickerTarget = target
    }

    fun dismissListPicker() {
        listPickerTarget = null
    }

    fun listPickerValue(target: ListPickerTarget): Int = when (target) {
        ListPickerTarget.PRIORITY -> settings.defaultPriority
        ListPickerTarget.START_DATE -> settings.defaultHideUntil
        ListPickerTarget.DUE_DATE -> settings.defaultDueDate
        ListPickerTarget.REPEAT_FROM -> settings.defaultRecurrenceFrom
        ListPickerTarget.RING_MODE -> settings.defaultRingMode
        ListPickerTarget.LOCATION_REMINDER -> settings.defaultLocationReminder
        ListPickerTarget.LOCATION_UPDATE_INTERVAL -> settings.locationUpdateIntervalMinutes
    }

    fun setListPickerValue(target: ListPickerTarget, value: Int) {
        when (target) {
            ListPickerTarget.PRIORITY -> {
                settings = settings.copy(defaultPriority = value)
                persist { appPreferences.setDefaultPriority(value) }
            }
            ListPickerTarget.START_DATE -> {
                settings = settings.copy(defaultHideUntil = value)
                persist { appPreferences.setDefaultHideUntil(value) }
            }
            ListPickerTarget.DUE_DATE -> {
                settings = settings.copy(defaultDueDate = value)
                persist { appPreferences.setDefaultDueDate(value) }
            }
            ListPickerTarget.REPEAT_FROM -> {
                settings = settings.copy(defaultRecurrenceFrom = value)
                persist { appPreferences.setDefaultRecurrenceFrom(value) }
            }
            ListPickerTarget.RING_MODE -> {
                settings = settings.copy(defaultRingMode = value)
                persist { appPreferences.setDefaultRingMode(value) }
            }
            ListPickerTarget.LOCATION_REMINDER -> {
                settings = settings.copy(defaultLocationReminder = value)
                persist { appPreferences.setDefaultLocationReminder(value) }
            }
            ListPickerTarget.LOCATION_UPDATE_INTERVAL -> {
                settings = settings.copy(locationUpdateIntervalMinutes = value)
                persist {
                    appPreferences.setLocationUpdateIntervalMinutes(value)
                    onLocationUpdateIntervalChanged()
                }
            }
        }
    }

    private suspend fun tagsFor(uuids: List<String>): List<TagData> = uuids
        .takeIf { it.isNotEmpty() }
        ?.let { tagDataDao.getByUuid(it) }
        ?.sortedBy { it.name }
        ?: emptyList()

    private suspend fun describeTags(uuids: List<String>): String = tagsFor(uuids)
        .mapNotNull { it.name }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?: getString(Res.string.none)

    private suspend fun describeRecurrence(rrule: String?): String = rrule
        ?.let {
            try {
                repeatRuleToString.toString(it)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e, tag = TAG) { "Failed to describe $it" }
                null
            }
        }
        ?: getString(Res.string.repeat_option_does_not_repeat)
}

private const val TAG = "TaskDefaultsViewModel"
