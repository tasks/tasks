package org.tasks.ui

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.NOTIFY_MODE_FIVE
import com.todoroo.astrid.data.Task.Companion.NOTIFY_MODE_NONSTOP
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Strings
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.*
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.Alarm.Companion.TYPE_REL_END
import org.tasks.data.Alarm.Companion.TYPE_REL_START
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.location.GeofenceApi
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.time.DateTimeUtils.startOfDay
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskEditViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val taskDao: TaskDao,
        private val taskDeleter: TaskDeleter,
        private val timerPlugin: TimerPlugin,
        private val permissionChecker: PermissionChecker,
        private val calendarEventProvider: CalendarEventProvider,
        private val gCalHelper: GCalHelper,
        private val taskMover: TaskMover,
        private val locationDao: LocationDao,
        private val geofenceApi: GeofenceApi,
        private val tagDao: TagDao,
        private val tagDataDao: TagDataDao,
        private val preferences: Preferences,
        private val googleTaskDao: GoogleTaskDao,
        private val caldavDao: CaldavDao,
        private val taskCompleter: TaskCompleter,
        private val alarmService: AlarmService,
        private val taskListEvents: TaskListEventBus,
        private val mainActivityEvents: MainActivityEventBus,
        private val firebase: Firebase? = null,
) : ViewModel() {

    private var cleared = false

    fun setup(
            task: Task,
            list: Filter,
            location: Location?,
            tags: List<TagData>,
            alarms: List<Alarm>,
    ) {
        this.task = task
        dueDate.value = task.dueDate
        startDate.value = task.hideUntil
        isNew = task.isNew
        originalList = list
        selectedList.value = list
        originalLocation = location
        originalTags = tags.toList()
        selectedTags.value = ArrayList(tags)
        originalAlarms =
            if (isNew) {
                ArrayList<Alarm>().apply {
                    if (task.isNotifyAtStart) {
                        add(whenStarted(0))
                    }
                    if (task.isNotifyAtDeadline) {
                        add(whenDue(0))
                    }
                    if (task.isNotifyAfterDeadline) {
                        add(whenOverdue(0))
                    }
                    if (task.randomReminder > 0) {
                        add(Alarm(0, task.randomReminder, TYPE_RANDOM))
                    }
                }
            } else {
                alarms
            }
        selectedAlarms.value = originalAlarms
        if (isNew && permissionChecker.canAccessCalendars()) {
            originalCalendar = preferences.defaultCalendar
        }
        eventUri.value = task.calendarURI
        priority.value = task.priority
        elapsedSeconds.value = task.elapsedSeconds
        estimatedSeconds.value = task.estimatedSeconds
        timerStarted.value = task.timerStart
        recurrence.value = task.recurrence
        repeatAfterCompletion.value = task.repeatAfterCompletion()
    }

    lateinit var task: Task
        private set

    var creationDate: Long? = null
        get() = field ?: task.creationDate

    var modificationDate: Long? = null
        get() = field ?: task.modificationDate

    var completionDate: Long? = null
        get() = field ?: task.completionDate

    var title: String? = null
        get() = field ?: task.title

    var completed: Boolean? = null
        get() = field ?: task.isCompleted

    val dueDate = MutableStateFlow(0L)

    fun setDueDate(value: Long) {
        dueDate.value = when {
            value == 0L -> 0
            hasDueTime(value) -> createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, value)
            else -> createDueDate(Task.URGENCY_SPECIFIC_DAY, value)
        }
    }

    var priority = MutableStateFlow(Task.Priority.NONE)

    var description: String? = null
        get() = field ?: task.notes.stripCarriageReturns()

    val startDate = MutableStateFlow(0L)

    fun setStartDate(value: Long) {
        startDate.value = when {
            value == 0L -> 0
            hasDueTime(value) ->
                value.toDateTime().withSecondOfMinute(1).withMillisOfSecond(0).millis
            else -> value.startOfDay()
        }
    }

    val recurrence = MutableStateFlow<String?>(null)
    val repeatAfterCompletion = MutableStateFlow(false)

    private var originalCalendar: String? = null
        private set(value) {
            field = value
            selectedCalendar.value = value
        }

    var selectedCalendar = MutableStateFlow<String?>(null)

    var eventUri = MutableStateFlow<String?>(null)

    var isNew: Boolean = false
        private set

    val timerStarted = MutableStateFlow(0L)
    val estimatedSeconds = MutableStateFlow(0)
    val elapsedSeconds = MutableStateFlow(0)

    private lateinit var originalList: Filter

    var selectedList = MutableStateFlow(null as Filter?)

    var originalLocation: Location? = null
        private set(value) {
            field = value
            selectedLocation.value = value
        }

    var selectedLocation = MutableStateFlow<Location?>(null)

    private lateinit var originalTags: List<TagData>

    val selectedTags = MutableStateFlow(ArrayList<TagData>())

    var newSubtasks = MutableStateFlow(emptyList<Task>())

    private lateinit var originalAlarms: List<Alarm>

    var selectedAlarms = MutableStateFlow(emptyList<Alarm>())

    var ringNonstop: Boolean? = null
        get() = field ?: task.isNotifyModeNonstop
        set(value) {
            field = value
            if (value == true) {
                ringFiveTimes = false
            }
        }

    var ringFiveTimes:Boolean? = null
        get() = field ?: task.isNotifyModeFive
        set(value) {
            field = value
            if (value == true) {
                ringNonstop = false
            }
        }

    fun hasChanges(): Boolean =
        (task.title != title || (isNew && title?.isNotBlank() == true)) ||
                task.isCompleted != completed ||
                task.dueDate != dueDate.value ||
                task.priority != priority.value ||
                if (task.notes.isNullOrBlank()) {
                    !description.isNullOrBlank()
                } else {
                    task.notes != description
                } ||
                task.hideUntil != startDate.value ||
                if (task.recurrence.isNullOrBlank()) {
                    !recurrence.value.isNullOrBlank()
                } else {
                    task.recurrence != recurrence.value
                } ||
                task.repeatAfterCompletion() != repeatAfterCompletion.value ||
                originalCalendar != selectedCalendar.value ||
                if (task.calendarURI.isNullOrBlank()) {
                    !eventUri.value.isNullOrBlank()
                } else {
                    task.calendarURI != eventUri.value
                } ||
                task.elapsedSeconds != elapsedSeconds.value ||
                task.estimatedSeconds != estimatedSeconds.value ||
                originalList != selectedList.value ||
                originalLocation != selectedLocation.value ||
                originalTags.toHashSet() != selectedTags.value.toHashSet() ||
                newSubtasks.value.isNotEmpty() ||
                getRingFlags() != when {
                    task.isNotifyModeFive -> NOTIFY_MODE_FIVE
                    task.isNotifyModeNonstop -> NOTIFY_MODE_NONSTOP
                    else -> 0
                } ||
                originalAlarms.toHashSet() != selectedAlarms.value.toHashSet()

    @MainThread
    suspend fun save(remove: Boolean = true): Boolean = withContext(NonCancellable) {
        if (cleared) {
            return@withContext false
        }
        if (!hasChanges()) {
            discard(remove)
            return@withContext false
        }
        clear(remove)
        task.title = if (title.isNullOrBlank()) context.getString(R.string.no_title) else title
        task.dueDate = dueDate.value
        task.priority = priority.value
        task.notes = description
        task.hideUntil = startDate.value
        task.recurrence = recurrence.value
        task.repeatFrom = if (repeatAfterCompletion.value) {
            Task.RepeatFrom.COMPLETION_DATE
        } else {
            Task.RepeatFrom.DUE_DATE
        }
        task.elapsedSeconds = elapsedSeconds.value
        task.estimatedSeconds = estimatedSeconds.value
        task.ringFlags = getRingFlags()

        applyCalendarChanges()

        val isNew = task.isNew

        if (isNew) {
            taskDao.createNew(task)
        }

        if (isNew || originalList != selectedList.value) {
            task.parent = 0
            taskMover.move(listOf(task.id), selectedList.value!!)
        }

        if ((isNew && selectedLocation.value != null) || originalLocation != selectedLocation.value) {
            originalLocation?.let { location ->
                if (location.geofence.id > 0) {
                    locationDao.delete(location.geofence)
                    geofenceApi.update(location.place)
                }
            }
            selectedLocation.value?.let { location ->
                val place = location.place
                val geofence = location.geofence
                geofence.task = task.id
                geofence.place = place.uid
                geofence.id = locationDao.insert(geofence)
                geofenceApi.update(place)
            }
            task.putTransitory(SyncFlags.FORCE_CALDAV_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }

        if ((isNew && selectedTags.value.isNotEmpty()) || originalTags.toHashSet() != selectedTags.value.toHashSet()) {
            tagDao.applyTags(task, tagDataDao, selectedTags.value)
            task.modificationDate = currentTimeMillis()
        }

        for (subtask in newSubtasks.value) {
            if (Strings.isNullOrEmpty(subtask.title)) {
                continue
            }
            if (!subtask.isCompleted) {
                subtask.completionDate = task.completionDate
            }
            taskDao.createNew(subtask)
            firebase?.addTask("subtasks")
            when (selectedList.value) {
                is GtasksFilter -> {
                    val googleTask = GoogleTask(subtask.id, (selectedList.value as GtasksFilter).remoteId)
                    googleTask.parent = task.id
                    googleTask.isMoved = true
                    googleTaskDao.insertAndShift(googleTask, false)
                }
                is CaldavFilter -> {
                    val caldavTask = CaldavTask(subtask.id, (selectedList.value as CaldavFilter).uuid)
                    subtask.parent = task.id
                    caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.id)
                    taskDao.save(subtask)
                    caldavDao.insert(subtask, caldavTask, false)
                }
                else -> {
                    subtask.parent = task.id
                    taskDao.save(subtask)
                }
            }
        }

        if (!task.hasStartDate()) {
            selectedAlarms.value = selectedAlarms.value.filterNot { a -> a.type == TYPE_REL_START }
        }
        if (!task.hasDueDate()) {
            selectedAlarms.value = selectedAlarms.value.filterNot { a -> a.type == TYPE_REL_END }
        }

        taskDao.save(task, null)

        if (
            selectedAlarms.value.toHashSet() != originalAlarms.toHashSet() ||
            (isNew && selectedAlarms.value.isNotEmpty())
        ) {
            alarmService.synchronizeAlarms(task.id, selectedAlarms.value.toMutableSet())
            task.putTransitory(SyncFlags.FORCE_CALDAV_SYNC, true)
            task.modificationDate = now()
        }

        if (task.isCompleted != completed!!) {
            taskCompleter.setComplete(task, completed!!)
        }

        if (isNew) {
            val model = task
            taskListEvents.emit(TaskListEvent.TaskCreated(model.uuid))
            model.calendarURI?.takeIf { it.isNotBlank() }?.let {
                taskListEvents.emit(TaskListEvent.CalendarEventCreated(model.title, it))
            }
            mainActivityEvents.emit(MainActivityEvent.RequestRating)
        }
        true
    }

    private suspend fun applyCalendarChanges() {
        if (!permissionChecker.canAccessCalendars()) {
            return
        }
        if (eventUri.value == null) {
            calendarEventProvider.deleteEvent(task)
        }
        if (!task.hasDueDate()) {
            return
        }
        selectedCalendar.value?.let {
            try {
                task.calendarURI = gCalHelper.createTaskEvent(task, it)?.toString()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getRingFlags() = when {
        ringNonstop == true -> NOTIFY_MODE_NONSTOP
        ringFiveTimes == true -> NOTIFY_MODE_FIVE
        else -> 0
    }

    suspend fun delete() {
        taskDeleter.markDeleted(task)
        discard()
    }

    suspend fun discard(remove: Boolean = true) {
        if (task.isNew) {
            timerPlugin.stopTimer(task)
        }
        clear(remove)
    }

    @MainThread
    suspend fun clear(remove: Boolean = true) {
        if (cleared) {
            return
        }
        cleared = true
        if (remove) {
            mainActivityEvents.emit(MainActivityEvent.ClearTaskEditFragment)
        }
    }

    override fun onCleared() {
        if (!cleared) {
            runBlocking {
                save(remove = false)
            }
        }
    }

    companion object {
        fun String?.stripCarriageReturns(): String? = this?.replace("\\r\\n?".toRegex(), "\n")
    }
}