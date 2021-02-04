package org.tasks.ui

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.todoroo.andlib.utility.DateUtilities.now
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.data.Task.Companion.hasDueTime
import com.todoroo.astrid.data.Task.Companion.isRepeatAfterCompletion
import com.todoroo.astrid.data.Task.Companion.withoutFrom
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.timers.TimerPlugin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import net.fortuna.ical4j.model.Recur
import org.tasks.Event
import org.tasks.R
import org.tasks.Strings
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.*
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.location.GeofenceApi
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.currentTimeMillis
import org.tasks.time.DateTimeUtils.startOfDay
import timber.log.Timber
import java.text.ParseException
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
        private val alarmService: AlarmService) : ViewModel() {

    val cleared = MutableLiveData<Event<Boolean>>()

    fun setup(
            task: Task, list: Filter, location: Location?, tags: List<TagData>, alarms: LongArray) {
        this.task = task
        isNew = task.isNew
        originalList = list
        originalLocation = location
        originalTags = tags.toImmutableList()
        originalAlarms = alarms.toList().toImmutableSet()
        if (isNew && permissionChecker.canAccessCalendars()) {
            originalCalendar = preferences.defaultCalendar
        }
        eventUri = task.calendarURI
    }

    var task: Task? = null
        private set

    var title: String? = null
        get() = field ?: task?.title

    var completed: Boolean? = null
        get() = field ?: task?.isCompleted ?: false

    var dueDate: Long? = null
        get() = field ?: task?.dueDate ?: 0
        set(value) {
            field = when {
                value == null -> null
                value == 0L -> 0
                hasDueTime(value) -> createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, value)
                else -> createDueDate(Task.URGENCY_SPECIFIC_DAY, value)
            }
        }

    var priority: Int? = null
        get() = field ?: task?.priority ?: 0

    var description: String? = null
        get() = field ?: task?.notes.stripCarriageReturns()

    var hideUntil: Long? = null
        get() = field ?: task?.hideUntil ?: 0
        set(value) {
            field = when {
                value == null -> null
                value == 0L -> 0
                hasDueTime(value) ->
                    value.toDateTime().withSecondOfMinute(1).withMillisOfSecond(0).millis
                else -> value.startOfDay()
            }
        }

    var recurrence: String? = null
        get() = field ?: task?.recurrence

    var repeatUntil: Long? = null
        get() = field ?: task?.repeatUntil ?: 0

    var repeatAfterCompletion: Boolean? = null
        get() = field ?: task?.repeatAfterCompletion() ?: false
        set(value) {
            field = value
            if (value == true) {
                if (!recurrence.isRepeatAfterCompletion()) {
                    recurrence += ";FROM=COMPLETION"
                }
            } else if (recurrence.isRepeatAfterCompletion()) {
                recurrence = recurrence.withoutFrom()
            }
        }

    var recur: Recur?
        get() = if (recurrence.isNullOrBlank()) {
            null
        } else {
            val rrule = newRecur(recurrence!!)
            repeatUntil?.takeIf { it > 0 }?.let {
                rrule.until = DateTime(it).toDate()
            }
            rrule
        }
        set(value) {
            if (value == null) {
                recurrence = ""
                repeatUntil = 0
                return
            }
            val copy = try {
                newRecur(value.toString())
            } catch (e: ParseException) {
                recurrence = ""
                repeatUntil = 0
                return
            }
            repeatUntil = DateTime.from(copy.until).millis
            if (repeatUntil ?: 0 > 0) {
                copy.until = null
            }
            var result = copy.toString()
            if (repeatAfterCompletion!! && !result.isNullOrBlank()) {
                result += ";FROM=COMPLETION"
            }
            recurrence = result
        }

    var originalCalendar: String? = null
        private set(value) {
            field = value
            selectedCalendar = value
        }

    var selectedCalendar: String? = null

    var eventUri: String? = null

    var isNew: Boolean = false
        private set

    var timerStarted: Long
        get() = task?.timerStart ?: 0
        set(value) {
            task?.timerStart = value
        }

    var estimatedSeconds: Int? = null
        get() = field ?: task?.estimatedSeconds ?: 0

    var elapsedSeconds: Int? = null
        get() = field ?: task?.elapsedSeconds ?: 0

    var originalList: Filter? = null
        private set(value) {
            field = value
            selectedList = value
        }

    var selectedList: Filter? = null

    var originalLocation: Location? = null
        private set(value) {
            field = value
            selectedLocation = value
        }

    var selectedLocation: Location? = null

    var originalTags: ImmutableList<TagData>? = null
        private set(value) {
            field = value
            selectedTags = value?.let { ArrayList(it) }
        }

    var selectedTags: ArrayList<TagData>? = null

    var newSubtasks = ArrayList<Task>()

    var reminderPeriod: Long? = null
        get() = field ?: task?.reminderPeriod ?: 0

    var originalAlarms: ImmutableSet<Long>? = null
        private set(value) {
            field = value
            selectedAlarms = value?.let { HashSet(it) }
        }

    var selectedAlarms: HashSet<Long>? = null

    var whenDue: Boolean? = null
        get() = field ?: (task?.reminderFlags?.and(Task.NOTIFY_AT_DEADLINE) ?: 0 > 0)

    var whenOverdue: Boolean? = null
        get() = field ?: (task?.reminderFlags?.and(Task.NOTIFY_AFTER_DEADLINE) ?: 0 > 0)

    var ringNonstop: Boolean? = null
        get() = field ?: (task?.reminderFlags?.and(Task.NOTIFY_MODE_NONSTOP) ?: 0 > 0)
        set(value) {
            field = value
            if (value == true) {
                ringFiveTimes = false
            }
        }

    var ringFiveTimes:Boolean? = null
        get() = field ?: (task?.reminderFlags?.and(Task.NOTIFY_MODE_FIVE) ?: 0 > 0)
        set(value) {
            field = value
            if (value == true) {
                ringNonstop = false
            }
        }

    fun hasChanges(): Boolean = task?.let {
        (it.title != title || (isNew && title?.isNotBlank() == true)) ||
                it.isCompleted != completed ||
                it.dueDate != dueDate ||
                it.priority != priority ||
                it.notes != description ||
                it.hideUntil != hideUntil ||
                if (it.recurrence.isNullOrBlank()) {
                    !recurrence.isNullOrBlank()
                } else {
                    it.recurrence != recurrence
                } ||
                it.repeatAfterCompletion() != repeatAfterCompletion ||
                it.repeatUntil != repeatUntil ||
                originalCalendar != selectedCalendar ||
                if (it.calendarURI.isNullOrBlank()) {
                    !eventUri.isNullOrBlank()
                } else {
                    it.calendarURI != eventUri
                } ||
                it.elapsedSeconds != elapsedSeconds ||
                it.estimatedSeconds != estimatedSeconds ||
                originalList != selectedList ||
                originalLocation != selectedLocation ||
                originalTags?.toHashSet() != selectedTags?.toHashSet() ||
                newSubtasks.isNotEmpty() ||
                it.reminderPeriod != reminderPeriod ||
                it.reminderFlags != getReminderFlags() ||
                originalAlarms != selectedAlarms
    } ?: false

    fun cleared() = cleared.value?.value == true

    @MainThread
    suspend fun save(): Boolean = task?.let {
        if (cleared()) {
            return false
        }
        if (!hasChanges()) {
            discard()
            return false
        }
        clear()
        it.title = if (title.isNullOrBlank()) context.getString(R.string.no_title) else title
        it.dueDate = dueDate!!
        it.priority = priority!!
        it.notes = description
        it.hideUntil = hideUntil!!
        it.recurrence = recurrence
        it.repeatUntil = repeatUntil!!
        it.elapsedSeconds = elapsedSeconds!!
        it.estimatedSeconds = estimatedSeconds!!
        it.reminderFlags = getReminderFlags()
        it.reminderPeriod = reminderPeriod!!

        applyCalendarChanges()

        val isNew = it.isNew

        if (isNew) {
            taskDao.createNew(it)
        }

        if (isNew || originalList != selectedList) {
            it.parent = 0
            taskMover.move(listOf(it.id), selectedList!!)
        }

        if ((isNew && selectedLocation != null) || originalLocation != selectedLocation) {
            originalLocation?.let { location ->
                if (location.geofence.id > 0) {
                    locationDao.delete(location.geofence)
                    geofenceApi.update(location.place)
                }
            }
            selectedLocation?.let { location ->
                val place = location.place
                val geofence = location.geofence
                geofence.task = it.id
                geofence.place = place.uid
                geofence.id = locationDao.insert(geofence)
                geofenceApi.update(place)
            }
            it.putTransitory(SyncFlags.FORCE_CALDAV_SYNC, true)
            it.modificationDate = currentTimeMillis()
        }

        if ((isNew && selectedTags?.isNotEmpty() == true) || originalTags?.toHashSet() != selectedTags?.toHashSet()) {
            tagDao.applyTags(it, tagDataDao, selectedTags!!)
            it.modificationDate = currentTimeMillis()
        }

        for (subtask in newSubtasks) {
            if (Strings.isNullOrEmpty(subtask.title)) {
                continue
            }
            if (!subtask.isCompleted) {
                subtask.completionDate = it.completionDate
            }
            taskDao.createNew(subtask)
            when (selectedList) {
                is GtasksFilter -> {
                    val googleTask = GoogleTask(subtask.id, (selectedList as GtasksFilter).remoteId)
                    googleTask.parent = it.id
                    googleTask.isMoved = true
                    googleTaskDao.insertAndShift(googleTask, false)
                }
                is CaldavFilter -> {
                    val caldavTask = CaldavTask(subtask.id, (selectedList as CaldavFilter).uuid)
                    subtask.parent = it.id
                    caldavTask.remoteParent = caldavDao.getRemoteIdForTask(it.id)
                    taskDao.save(subtask)
                    caldavDao.insert(subtask, caldavTask, false)
                }
                else -> {
                    subtask.parent = it.id
                    taskDao.save(subtask)
                }
            }
        }

        if (selectedAlarms != originalAlarms) {
            alarmService.synchronizeAlarms(it.id, selectedAlarms!!)
            it.modificationDate = now()
        }

        taskDao.save(it, null)

        if (it.isCompleted != completed!!) {
            taskCompleter.setComplete(it, completed!!)
        }

        true
    } ?: false

    private suspend fun applyCalendarChanges() {
        if (!permissionChecker.canAccessCalendars()) {
            return
        }
        if (eventUri == null) {
            calendarEventProvider.deleteEvent(task)
        }
        if (task?.hasDueDate() != true) {
            return
        }
        selectedCalendar?.let {
            try {
                task?.calendarURI = gCalHelper.createTaskEvent(task!!, it)?.toString()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getReminderFlags(): Int {
        var value = 0
        if (whenDue == true) {
            value = value or Task.NOTIFY_AT_DEADLINE
        }
        if (whenOverdue == true) {
            value = value or Task.NOTIFY_AFTER_DEADLINE
        }
        value = value and (Task.NOTIFY_MODE_FIVE or Task.NOTIFY_MODE_NONSTOP).inv()
        if (ringNonstop == true) {
            value = value or Task.NOTIFY_MODE_NONSTOP
        } else if (ringFiveTimes == true) {
            value = value or Task.NOTIFY_MODE_FIVE
        }
        return value
    }

    suspend fun delete() {
        task?.let { taskDeleter.markDeleted(it) }
        discard()
    }

    suspend fun discard() {
        task?.let {
            if (it.isNew) {
                timerPlugin.stopTimer(it)
            }
        }
        clear()
    }

    @MainThread
    fun clear() {
        if (!cleared()) {
            cleared.value = Event(true)
        }
    }

    override fun onCleared() {
        cleared.value.let {
            if (it == null || !it.value) {
                runBlocking(NonCancellable) {
                    save()
                }
            }
        }
    }

    companion object {
        fun String?.stripCarriageReturns(): String? = this?.replace("\\r\\n?".toRegex(), "\n")
    }
}