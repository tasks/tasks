package org.tasks.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoroo.astrid.activity.BeastModePreferences
import com.todoroo.astrid.activity.TaskEditFragment
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.files.FilesControlSet
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.service.TaskCompleter
import com.todoroo.astrid.service.TaskCreator.Companion.getDefaultAlarms
import com.todoroo.astrid.service.TaskDeleter
import com.todoroo.astrid.service.TaskMover
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.timers.TimerPlugin
import com.todoroo.astrid.ui.ReminderControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Strings
import org.tasks.analytics.Firebase
import org.tasks.calendars.CalendarEventProvider
import org.tasks.data.Location
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.whenDue
import org.tasks.data.entity.Alarm.Companion.whenOverdue
import org.tasks.data.entity.Alarm.Companion.whenStarted
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.FORCE_CALDAV_SYNC
import org.tasks.data.entity.FORCE_MICROSOFT_SYNC
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Companion.NOTIFY_MODE_FIVE
import org.tasks.data.entity.Task.Companion.NOTIFY_MODE_NONSTOP
import org.tasks.data.entity.Task.Companion.hasDueTime
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.UserActivity
import org.tasks.data.setPicture
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.files.FileHelper
import org.tasks.filters.CaldavFilter
import org.tasks.location.GeofenceApi
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.startOfDay
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
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
    private val userActivityDao: UserActivityDao,
    private val alarmDao: AlarmDao,
    private val taskAttachmentDao: TaskAttachmentDao,
) : ViewModel() {
    data class ViewState(
        val task: Task,
        val displayOrder: ImmutableList<Int>,
        val showBeastModeHint: Boolean,
        val showComments: Boolean,
        val showKeyboard: Boolean,
        val backButtonSavesTask: Boolean,
        val isReadOnly: Boolean,
        val linkify: Boolean,
        val list: CaldavFilter,
        val location: Location?,
        val tags: ImmutableSet<TagData>,
        val calendar: String?,
        val attachments: ImmutableSet<TaskAttachment> = persistentSetOf(),
        val alarms: ImmutableSet<Alarm>,
        val newSubtasks: ImmutableList<Task> = persistentListOf(),
        val multilineTitle: Boolean,
    ) {
        val isNew: Boolean
            get() = task.isNew

        val hasParent: Boolean
            get() = task.parent > 0

        val isCompleted: Boolean
            get() = task.completionDate > 0
    }

    private val resources = context.resources
    private var cleared = false

    private val task: Task = savedStateHandle.get<Task>(TaskEditFragment.EXTRA_TASK)
        ?.apply { notes = notes?.stripCarriageReturns() } // copying here broke tests 🙄
        ?: throw IllegalArgumentException("task is null")

    private var _originalState: ViewState
    val originalState: ViewState
        get() = _originalState

    private val _viewState = MutableStateFlow(
        ViewState(
            task = task,
            showBeastModeHint = !preferences.shownBeastModeHint,
            showComments = preferences.getBoolean(R.string.p_show_task_edit_comments, false),
            showKeyboard = task.isNew && task.title.isNullOrBlank(),
            backButtonSavesTask = preferences.backButtonSavesTask(),
            isReadOnly = task.readOnly,
            linkify = preferences.linkify,
            list = savedStateHandle[TaskEditFragment.EXTRA_LIST]!!,
            location = savedStateHandle[TaskEditFragment.EXTRA_LOCATION],
            tags = savedStateHandle.get<ArrayList<TagData>>(TaskEditFragment.EXTRA_TAGS)
                ?.toPersistentSet()
                ?: persistentSetOf(),
            calendar = if (task.isNew && permissionChecker.canAccessCalendars()) {
                preferences.defaultCalendar
            } else {
                null
            },
            displayOrder = TASK_EDIT_CONTROL_SET_FRAGMENTS
                .associateBy(context::getString) { it }
                .let { controlSetStrings ->
                    BeastModePreferences
                        .constructOrderedControlList(preferences, context)
                        .let { items ->
                            items
                                .subList(
                                    0,
                                    items.indexOf(context.getString(R.string.TEA_ctrl_hide_section_pref))
                                )
                                .also { it.add(0, context.getString(R.string.TEA_ctrl_title)) }
                        }
                        .mapNotNull { controlSetStrings[it] }
                        .toPersistentList()
                },
            alarms = if (task.isNew) {
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
                        add(Alarm(time = task.randomReminder, type = Alarm.TYPE_RANDOM))
                    }
                }
            } else {
                savedStateHandle[TaskEditFragment.EXTRA_ALARMS]!!
            }.toPersistentSet(),
            multilineTitle = preferences.multilineTitle,
        )
    )
    val viewState: StateFlow<ViewState> = _viewState

    var eventUri = MutableStateFlow(task.calendarURI)
    val timerStarted = MutableStateFlow(task.timerStart)
    val estimatedSeconds = MutableStateFlow(task.estimatedSeconds)
    val elapsedSeconds = MutableStateFlow(task.elapsedSeconds)

    val dueDate = MutableStateFlow(task.dueDate)

    fun setDueDate(value: Long) {
        val addedDueDate = value > 0 && dueDate.value == 0L
        dueDate.value = when {
            value == 0L -> 0
            hasDueTime(value) -> createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, value)
            else -> createDueDate(Task.URGENCY_SPECIFIC_DAY, value)
        }
        if (addedDueDate) {
            val reminderFlags = preferences.defaultReminders
            if (reminderFlags.isFlagSet(Task.NOTIFY_AT_DEADLINE)) {
                _viewState.update { state ->
                    state.copy(alarms = state.alarms.plusAlarm(whenDue(task.id)))
                }
            }
            if (reminderFlags.isFlagSet(Task.NOTIFY_AFTER_DEADLINE)) {
                _viewState.update { state ->
                    state.copy(alarms = state.alarms.plusAlarm(whenOverdue(task.id)))
                }
            }
        }
    }

    val startDate = MutableStateFlow(task.hideUntil)

    fun setStartDate(value: Long) {
        val addedStartDate = value > 0 && startDate.value == 0L
        startDate.value = when {
            value == 0L -> 0
            hasDueTime(value) ->
                value.toDateTime().withSecondOfMinute(1).withMillisOfSecond(0).millis
            else -> value.startOfDay()
        }
        if (addedStartDate && preferences.defaultReminders.isFlagSet(Task.NOTIFY_AT_START)) {
            _viewState.update { state ->
                state.copy(alarms = state.alarms.plusAlarm(whenStarted(task.id)))
            }
        }
    }

    var ringNonstop: Boolean = task.isNotifyModeNonstop
        set(value) {
            field = value
            if (value) {
                ringFiveTimes = false
            }
        }

    var ringFiveTimes:Boolean = task.isNotifyModeFive
        set(value) {
            field = value
            if (value) {
                ringNonstop = false
            }
        }

    fun hasChanges(): Boolean {
        val viewState = _viewState.value
        return originalState != viewState ||
                (viewState.isNew && viewState.task.title?.isNotBlank() == true) || // text shared to tasks
                task.dueDate != dueDate.value ||
                task.hideUntil != startDate.value ||
                if (task.calendarURI.isNullOrBlank()) {
                    !eventUri.value.isNullOrBlank()
                } else {
                    task.calendarURI != eventUri.value
                } ||
                task.elapsedSeconds != elapsedSeconds.value ||
                task.estimatedSeconds != estimatedSeconds.value ||
                getRingFlags() != when {
            task.isNotifyModeFive -> NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> NOTIFY_MODE_NONSTOP
            else -> 0
        }
    }

    @MainThread
    suspend fun save(remove: Boolean = true): Boolean = withContext(NonCancellable) {
        if (cleared) {
            return@withContext false
        }
        if (!hasChanges() || viewState.value.isReadOnly) {
            discard(remove)
            return@withContext false
        }
        clear(remove)
        val viewState = _viewState.value
        val isNew = viewState.isNew
        task.title = if (viewState.task.title.isNullOrBlank()) resources.getString(R.string.no_title) else viewState.task.title
        task.dueDate = dueDate.value
        task.priority = viewState.task.priority
        task.notes = viewState.task.notes
        task.hideUntil = startDate.value
        task.recurrence = viewState.task.recurrence
        task.repeatFrom = viewState.task.repeatFrom
        task.elapsedSeconds = elapsedSeconds.value
        task.estimatedSeconds = estimatedSeconds.value
        task.ringFlags = getRingFlags()

        applyCalendarChanges()
        if (isNew) {
            taskDao.createNew(task)
        }
        val selectedLocation = _viewState.value.location
        if ((isNew && selectedLocation != null) || originalState.location != selectedLocation) {
            originalState.location?.let { location ->
                if (location.geofence.id > 0) {
                    locationDao.delete(location.geofence)
                    geofenceApi.update(location.place)
                }
            }
            selectedLocation?.let { location ->
                val place = location.place
                locationDao.insert(
                    location.geofence.copy(
                        task = task.id,
                        place = place.uid,
                    )
                )
                geofenceApi.update(place)
            }
            task.putTransitory(FORCE_CALDAV_SYNC, true)
            task.putTransitory(FORCE_MICROSOFT_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }
        val selectedTags = _viewState.value.tags
        if ((isNew && selectedTags.isNotEmpty()) || originalState.tags.toHashSet() != selectedTags.toHashSet()) {
            tagDao.applyTags(task, tagDataDao, selectedTags)
            task.putTransitory(FORCE_CALDAV_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }

        if (!task.hasStartDate()) {
            _viewState.update { state ->
                state.copy(
                    alarms = state.alarms.filterNot { it.type == TYPE_REL_START }.toPersistentSet()
                )
            }
        }
        if (!task.hasDueDate()) {
            _viewState.update { state ->
                state.copy(
                    alarms = state.alarms.filterNot { it.type == TYPE_REL_END }.toPersistentSet()
                )
            }
        }

        if (
            (isNew && _viewState.value.alarms.isNotEmpty()) ||
            originalState.alarms != _viewState.value.alarms
        ) {
            alarmService.synchronizeAlarms(task.id, _viewState.value.alarms.toMutableSet())
            task.putTransitory(FORCE_CALDAV_SYNC, true)
            task.modificationDate = currentTimeMillis()
        }

        taskDao.save(task, null)
        val selectedList = _viewState.value.list
        if (isNew || originalState.list != selectedList) {
            task.parent = 0
            taskMover.move(listOf(task.id), selectedList)
        }

        for (subtask in viewState.newSubtasks) {
            if (Strings.isNullOrEmpty(subtask.title)) {
                continue
            }
            if (!subtask.isCompleted) {
                subtask.completionDate = task.completionDate
            }
            taskDao.createNew(subtask)
            alarmDao.insert(subtask.getDefaultAlarms())
            firebase?.addTask("subtasks")
            val filter = selectedList
            when {
                filter.isGoogleTasks -> {
                    val googleTask = CaldavTask(
                        task = subtask.id,
                        calendar = filter.uuid,
                        remoteId = null,
                    )
                    subtask.parent = task.id
                    googleTask.isMoved = true
                    googleTaskDao.insertAndShift(
                        task = subtask,
                        caldavTask = googleTask,
                        top = if (isNew) false else preferences.addTasksToTop()
                    )
                }
                else -> {
                    val caldavTask = CaldavTask(
                        task = subtask.id,
                        calendar = selectedList.uuid,
                    )
                    subtask.parent = task.id
                    caldavTask.remoteParent = caldavDao.getRemoteIdForTask(task.id)
                    taskDao.save(subtask)
                    caldavDao.insert(
                        task = subtask,
                        caldavTask = caldavTask,
                        addToTop = if (isNew) false else preferences.addTasksToTop()
                    )
                }
            }
        }

        if (originalState.attachments != _viewState.value.attachments) {
            originalState.attachments
                .minus(_viewState.value.attachments)
                .map { it.remoteId }
                .let { taskAttachmentDao.delete(task.id, it) }
            _viewState.value.attachments
                .minus(originalState.attachments)
                .map {
                    Attachment(
                        task = task.id,
                        fileId = it.id!!,
                        attachmentUid = it.remoteId,
                    )
                }
                .let { taskAttachmentDao.insert(it) }
        }

        if (task.isCompleted != _viewState.value.isCompleted) {
            taskCompleter.setComplete(task, _viewState.value.isCompleted)
            if (_viewState.value.isCompleted) {
                firebase?.completeTask("edit_screen_v2")
            }
        }

        if (isNew) {
            val model = task
            taskListEvents.emit(TaskListEvent.TaskCreated(model.uuid))
            model.calendarURI?.takeIf { it.isNotBlank() }?.let {
                taskListEvents.emit(TaskListEvent.CalendarEventCreated(model.title, it))
            }
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
        _viewState.value.calendar?.let {
            try {
                task.calendarURI = gCalHelper.createTaskEvent(task, it)?.toString()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun getRingFlags() = when {
        ringNonstop -> NOTIFY_MODE_NONSTOP
        ringFiveTimes -> NOTIFY_MODE_FIVE
        else -> 0
    }

    suspend fun delete() {
        taskDeleter.markDeleted(task)
        discard()
    }

    suspend fun discard(remove: Boolean = true) {
        if (_viewState.value.isNew) {
            timerPlugin.stopTimer(task)
            (originalState.attachments + _viewState.value.attachments)
                .onEach { attachment -> FileHelper.delete(context, attachment.uri.toUri()) }
                .let { taskAttachmentDao.delete(it.toList()) }
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

    fun removeAlarm(alarm: Alarm) {
        _viewState.update { state ->
            state.copy(alarms = state.alarms.minus(alarm).toPersistentSet())
        }
    }

    fun addAlarm(alarm: Alarm) {
        _viewState.update { state ->
            state.copy(alarms = state.alarms.plusAlarm(alarm))
        }
    }

    fun addComment(message: String?, picture: Uri?) {
        val userActivity = UserActivity()
        if (picture != null) {
            val output = FileHelper.copyToUri(context, preferences.attachmentsDirectory!!, picture)
            userActivity.setPicture(output)
        }
        userActivity.message = message
        userActivity.targetId = task.uuid
        userActivity.created = currentTimeMillis()
        viewModelScope.launch {
            withContext(NonCancellable) {
                userActivityDao.createNew(userActivity)
            }
        }
    }

    fun hideBeastModeHint(click: Boolean) {
        _viewState.update {
            it.copy(showBeastModeHint = false)
        }
        preferences.shownBeastModeHint = true
        firebase?.logEvent(R.string.event_banner_beast, R.string.param_click to click)
    }

    fun setPriority(priority: Int) {
        _viewState.update { state -> state.copy(task = state.task.copy(priority = priority)) }
    }

    fun setTitle(title: String) {
        _viewState.update { state -> state.copy(task = state.task.copy(title = title)) }
    }

    fun setRecurrence(recurrence: String?) {
        _viewState.update { state ->
            state.copy(
                task = state.task.copy(
                    recurrence = recurrence,
                    dueDate = if (recurrence?.isNotBlank() == true && task.dueDate == 0L) {
                        currentTimeMillis().startOfDay()
                    } else {
                        task.dueDate
                    }
                )
            )
        }
    }

    fun setDescription(description: String) {
        _viewState.update { state -> state.copy(task = state.task.copy(notes = description)) }
    }

    fun setList(list: CaldavFilter) {
        _viewState.update { it.copy(list = list) }
    }

    fun setTags(tags: Set<TagData>) {
        _viewState.update { it.copy(tags = tags.toPersistentSet()) }
    }

    fun setLocation(location: Location?) {
        _viewState.update { it.copy(location = location) }
    }

    fun setCalendar(calendar: String?) {
        _viewState.update { it.copy(calendar = calendar) }
    }

    fun setAttachments(attachments: Set<TaskAttachment>) {
        _viewState.update { it.copy(attachments = attachments.toPersistentSet()) }
    }

    fun setSubtasks(subtasks: List<Task>) {
        _viewState.update { it.copy(newSubtasks = subtasks.toPersistentList()) }
    }

    fun setComplete(completed: Boolean) {
        _viewState.update { state ->
            state.copy(
                task = state.task.copy(
                    completionDate = when {
                        !completed -> 0
                        task.isCompleted -> task.completionDate
                        else -> currentTimeMillis()
                    }
                )
            )
        }
    }

    fun setRepeatFrom(repeatFrom: @Task.RepeatFrom Int) {
        _viewState.update { state ->
            state.copy(task = state.task.copy(repeatFrom = repeatFrom))
        }
    }

    init {
        _originalState = _viewState.value.copy()
        viewModelScope.launch {
            taskAttachmentDao.getAttachments(task.id).toPersistentSet().let { attachments ->
                _originalState = _originalState.copy(attachments = attachments)
                _viewState.value = _viewState.value.copy(attachments = attachments)
            }
        }
    }

    companion object {
        // one spark tasks for windows adds these
        fun String?.stripCarriageReturns(): String? = this?.replace("\\r\\n?".toRegex(), "\n")

        private fun Int.isFlagSet(flag: Int): Boolean = this and flag > 0

        private fun ImmutableSet<Alarm>.plusAlarm(alarm: Alarm): ImmutableSet<Alarm> =
            if (any { it.same(alarm) }) this else this.plus(alarm).toPersistentSet()

        val TAG_TITLE = R.string.TEA_ctrl_title
        val TAG_DESCRIPTION = R.string.TEA_ctrl_notes_pref
        val TAG_CREATION = R.string.TEA_ctrl_creation_date
        val TAG_LIST = R.string.TEA_ctrl_google_task_list
        val TAG_PRIORITY = R.string.TEA_ctrl_importance_pref
        val TAG_DUE_DATE = R.string.TEA_ctrl_when_pref

        val TASK_EDIT_CONTROL_SET_FRAGMENTS = intArrayOf(
            TAG_TITLE,
            TAG_DUE_DATE,
            TimerControlSet.TAG,
            TAG_DESCRIPTION,
            CalendarControlSet.TAG,
            TAG_PRIORITY,
            StartDateControlSet.TAG,
            ReminderControlSet.TAG,
            LocationControlSet.TAG,
            FilesControlSet.TAG,
            TagsControlSet.TAG,
            RepeatControlSet.TAG,
            TAG_CREATION,
            TAG_LIST,
            SubtaskControlSet.TAG
        )
    }
}
