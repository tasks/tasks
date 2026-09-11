package org.tasks.api

import com.todoroo.astrid.alarms.AlarmService
import org.tasks.api.TasksContract.Accounts
import org.tasks.api.TasksContract.Reminders
import org.tasks.api.TasksContract.Lists
import org.tasks.api.TasksContract.Places
import org.tasks.api.TasksContract.Tags
import org.tasks.api.TasksContract.TaskTags
import org.tasks.api.TasksContract.Tasks
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.compose.pickers.startDateFollowingDue
import org.tasks.data.TaskSaver
import org.tasks.data.applicableTo
import org.tasks.data.createDueDate
import org.tasks.data.createHideUntil
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.ApiDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.SYNC_ALARMS
import org.tasks.data.entity.SYNC_LOCATION
import org.tasks.data.entity.SYNC_TAGS
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.TaskMover
import org.tasks.filters.CaldavFilter
import org.tasks.location.LocationService
import org.tasks.repeats.anchoredToDueDate
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.service.TaskCompleter
import org.tasks.service.TaskDeleter
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.time.ONE_MINUTE
import org.tasks.time.startOfDay

class ApiWriter(
    private val apiDao: ApiDao,
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val tagDao: TagDao,
    private val tagDataDao: TagDataDao,
    private val alarmDao: AlarmDao,
    private val locationDao: LocationDao,
    private val taskFactory: ApiTaskFactory,
    private val taskSaver: TaskSaver,
    private val taskCompleter: TaskCompleter,
    private val taskMover: TaskMover,
    private val taskDeleter: TaskDeleter,
    private val alarmService: AlarmService,
    private val locationService: LocationService,
    private val listManager: ApiListManager,
) {
    data class Insertion(val id: Long, val created: Boolean)

    suspend fun descendantsOf(ids: List<Long>): List<Long> = taskDao.getChildren(ids)

    suspend fun ancestorsOf(id: Long): List<Long> = taskDao.getParents(id).filter { it != id }

    suspend fun insertTask(values: ApiValues): Long {
        values.reject(Tasks.PATH, Tasks.INSERT_ONLY + Tasks.WRITABLE)
        val title = values.name(Tasks.TITLE)
            ?: throw IllegalArgumentException("${Tasks.TITLE} is required")
        val parent = values.number(Tasks.PARENT_ID) ?: 0L
        if (parent != 0L) {
            liveTask(parent) ?: throw IllegalArgumentException("${Tasks.PARENT_ID} $parent not found")
        }
        val filter = if (parent != 0L) {
            resolveListByUuid(caldavDao.getTask(parent)?.calendar)
        } else {
            resolveList(values.number(Tasks.LIST_ID))
        }
        requireWritable(filter.calendar)
        val task = taskFactory.create(title, filter) {
            it.parent = parent
            applyTaskValues(it, Task(), values)
        }
        if (parent != 0L) {
            taskMover.move(listOf(task.id), filter, parent)
        }
        values.number(Tasks.PLACE_ID)?.let { placeId ->
            taskDao.fetch(task.id)?.let { applyPlace(it, placeId, resetTriggers = true) }
        }
        values.instant(Tasks.COMPLETED_AT)?.takeIf { it > 0 }?.let { completedAt ->
            taskDao.fetch(task.id)?.let {
                taskCompleter.setComplete(it, completed = true, includeChildren = true, completedAt = completedAt)
            }
        }
        return task.id
    }

    suspend fun updateTask(id: Long, values: ApiValues): Int {
        values.reject(Tasks.PATH, Tasks.WRITABLE)
        val original = liveTask(id) ?: return 0
        requireWritable(listFor(id))

        val task = original.copy()
        applyTaskValues(task, original, values)
        task.completionDate = original.completionDate
        task.parent = original.parent
        taskSaver.save(task, original)
        dropOrphanedReminders(task, original)

        val currentListUuid = caldavDao.getTask(id)?.calendar
        val newListId = values.number(Tasks.LIST_ID)?.also {
            if (it <= 0) {
                throw IllegalArgumentException(
                    "${Tasks.LIST_ID} must name a list - a task always belongs to one"
                )
            }
        }
        val newParent = values.number(Tasks.PARENT_ID)
        if (newParent != null && newParent != 0L) {
            liveTask(newParent)
                ?: throw IllegalArgumentException("${Tasks.PARENT_ID} $newParent not found")
            if (newParent == id || id in taskDao.getParents(newParent)) {
                throw IllegalArgumentException(
                    "${Tasks.PARENT_ID} $newParent is task $id or one of its subtasks"
                )
            }
        }
        val newListUuid = newListId?.let {
            caldavDao.getCalendarById(it)?.uuid
                ?: throw IllegalArgumentException("No list with id $it")
        }
        if ((newListUuid != null && newListUuid != currentListUuid) || newParent != null) {
            val destination = if (newParent != null && newParent != 0L) {
                caldavDao.getTask(newParent)?.calendar
            } else {
                newListUuid ?: currentListUuid
            }
            val filter = resolveListByUuid(destination)
            requireWritable(filter.calendar)
            taskMover.move(listOf(id), filter, newParent ?: 0L)
            taskDao.fetch(id)?.let { touch(it) }
        }

        values.number(Tasks.PLACE_ID)?.let { placeId ->
            taskDao.fetch(id)?.let { applyPlace(it, placeId) }
        }
        values.instant(Tasks.COMPLETED_AT)?.let { completedAt ->
            val completed = completedAt > 0
            if (completed != original.isCompleted) {
                taskDao.fetch(id)?.let {
                    taskCompleter.setComplete(it, completed, includeChildren = true, completedAt = completedAt)
                }
            }
        }
        return 1
    }

    suspend fun deleteTask(id: Long): Int {
        liveTask(id) ?: return 0
        requireWritable(listFor(id))
        taskDeleter.markDeleted(listOf(id))
        return 1
    }

    private fun applyTaskValues(task: Task, original: Task, values: ApiValues) {
        values.name(Tasks.TITLE)?.let { task.title = it }
        values.text(Tasks.NOTES)?.let { task.notes = it }
        values.enum(Tasks.PRIORITY, Priorities.FROM_API, Task.Priority.NONE)?.let { task.priority = it }
        values.text(Tasks.RECURRENCE)?.let { task.recurrence = normalizeRecurrence(it) }
        values.enum(Tasks.REPEAT_FROM, RepeatFrom.FROM_API, Task.RepeatFrom.DUE_DATE)
            ?.let { task.repeatFrom = it }

        val due = values.date(Tasks.DUE_DATE)
        val dueAllDay = values.flag(Tasks.DUE_ALL_DAY) ?: due?.allDay
        if (due != null || dueAllDay != null) {
            task.dueDate = encodeDue(due?.millis ?: task.dueDate, dueAllDay ?: task.isDueAllDay())
        }
        if (task.dueDate != original.dueDate) {
            task.hideUntil = startDateFollowingDue(task.hideUntil, original.dueDate, task.dueDate)
            if (task.hasDueDate()) {
                task.recurrence = task.recurrence.anchoredToDueDate(task.dueDate)
            }
        }
        val start = values.date(Tasks.START_DATE)
        val startAllDay = values.flag(Tasks.START_ALL_DAY) ?: start?.allDay
        if (start != null || startAllDay != null) {
            task.hideUntil =
                task.encodeStart(start?.millis ?: task.hideUntil, startAllDay ?: task.isStartAllDay())
        }
        if (values.text(Tasks.RECURRENCE)?.isNotBlank() == true && !task.hasDueDate()) {
            task.dueDate = currentTimeMillis().startOfDay()
        }
        values.number(Tasks.PARENT_ID)?.let { task.parent = it }
    }

    private suspend fun dropOrphanedReminders(task: Task, original: Task) {
        val dueCleared = original.hasDueDate() && !task.hasDueDate()
        val startCleared = original.hasStartDate() && !task.hasStartDate()
        if (!dueCleared && !startCleared) {
            return
        }
        val existing = alarmDao.getAlarms(task.id)
        val kept = existing.applicableTo(task)
        if (kept.size == existing.size) {
            return
        }
        alarmService.synchronizeAlarms(task.id, kept.toMutableSet())
        markSynced(task, SYNC_ALARMS)
    }

    private suspend fun applyPlace(task: Task, placeId: Long, resetTriggers: Boolean = false) {
        val existing = locationDao.getGeofences(task.id)
        if (placeId <= 0) {
            locationDao.getGeofencesForTask(task.id).forEach {
                locationDao.delete(it)
                it.place?.let { uid -> locationService.updateGeofences(uid) }
            }
            if (existing != null) markSynced(task, SYNC_LOCATION)
            return
        }
        val placeUid = locationDao.getPlace(placeId)?.uid
            ?: throw IllegalArgumentException("No place with id $placeId")
        if (existing?.place?.uid == placeUid && !resetTriggers) {
            return
        }
        if (existing == null) {
            locationDao.insert(
                Geofence(task = task.id, place = placeUid, isArrival = false, isDeparture = false)
            )
        } else {
            locationDao.update(
                existing.geofence.copy(
                    place = placeUid,
                    isArrival = if (resetTriggers) false else existing.geofence.isArrival,
                    isDeparture = if (resetTriggers) false else existing.geofence.isDeparture,
                )
            )
            locationService.updateGeofences(existing.place.uid!!)
        }
        locationService.updateGeofences(placeUid)
        markSynced(task, SYNC_LOCATION)
    }

    private fun Task.isDueAllDay(): Boolean = hasDueDate() && !hasDueTime()

    private fun Task.isStartAllDay(): Boolean = hasStartDate() && !hasStartTime()

    private fun encodeDue(millis: Long, allDay: Boolean): Long = when {
        millis <= 0 -> 0
        allDay -> createDueDate(Task.URGENCY_SPECIFIC_DAY, millis)
        else -> createDueDate(Task.URGENCY_SPECIFIC_DAY_TIME, millis)
    }

    private fun Task.encodeStart(millis: Long, allDay: Boolean): Long = when {
        millis <= 0 -> 0
        allDay -> createHideUntil(Task.HIDE_UNTIL_SPECIFIC_DAY, millis)
        else -> createHideUntil(Task.HIDE_UNTIL_SPECIFIC_DAY_TIME, millis)
    }

    private fun normalizeRecurrence(value: String): String {
        if (value.isBlank()) {
            return ""
        }
        return try {
            newRecur(value).toString()
        } catch (e: Exception) {
            throw IllegalArgumentException("${Tasks.RECURRENCE} is not a valid RRULE: '$value'", e)
        }
    }

    suspend fun insertReminder(values: ApiValues): Long {
        values.reject(Reminders.PATH, Reminders.INSERT_ONLY + Reminders.WRITABLE)
        val taskId = values.number(Reminders.TASK_ID)
            ?: throw IllegalArgumentException("${Reminders.TASK_ID} is required")
        val task = requireLiveTask(taskId)
        requireWritable(listFor(taskId))
        val type = values.requiredEnum(Reminders.TYPE, AlarmTypes.FROM_API)
        if (AlarmTypes.isLocation(type)) {
            return insertLocationAlarm(task, values, arrival = type == Alarm.TYPE_GEO_ENTER)
        }
        if (type == Alarm.TYPE_REL_END && !task.hasDueDate()) {
            throw IllegalArgumentException(
                "Task $taskId has no due date, so a ${Reminders.TYPE_RELATIVE_DUE} reminder would " +
                        "never fire. Set ${Tasks.DUE_DATE} first."
            )
        }
        if (type == Alarm.TYPE_REL_START && !task.hasStartDate()) {
            throw IllegalArgumentException(
                "Task $taskId has no start date, so a ${Reminders.TYPE_RELATIVE_START} reminder " +
                        "would never fire. Set ${Tasks.START_DATE} first."
            )
        }
        values.number(Reminders.PLACE_ID)?.takeIf { it != 0L }?.let {
            throw IllegalArgumentException(
                "${Reminders.PLACE_ID} only applies to a" +
                        " ${Reminders.TYPE_LOCATION_ARRIVAL} or ${Reminders.TYPE_LOCATION_DEPARTURE} reminder"
            )
        }
        val alarm = Alarm(
            task = taskId,
            time = values.alarmTime(type, null),
            type = type,
            repeat = values.number(Reminders.REPEAT_COUNT)?.toInt() ?: 0,
            interval = values.number(Reminders.INTERVAL_MS) ?: 0,
        ).requireCanFire()
        val existing = alarmDao.getAlarms(taskId)
        existing.firstOrNull { it.same(alarm) }?.let { return it.id }
        alarmService.synchronizeAlarms(taskId, (existing + alarm).toMutableSet())
        markSynced(task, SYNC_ALARMS)
        return alarmDao.getAlarms(taskId).first { it.same(alarm) }.id
    }

    suspend fun updateReminder(id: Long, values: ApiValues): Int {
        values.reject(Reminders.PATH, Reminders.WRITABLE)
        if (Reminders.isLocationId(id)) {
            throw IllegalArgumentException(
                "A location reminder has nothing to update - it is identified entirely by its" +
                        " task, place and direction. Delete it and insert the other type instead."
            )
        }
        val existing = apiDao.getAlarm(id) ?: return 0
        val task = liveTask(existing.task) ?: return 0
        requireWritable(listFor(existing.task))
        val updated = existing.copy(
            time = values.alarmTime(existing.type, existing.time),
            repeat = values.number(Reminders.REPEAT_COUNT)?.toInt() ?: existing.repeat,
            interval = values.number(Reminders.INTERVAL_MS) ?: existing.interval,
        ).requireCanFire()
        if (updated == existing) {
            return 1
        }
        val alarms = alarmDao.getAlarms(existing.task).filterNot { it.id == id } + updated
        alarmService.synchronizeAlarms(existing.task, alarms.toMutableSet())
        markSynced(task, SYNC_ALARMS)
        return 1
    }

    private fun Alarm.requireCanFire(): Alarm {
        val apiType = AlarmTypes.toApi(type)
        if (type in AlarmTypes.ABSOLUTE && time <= 0) {
            throw IllegalArgumentException("${Reminders.TRIGGER_AT} is required for a $apiType reminder")
        }
        if (type == Alarm.TYPE_RANDOM && time <= 0) {
            throw IllegalArgumentException(
                "${Reminders.OFFSET_MS} must be positive for a $apiType reminder - it is how often it fires"
            )
        }
        if (type == Alarm.TYPE_RANDOM && time < ONE_MINUTE) {
            throw IllegalArgumentException(
                "${Reminders.OFFSET_MS} is in milliseconds; the shortest $apiType period is a minute ($ONE_MINUTE)"
            )
        }
        if (repeat < 0 || interval < 0) {
            throw IllegalArgumentException(
                "${Reminders.REPEAT_COUNT} and ${Reminders.INTERVAL_MS} must not be negative"
            )
        }
        if (repeat > 0 && interval <= 0) {
            throw IllegalArgumentException(
                "${Reminders.INTERVAL_MS} is required when ${Reminders.REPEAT_COUNT} is set"
            )
        }
        if (repeat > 0 && interval < ONE_MINUTE) {
            throw IllegalArgumentException(
                "${Reminders.INTERVAL_MS} is in milliseconds; the shortest repeat is a minute ($ONE_MINUTE)"
            )
        }
        return this
    }

    suspend fun reminderOwner(id: Long): Long? =
        Reminders.decodeLocationId(id)?.let { apiDao.getGeofence(it.geofenceId)?.task }
            ?: apiDao.getAlarm(id)?.task

    suspend fun requireTag(id: Long) {
        apiDao.getTag(id) ?: throw IllegalArgumentException("No tag with id $id")
    }

    suspend fun deleteReminder(id: Long): Int {
        Reminders.decodeLocationId(id)?.let { return deleteLocationAlarm(it) }
        val existing = apiDao.getAlarm(id) ?: return 0
        val task = liveTask(existing.task) ?: return 0
        requireWritable(listFor(existing.task))
        val alarms = alarmDao.getAlarms(existing.task).filterNot { it.id == id }
        alarmService.synchronizeAlarms(existing.task, alarms.toMutableSet())
        markSynced(task, SYNC_ALARMS)
        return 1
    }

    private fun ApiValues.alarmTime(type: Int, current: Long?): Long {
        val absolute = type in AlarmTypes.ABSOLUTE
        val wrong = if (absolute) Reminders.OFFSET_MS else Reminders.TRIGGER_AT
        val right = if (absolute) Reminders.TRIGGER_AT else Reminders.OFFSET_MS
        timing(wrong)?.takeIf { it != 0L }?.let {
            throw IllegalArgumentException(
                "$wrong does not apply to a ${AlarmTypes.toApi(type)} reminder; use $right"
            )
        }
        if (absolute) {
            listOf(Reminders.REPEAT_COUNT, Reminders.INTERVAL_MS).forEach { key ->
                number(key)?.takeIf { it != 0L }?.let {
                    throw IllegalArgumentException(
                        "$key does not apply to a ${AlarmTypes.toApi(type)} reminder"
                    )
                }
            }
        }
        return timing(right) ?: current ?: 0L
    }

    private fun ApiValues.timing(key: String): Long? =
        if (key == Reminders.TRIGGER_AT) instant(key) else number(key)

    suspend fun insertTaskTag(values: ApiValues): Long {
        values.reject(TaskTags.PATH, TaskTags.INSERT_ONLY)
        val taskId = values.number(TaskTags.TASK_ID)
            ?: throw IllegalArgumentException("${TaskTags.TASK_ID} is required")
        val tagId = values.number(TaskTags.TAG_ID)
            ?: throw IllegalArgumentException("${TaskTags.TAG_ID} is required")
        val task = requireLiveTask(taskId)
        requireWritable(listFor(taskId))
        val tagData = apiDao.getTag(tagId)
            ?: throw IllegalArgumentException("No tag with id $tagId")
        val tagUid = tagData.remoteId
            ?: throw IllegalArgumentException("Tag $tagId has no identifier")
        tagDao.getTagByTaskAndTagUid(taskId, tagUid)?.let { return it.id }
        tagDao.insert(task, listOf(tagData))
        markSynced(task, SYNC_TAGS)
        return tagDao.getTagByTaskAndTagUid(taskId, tagUid)!!.id
    }

    suspend fun deleteTaskTag(id: Long): Int {
        val tag = apiDao.getTaskTag(id) ?: return 0
        return deleteTaskTagByUid(tag.task, tag.tagUid.orEmpty())
    }

    suspend fun deleteTaskTag(taskId: Long, tagId: Long): Int {
        val tagUid = apiDao.getTag(tagId)?.remoteId ?: return 0
        return deleteTaskTagByUid(taskId, tagUid)
    }

    private suspend fun deleteTaskTagByUid(taskId: Long, tagUid: String): Int {
        val task = liveTask(taskId) ?: return 0
        requireWritable(listFor(taskId))
        val tag = tagDao.getTagByTaskAndTagUid(taskId, tagUid) ?: return 0
        tagDao.delete(listOf(tag))
        markSynced(task, SYNC_TAGS)
        return 1
    }

    private suspend fun insertLocationAlarm(
        task: Task,
        values: ApiValues,
        arrival: Boolean,
    ): Long {
        val apiType = if (arrival) Reminders.TYPE_LOCATION_ARRIVAL else Reminders.TYPE_LOCATION_DEPARTURE
        listOf(Reminders.TRIGGER_AT, Reminders.OFFSET_MS, Reminders.REPEAT_COUNT, Reminders.INTERVAL_MS)
            .forEach { key ->
                values.timing(key)?.takeIf { it != 0L }?.let {
                    throw IllegalArgumentException("$key does not apply to a $apiType reminder")
                }
            }
        val placeId = values.number(Reminders.PLACE_ID)?.takeIf { it != 0L }
            ?: throw IllegalArgumentException("${Reminders.PLACE_ID} is required for a $apiType reminder")
        val placeUid = locationDao.getPlace(placeId)?.uid
            ?: throw IllegalArgumentException("No place with id $placeId")

        val existing = locationDao.getGeofencesForTask(task.id).firstOrNull()
        if (existing != null && existing.place != placeUid) {

            throw IllegalArgumentException(
                "Task ${task.id} is already at another place, and a task can only be at one." +
                        " Set ${Tasks.PLACE_ID} on the task first to move it."
            )
        }

        val id: Long
        if (existing == null) {
            id = locationDao.insert(
                Geofence(
                    task = task.id,
                    place = placeUid,
                    isArrival = arrival,
                    isDeparture = !arrival,
                )
            )
        } else {
            id = existing.id
            val updated = if (arrival) {
                existing.copy(isArrival = true)
            } else {
                existing.copy(isDeparture = true)
            }
            if (updated == existing) {
                return Reminders.encodeLocationId(id, arrival)
            }
            locationDao.update(updated)
        }
        locationService.updateGeofences(placeUid)
        markSynced(task, SYNC_LOCATION)
        return Reminders.encodeLocationId(id, arrival)
    }

    private suspend fun deleteLocationAlarm(id: Reminders.LocationId): Int {
        val existing = apiDao.getGeofence(id.geofenceId) ?: return 0
        val task = liveTask(existing.task) ?: return 0
        requireWritable(listFor(existing.task))
        val wasSet = if (id.arrival) existing.isArrival else existing.isDeparture
        if (!wasSet) return 0
        val updated = if (id.arrival) {
            existing.copy(isArrival = false)
        } else {
            existing.copy(isDeparture = false)
        }
        locationDao.update(updated)
        existing.place?.let { locationService.updateGeofences(it) }
        markSynced(task, SYNC_LOCATION)
        return 1
    }

    suspend fun insertList(values: ApiValues): Long {
        values.reject(Lists.PATH, Lists.INSERT_ONLY + Lists.WRITABLE)
        val accountId = values.number(Lists.ACCOUNT_ID)
            ?: throw IllegalArgumentException("${Lists.ACCOUNT_ID} is required")
        val title = values.name(Lists.TITLE)
            ?: throw IllegalArgumentException("${Lists.TITLE} is required")
        val account = caldavDao.getAccount(accountId)
            ?: throw IllegalArgumentException("No account with id $accountId")
        val created = listManager.create(
            account = account,
            title = title,
            color = values.number(Lists.COLOR)?.toInt() ?: 0,
            icon = values.text(Lists.ICON).orEmpty(),
        )
        return created.id
    }

    suspend fun updateList(id: Long, values: ApiValues): Int {
        values.reject(Lists.PATH, Lists.WRITABLE)
        val calendar = caldavDao.getCalendarById(id) ?: return 0
        requireWritable(calendar)
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return 0
        listManager.update(
            account = account,
            calendar = calendar,
            title = values.name(Lists.TITLE) ?: calendar.name.orEmpty(),
            color = values.number(Lists.COLOR)?.toInt() ?: calendar.color,
            icon = values.text(Lists.ICON) ?: calendar.icon.orEmpty(),
        )
        return 1
    }

    suspend fun deleteList(id: Long): Int {
        val calendar = caldavDao.getCalendarById(id) ?: return 0
        requireWritable(calendar)
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) } ?: return 0
        listManager.delete(account, calendar)
        return 1
    }

    suspend fun insertTag(values: ApiValues): Insertion {
        values.reject(Tags.PATH, Tags.WRITABLE)
        val name = values.name(Tags.NAME)
            ?: throw IllegalArgumentException("${Tags.NAME} is required")
        tagDataDao.getTagByName(name)?.let { return Insertion(it.id!!, created = false) }
        val created = tagDataDao.createDirty(
            TagData(
                name = name,
                color = values.number(Tags.COLOR)?.toInt() ?: 0,
                icon = values.text(Tags.ICON)?.takeIf { it.isNotEmpty() },
            )
        ) ?: tagDataDao.getTagByName(name)
        return Insertion(created?.id ?: throw IllegalStateException("Failed to create tag"), created = true)
    }

    suspend fun updateTag(id: Long, values: ApiValues): Int {
        values.reject(Tags.PATH, Tags.WRITABLE)
        val tag = apiDao.getTag(id) ?: return 0
        val remoteId = tag.remoteId ?: return 0
        val name = values.name(Tags.NAME) ?: tag.name.orEmpty()
        val color = values.number(Tags.COLOR)?.toInt() ?: tag.color ?: 0
        val icon = values.text(Tags.ICON) ?: tag.icon
        val changed = tagDataDao.editTag(
            remoteId = remoteId,
            name = name,
            color = color,
            icon = icon?.takeIf { it.isNotEmpty() },
            nameChanged = name != tag.name,
            colorChanged = color != (tag.color ?: 0),
            iconChanged = icon != tag.icon,
            order = tag.order,
        )
        if (!changed) {
            val clash = tagDataDao.getTagByName(name)
            throw IllegalArgumentException(
                "A tag named '$name' already exists" +
                        (clash?.id?.let { " with id $it" } ?: "")
            )
        }
        return 1
    }

    suspend fun deleteTag(id: Long): Int {
        val tag = apiDao.getTag(id) ?: return 0
        tagDataDao.deleteWithTombstone(tag)
        return 1
    }

    suspend fun insertPlace(values: ApiValues): Insertion {
        values.reject(Places.PATH, Places.INSERT_ONLY + Places.WRITABLE)
        val latitude = values.decimal(Places.LATITUDE)
            ?: throw IllegalArgumentException("${Places.LATITUDE} is required")
        val longitude = values.decimal(Places.LONGITUDE)
            ?: throw IllegalArgumentException("${Places.LONGITUDE} is required")
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            throw IllegalArgumentException(
                "$latitude, $longitude is not on the map: ${Places.LATITUDE} runs -90 to 90 and " +
                        "${Places.LONGITUDE} -180 to 180"
            )
        }
        locationDao.findPlace(latitude.toLikeString(), longitude.toLikeString())?.let {
            return Insertion(it.id, created = false)
        }
        val place = Place(
            name = values.text(Places.NAME)?.trim()?.takeIf { it.isNotEmpty() },
            address = values.text(Places.ADDRESS),
            phone = values.text(Places.PHONE),
            url = values.text(Places.URL),
            latitude = latitude,
            longitude = longitude,
            radius = values.radius() ?: DEFAULT_RADIUS,
            color = values.number(Places.COLOR)?.toInt() ?: 0,
            icon = values.text(Places.ICON)?.takeIf { it.isNotEmpty() },
        )
        return Insertion(locationDao.insert(place), created = true)
    }

    suspend fun updatePlace(id: Long, values: ApiValues): Int {
        values.reject(Places.PATH, Places.WRITABLE)
        val place = locationDao.getPlace(id) ?: return 0
        val updated = place.copy(
            name = values.text(Places.NAME)?.trim() ?: place.name,
            address = values.text(Places.ADDRESS) ?: place.address,
            phone = values.text(Places.PHONE) ?: place.phone,
            url = values.text(Places.URL) ?: place.url,
            radius = values.radius() ?: place.radius,
            color = values.number(Places.COLOR)?.toInt() ?: place.color,
            icon = values.text(Places.ICON) ?: place.icon,
        )
        locationDao.update(updated)
        place.uid?.let { locationService.updateGeofences(it) }
        return 1
    }

    private fun ApiValues.radius(): Int? = number(Places.RADIUS)?.toInt()?.also {
        if (it <= 0) throw IllegalArgumentException("${Places.RADIUS} must be a positive number of metres, was $it")
    }

    suspend fun deletePlace(id: Long): Int {
        val place = locationDao.getPlace(id) ?: return 0
        place.uid?.let { locationDao.deleteGeofencesByPlace(it) }
        locationDao.delete(place)
        locationService.updateGeofences(place)
        return 1
    }

    private suspend fun liveTask(id: Long): Task? = taskDao.fetch(id)?.takeIf { !it.isDeleted }

    private suspend fun requireLiveTask(id: Long): Task =
        liveTask(id) ?: throw IllegalArgumentException("No task with id $id")

    private suspend fun listFor(taskId: Long): CaldavCalendar? =
        caldavDao.getTask(taskId)?.calendar?.let { caldavDao.getCalendar(it) }

    private fun requireWritable(calendar: CaldavCalendar?) {
        if (calendar?.readOnly() == true) {
            throw UnsupportedOperationException("'${calendar.name}' is read-only")
        }
    }

    private suspend fun resolveList(id: Long?): CaldavFilter {
        if (id == null) {
            return taskFactory.defaultList()
        }
        if (id <= 0) {
            throw IllegalArgumentException(
                "${Tasks.LIST_ID} must name a list - a task always belongs to one"
            )
        }
        val calendar = caldavDao.getCalendarById(id)
            ?: throw IllegalArgumentException("No list with id $id")
        return filterFor(calendar)
    }

    private suspend fun resolveListByUuid(uuid: String?): CaldavFilter {
        if (uuid.isNullOrEmpty()) {
            return taskFactory.defaultList()
        }
        val calendar = caldavDao.getCalendar(uuid)
            ?: throw IllegalArgumentException("No list '$uuid'")
        return filterFor(calendar)
    }

    private suspend fun filterFor(calendar: CaldavCalendar): CaldavFilter {
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) }
            ?: throw IllegalArgumentException("List '${calendar.name}' has no account")
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun markSynced(task: Task, trait: String) {
        task.putTransitory(trait, true)
        taskSaver.save(task, task.copy())
    }

    private suspend fun touch(task: Task) {
        val original = task.copy()
        task.modificationDate = currentTimeMillis()
        taskSaver.save(task, original)
    }

    companion object {
        private const val DEFAULT_RADIUS = 250
    }
}
