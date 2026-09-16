package org.tasks.caldav

import co.touchlab.kermit.Logger
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.datetime.TimeZone
import org.tasks.TasksBuildConfig
import org.tasks.caldav.GeoUtils.equalish
import org.tasks.caldav.GeoUtils.toGeo
import org.tasks.caldav.toLikeString
import org.tasks.caldav.extensions.toAlarms
import org.tasks.caldav.extensions.toVAlarms
import org.tasks.data.TaskSaver
import org.tasks.data.createDueDate
import org.tasks.data.createGeofence
import org.tasks.data.createHideUntil
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.DirtyDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import org.tasks.data.entity.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY_TIME
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import org.tasks.data.getDefaultAlarms
import org.tasks.data.setDefaultReminders
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.icalendar.Geo
import org.tasks.icalendar.ICalDate
import org.tasks.icalendar.ICalProperty
import org.tasks.icalendar.RelatedTo
import org.tasks.icalendar.TodoStatus
import org.tasks.icalendar.Trigger
import org.tasks.icalendar.VAlarm
import org.tasks.icalendar.VTodo
import org.tasks.icalendar.formatICalUtc
import org.tasks.icalendar.localMillis
import org.tasks.icalendar.parseICalDateTime
import org.tasks.icalendar.parseVTodos
import org.tasks.icalendar.serialize
import org.tasks.icalendar.toICalDate
import org.tasks.icalendar.toICalDateTime
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.notifications.CancelReason
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.repeats.Recur
import org.tasks.time.DateTime
import org.tasks.time.ONE_DAY
import org.tasks.time.startOfDay
import org.tasks.time.startOfMinute
import kotlin.math.max
import kotlin.math.min

@Suppress("ClassName")
class iCalendar(
    private val tagDataDao: TagDataDao,
    private val preferences: AppPreferences,
    private val locationDao: LocationDao,
    private val geocoder: Geocoder,
    private val locationService: LocationService,
    private val tagDao: TagDao,
    private val taskDao: TaskDao,
    private val dirtyDao: DirtyDao,
    private val taskSaver: TaskSaver,
    private val caldavDao: CaldavDao,
    private val alarmDao: AlarmDao,
    private val alarmService: AlarmService,
    private val vtodoCache: VtodoCache,
    private val notifier: Notifier,
) {

    suspend fun setPlace(taskId: Long, geo: Geo?) {
        if (geo == null) {
            locationDao.getActiveGeofences(taskId).forEach {
                locationDao.delete(it.geofence)
                locationService.updateGeofences(it.place)
            }
            return
        }
        var place: Place? = locationDao.findPlace(
                geo.latitude.toLikeString(),
                geo.longitude.toLikeString()
        )
        if (place == null) {
            place = Place(
                latitude = geo.latitude,
                longitude = geo.longitude,
            ).let {
                it.copy(id = locationDao.insert(it))
            }
            try {
                geocoder.reverseGeocode(
                    MapPosition(place.latitude, place.longitude)
                )?.takeIf { place.distanceTo(it) <= 100 }
                ?.let { result ->
                    place = place.copy(
                        name = result.name,
                        address = result.address,
                        phone = result.phone,
                        url = result.url,
                    )
                    locationDao.update(place)
                }
            } catch (e: Exception) {
                Logger.e(e) { e.message.orEmpty() }
            }
        }
        val existing = locationDao.getGeofences(taskId)
        if (existing == null) {
            locationDao.insert(
                createGeofence(
                    place.uid,
                    preferences
                ).copy(task = taskId)
            )
        } else if (place != existing.place) {
            val geofence = existing.geofence.copy(place = place.uid)
            locationDao.update(geofence)
            locationService.updateGeofences(existing.place)
        }
        locationService.updateGeofences(place)
    }

    suspend fun toVtodo(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        caldavTask: CaldavTask,
        task: org.tasks.data.entity.Task
    ): ByteArray {
        var remoteModel: VTodo? = null
        try {
            val vtodo = vtodoCache.getVtodo(calendar, caldavTask)
            if (vtodo?.isNotBlank() == true) {
                remoteModel = fromVtodo(vtodo)
            }
        } catch (e: Exception) {
            Logger.e(e) { e.message.orEmpty() }
        }
        if (remoteModel == null) {
            remoteModel = VTodo()
        }

        return toVtodo(account, caldavTask, task, remoteModel)
    }

    suspend fun applyLocalTo(
        account: CaldavAccount,
        caldavTask: CaldavTask,
        task: org.tasks.data.entity.Task,
        remoteModel: VTodo
    ) {
        remoteModel.applyLocal(caldavTask, task)
        val categories = remoteModel.categories
        categories.clear()
        categories.addAll(tagDataDao.getTagDataForTask(task.id).map { it.name!! })
        if (TasksBuildConfig.DEBUG && caldavTask.remoteId.isNullOrBlank()) {
            throw IllegalStateException()
        }
        remoteModel.uid = caldavTask.remoteId
        val location = locationDao.getGeofences(task.id)
        val localGeo = toGeo(location)
        if (localGeo == null || !localGeo.equalish(remoteModel.geoPosition)) {
            remoteModel.geoPosition = localGeo
        }
        remoteModel.lastAck = max(task.reminderDismissed, remoteModel.lastAck ?: 0)
        if (account.reminderSync) {
            remoteModel.alarms.removeAll(remoteModel.alarms.filtered)
            val alarms = alarmDao.getAlarms(task.id)
            remoteModel.snooze = alarms.find { it.type == TYPE_SNOOZE }?.time
            remoteModel.alarms.addAll(alarms.toVAlarms())
        }
    }

    suspend fun toVtodo(
        account: CaldavAccount,
        caldavTask: CaldavTask,
        task: org.tasks.data.entity.Task,
        remoteModel: VTodo
    ): ByteArray {
        applyLocalTo(account, caldavTask, task, remoteModel)
        return remoteModel.serialize().encodeToByteArray()
    }

    suspend fun fromVtodo(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        existing: CaldavTask?,
        remote: VTodo,
        vtodo: String?,
        obj: String? = null,
        eTag: String? = null
    ) {
        if (existing?.isDeleted() == true) {
            return
        }
        val task = existing?.task
            ?.let { taskDao.fetch(it) }
            ?: org.tasks.data.entity.Task(
                readOnly = calendar.readOnly(),
                priority = preferences.defaultPriority(),
            ).also {
                taskDao.createNew(it)
            }
        val caldavTask =
            existing
                ?.copy(task = task.id, obj = existing.obj?.takeIf { it.isNotBlank() } ?: obj)
                ?: CaldavTask(
                    task = task.id,
                    calendar = calendar.uuid,
                    remoteId = remote.uid,
                    obj = obj,
                )
        val isNew = caldavTask.id == org.tasks.data.entity.Task.NO_ID
        val dirty = !isNew && dirtyDao.isDirty(caldavTask.id) == true
        val local = if (account.isOpenTasks) {
            null
        } else {
            vtodoCache.getVtodo(calendar, caldavTask)?.let { fromVtodo(it) }
        }
        if (dirty && local == null) {
            if (!account.isOpenTasks) vtodoCache.putVtodo(calendar, caldavTask, vtodo)
            caldavTask.etag = eTag
            caldavDao.update(caldavTask)
            return
        }
        val original = task.copy()
        task.applyRemote(remote, local)
        caldavTask.applyRemote(remote, local)
        val remoteModificationDate = task.modificationDate

        val remoteAck = remote.lastAck ?: 0
        if (task.isCompleted) {
            notifier.cancel(task.id, CancelReason.REMOTE_COMPLETION)
        } else if (task.isDeleted) {
            notifier.cancel(task.id, CancelReason.REMOTE_DELETION)
        } else if (acknowledgesLastReminder(remoteAck, task.reminderLast)) {
            notifier.cancel(task.id, CancelReason.REMOTE_CLEAR)
        }
        task.reminderDismissed = max(task.reminderDismissed, remoteAck)
        task.reminderLast = max(task.reminderLast, remoteAck)

        if (local != null) {
            val place = locationDao.getPlaceForTask(task.id)
            if (place?.toGeo() == local.geoPosition) {
                setPlace(task.id, remote.geoPosition)
            }
        } else {
            setPlace(task.id, remote.geoPosition)
        }

        if (local != null) {
            val current = tagDataDao.getTagDataForTask(task.id).mapNotNull { it.name }
            tagDao.applyTags(
                task,
                mergeCategories(base = local.categories, local = current, remote = remote.categories),
            )
        } else {
            tagDao.applyTags(task, remote.categories)
        }

        if (
            isNew &&
            remote.reminders.isEmpty() &&
            !calendar.ctag.isNullOrBlank() && // not initial sync
            vtodo?.prodId()?.supportsReminders() != true // other client doesn't support reminder sync
        ) {
            task.setDefaultReminders(preferences)
            alarmService.synchronizeAlarms(task.id, task.getDefaultAlarms(preferences.isDefaultDueTimeEnabled()).toMutableSet())
        } else if (account.reminderSync) {
            val localAlarms = alarmDao.getAlarms(task.id).map { it.copy(id = 0, task = 0) }
            if (account.isOpenTasks) {
                val randomReminders = localAlarms.filter { it.type == Alarm.TYPE_RANDOM }
                if (localAlarms.toSet() == randomReminders.toSet()) {
                    alarmService.synchronizeAlarms(
                        caldavTask.task,
                        remote.reminders.plus(randomReminders).toMutableSet(),
                    )
                }
            } else {
                val merged = mergeReminders(
                    base = local?.reminders.orEmpty(),
                    local = localAlarms,
                    remote = remote.reminders,
                )
                alarmService.synchronizeAlarms(caldavTask.task, merged.toMutableSet())
            }
        }

        task.suppressSync()
        task.suppressRefresh()
        if (!dirty) {
            task.modificationDate = remoteModificationDate
        }
        taskSaver.save(task, original, dirty = false)
        if (!account.isOpenTasks) vtodoCache.putVtodo(calendar, caldavTask, vtodo)
        caldavTask.etag = eTag
        if (dirty) {
            // Keep the task dirty so the merged result is pushed next sync; do NOT markSynced (it
            // would clear a never-pushed (dirty_version=1, synced_version=0) row). dirty implies the
            // caldav row already exists, so a plain update is correct.
            caldavDao.update(caldavTask)
        } else {
            caldavDao.insertOrUpdateAndMarkSynced(caldavTask)
        }
    }

    companion object {
        internal fun acknowledgesLastReminder(remoteAck: Long, reminderLast: Long): Boolean =
            reminderLast > 0 && remoteAck >= reminderLast.startOfMinute()

        private const val APPLE_SORT_ORDER = "X-APPLE-SORT-ORDER"
        private const val OC_HIDESUBTASKS = "X-OC-HIDESUBTASKS"
        private const val MOZ_SNOOZE_TIME = "X-MOZ-SNOOZE-TIME"
        private const val MOZ_LASTACK = "X-MOZ-LASTACK"
        private const val HIDE_SUBTASKS = "1"
        private val PRODID_MATCHER = Regex(".*?PRODID:(.*?)\n.*", RegexOption.DOT_MATCHES_ALL)
        private val IGNORE_ALARM = Trigger.Absolute(parseICalDateTime("19760401T005545Z")!!)
        private val IS_PARENT = { r: RelatedTo -> r.relType == "PARENT" || r.relType.isNullOrBlank() }

        fun ICalDate?.applyDue(task: org.tasks.data.entity.Task) {
            task.dueDate = toDueMillis()
        }

        fun ICalDate?.toDueMillis(): Long = when (this) {
            null -> 0
            is ICalDate.DateTime -> createDueDate(URGENCY_SPECIFIC_DAY_TIME, millis)
            is ICalDate.Date -> createDueDate(URGENCY_SPECIFIC_DAY, localMillis())
        }

        fun ICalDate?.matchesDue(task: org.tasks.data.entity.Task): Boolean = when (this) {
            null -> task.dueDate == 0L
            is ICalDate.DateTime -> toDueMillis() == task.dueDate
            is ICalDate.Date -> !task.hasDueTime() && task.dueDate > 0 && this == task.dueDate.startOfDay().toICalDate()
        }

        fun ICalDate?.applyStart(task: org.tasks.data.entity.Task) {
            task.hideUntil = toStartMillis(task)
        }

        fun ICalDate?.toStartMillis(task: org.tasks.data.entity.Task): Long = when (this) {
            null -> 0
            is ICalDate.DateTime -> task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY_TIME, millis)
            is ICalDate.Date -> task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY, localMillis())
        }

        fun ICalDate?.matchesStart(task: org.tasks.data.entity.Task): Boolean = when (this) {
            null -> task.hideUntil == 0L
            is ICalDate.DateTime -> toStartMillis(task) == task.hideUntil
            is ICalDate.Date -> !task.hasStartTime() && task.hideUntil > 0 && this == (task.hideUntil + ONE_DAY / 2).toICalDate()
        }

        fun String.supportsReminders(): Boolean {
            if (contains(NEXTCLOUD_TASKS)) {
                val (major, minor) = NEXTCLOUD_TASKS_VERSION.find(this)?.destructured
                    ?: return true
                return major.toInt() > 0 || minor.toInt() >= 17
            }
            return CLIENTS_WITH_REMINDER_SYNC.any { contains(it) }
        }

        fun String.prodId(): String? = PRODID_MATCHER.matchEntire(this)?.groupValues?.get(1)

        private const val NEXTCLOUD_TASKS = "Nextcloud Tasks"
        private val NEXTCLOUD_TASKS_VERSION = "Nextcloud Tasks v(\\d+)\\.(\\d+)".toRegex()

        private val CLIENTS_WITH_REMINDER_SYNC = listOf(
            "tasks.org",
            "Mozilla.org",
            "Apple Inc.",
        )

        fun fromVtodo(vtodo: String): VTodo? {
            try {
                val tasks = parseVTodos(vtodo)
                if (tasks.size == 1) {
                    return tasks[0]
                }
            } catch (e: Exception) {
                Logger.e(e) { e.message.orEmpty() }
            }
            return null
        }

        var VTodo.parent: String?
            get() = relatedTo.find(IS_PARENT)?.uid
            set(value) {
                val parents = relatedTo.filter(IS_PARENT)
                when {
                    value.isNullOrBlank() -> relatedTo.removeAll(parents)
                    parents.isEmpty() -> relatedTo.add(RelatedTo(value))
                    else -> {
                        relatedTo.removeAll(parents)
                        relatedTo.add(RelatedTo(value, "PARENT"))
                    }
                }
            }

        var VTodo.order: Long?
            get() = unknownProperty(APPLE_SORT_ORDER)?.toLongOrNull()
            set(order) {
                setUnknownProperty(APPLE_SORT_ORDER, order?.toString())
            }

        var VTodo.collapsed: Boolean
            get() = unknownProperty(OC_HIDESUBTASKS) == HIDE_SUBTASKS
            set(collapsed) {
                setUnknownProperty(OC_HIDESUBTASKS, HIDE_SUBTASKS.takeIf { collapsed })
            }

        var VTodo.lastAck: Long?
            get() = unknownProperty(MOZ_LASTACK)?.let { parseICalDateTime(it) }
            set(value) {
                value?.takeIf { it > 0 }?.let { setUnknownProperty(MOZ_LASTACK, formatICalUtc(it)) }
            }

        var VTodo.snooze: Long?
            get() = unknownProperty(MOZ_SNOOZE_TIME)?.let { parseICalDateTime(it) }
            set(value) {
                setUnknownProperty(MOZ_SNOOZE_TIME, value?.takeIf { DateTime(it).isAfterNow }?.let { formatICalUtc(it) })
            }

        private fun VTodo.unknownProperty(name: String): String? =
            unknownProperties.find { it.name.equals(name, ignoreCase = true) }?.value

        private fun VTodo.setUnknownProperty(name: String, value: String?) {
            val index = unknownProperties.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            when {
                value == null -> unknownProperties.removeAll { it.name.equals(name, ignoreCase = true) }
                index < 0 -> unknownProperties.add(ICalProperty(name, value))
                else -> unknownProperties[index] = unknownProperties[index].copy(value = value)
            }
        }

        fun VTodo.applyLocal(caldavTask: CaldavTask, task: org.tasks.data.entity.Task) {
            createdAt = newDateTime(task.creationDate).toUTC().millis
            summary = task.title
            description = task.notes
            val allDay = !task.hasDueTime() && !task.hasStartTime()
            val dueDate = if (task.hasDueTime()) task.dueDate else task.dueDate.startOfDay()
            var startDate = if (task.hasStartTime()) {
                task.hideUntil.startOfMinute()
            } else {
                task.hideUntil.startOfDay()
            }
            due = if (dueDate > 0) {
                startDate = min(dueDate, startDate)
                if (allDay) dueDate.toICalDate() else dueDate.toICalDateTime()
            } else {
                null
            }
            dtStart = if (startDate > 0) {
                if (allDay) startDate.toICalDate() else startDate.toICalDateTime()
            } else {
                null
            }
            if (task.isCompleted) {
                completedAt = task.completionDate
                status = TodoStatus.COMPLETED
                percentComplete = 100
            } else if (completedAt != null) {
                completedAt = null
                status = null
                percentComplete = null
            }
            rRule = if (task.isRecurring) {
                try {
                    Recur.parse(task.recurrence!!)
                } catch (e: IllegalArgumentException) {
                    Logger.e(e) { e.message.orEmpty() }
                    null
                }
            } else {
                null
            }
            lastModified = newDateTime(task.modificationDate).toUTC().millis
            priority = when (task.priority) {
                org.tasks.data.entity.Task.Priority.NONE -> 0
                org.tasks.data.entity.Task.Priority.MEDIUM -> 5
                org.tasks.data.entity.Task.Priority.HIGH ->
                    if (priority < 5) max(1, priority) else 1
                else -> if (priority > 5) min(9, priority) else 9
            }
            parent = caldavTask.remoteParent?.takeIf { it.isNotBlank() }
            order = task.order
            collapsed = task.isCollapsed
        }

        val List<VAlarm>.filtered: List<VAlarm>
            get() = filter { it.action == "DISPLAY" || it.action == "AUDIO" }.filterNot { it.trigger == IGNORE_ALARM }

        val VTodo.reminders: List<Alarm>
            get() = alarms.filtered.toAlarms().let { alarms ->
                snooze?.let { time -> alarms.plus(Alarm(time = time, type = TYPE_SNOOZE))} ?: alarms
            }
    }
}
