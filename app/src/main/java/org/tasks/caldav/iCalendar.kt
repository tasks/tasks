package org.tasks.caldav

import at.bitfire.ical4android.Task
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import at.bitfire.ical4android.util.DateUtils.ical4jTimeZone
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.alarms.AlarmService
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_SPECIFIC_DAY_TIME
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY
import com.todoroo.astrid.data.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import com.todoroo.astrid.service.TaskCreator
import com.todoroo.astrid.service.TaskCreator.Companion.getDefaultAlarms
import com.todoroo.astrid.service.TaskCreator.Companion.setDefaultReminders
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DateProperty
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.XProperty
import org.tasks.BuildConfig
import org.tasks.caldav.GeoUtils.equalish
import org.tasks.caldav.GeoUtils.toGeo
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.caldav.extensions.toAlarms
import org.tasks.caldav.extensions.toVAlarms
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.Alarm.Companion.TYPE_SNOOZE
import org.tasks.data.AlarmDao
import org.tasks.data.CaldavAccount
import org.tasks.data.CaldavCalendar
import org.tasks.data.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.Geofence
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.date.DateTimeUtils.toLocal
import org.tasks.jobs.WorkManager
import org.tasks.location.GeofenceApi
import org.tasks.notifications.NotificationManager
import org.tasks.preferences.Preferences
import org.tasks.repeats.RecurrenceUtils.newRRule
import org.tasks.time.DateTimeUtils.startOfDay
import org.tasks.time.DateTimeUtils.startOfMinute
import org.tasks.time.DateTimeUtils.toDate
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.text.ParseException
import java.util.TimeZone
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@Suppress("ClassName")
class iCalendar @Inject constructor(
        private val tagDataDao: TagDataDao,
        private val preferences: Preferences,
        private val locationDao: LocationDao,
        private val workManager: WorkManager,
        private val geofenceApi: GeofenceApi,
        private val taskCreator: TaskCreator,
        private val tagDao: TagDao,
        private val taskDao: TaskDao,
        private val caldavDao: CaldavDao,
        private val alarmDao: AlarmDao,
        private val alarmService: AlarmService,
        private val vtodoCache: VtodoCache,
        private val notificationManager: NotificationManager,
) {

    suspend fun setPlace(taskId: Long, geo: Geo?) {
        if (geo == null) {
            locationDao.getActiveGeofences(taskId).forEach {
                locationDao.delete(it.geofence)
                geofenceApi.update(it.place)
            }
            return
        }
        var place: Place? = locationDao.findPlace(
                geo.latitude.toLikeString(),
                geo.longitude.toLikeString()
        )
        if (place == null) {
            place = Place(
                latitude = geo.latitude.toDouble(),
                longitude = geo.longitude.toDouble(),
            ).let {
                it.copy(id = locationDao.insert(it))
            }
            workManager.reverseGeocode(place)
        }
        val existing = locationDao.getGeofences(taskId)
        if (existing == null) {
            locationDao.insert(
                Geofence(
                    place.uid,
                    preferences
                ).copy(task = taskId)
            )
        } else if (place != existing.place) {
            val geofence = existing.geofence.copy(place = place.uid)
            locationDao.update(geofence)
            geofenceApi.update(existing.place)
        }
        geofenceApi.update(place)
    }

    suspend fun getTags(categories: List<String>): List<TagData> {
        if (categories.isEmpty()) {
            return emptyList()
        }
        val tags = tagDataDao.getTags(categories).toMutableList()
        val existing = tags.map(TagData::name)
        val toCreate = categories subtract existing
        for (name in toCreate) {
            val tag = TagData(name)
            tagDataDao.createNew(tag)
            tags.add(tag)
        }
        return tags
    }

    suspend fun toVtodo(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        caldavTask: CaldavTask,
        task: com.todoroo.astrid.data.Task
    ): ByteArray {
        var remoteModel: Task? = null
        try {
            val vtodo = vtodoCache.getVtodo(calendar, caldavTask)
            if (vtodo?.isNotBlank() == true) {
                remoteModel = fromVtodo(vtodo)
            }
        } catch (e: java.lang.Exception) {
            Timber.e(e)
        }
        if (remoteModel == null) {
            remoteModel = Task()
        }

        return toVtodo(account, caldavTask, task, remoteModel)
    }

    suspend fun toVtodo(
        account: CaldavAccount,
        caldavTask: CaldavTask,
        task: com.todoroo.astrid.data.Task,
        remoteModel: Task
    ): ByteArray {
        remoteModel.applyLocal(caldavTask, task)
        val categories = remoteModel.categories
        categories.clear()
        categories.addAll(tagDataDao.getTagDataForTask(task.id).map { it.name!! })
        if (BuildConfig.DEBUG && caldavTask.remoteId.isNullOrBlank()) {
            throw IllegalStateException()
        }
        remoteModel.uid = caldavTask.remoteId
        val location = locationDao.getGeofences(task.id)
        val localGeo = toGeo(location)
        if (localGeo == null || !localGeo.equalish(remoteModel.geoPosition)) {
            remoteModel.geoPosition = localGeo
        }
        if (account.reminderSync) {
            remoteModel.alarms.removeAll(remoteModel.alarms.filtered)
            val alarms = alarmDao.getAlarms(task.id)
            remoteModel.snooze = alarms.find { it.type == TYPE_SNOOZE }?.time
            remoteModel.alarms.addAll(alarms.toVAlarms())
        }
        val os = ByteArrayOutputStream()
        remoteModel.write(os)
        return os.toByteArray()
    }

    suspend fun fromVtodo(
        account: CaldavAccount,
        calendar: CaldavCalendar,
        existing: CaldavTask?,
        remote: Task,
        vtodo: String?,
        obj: String? = null,
        eTag: String? = null
    ) {
        if (existing?.isDeleted() == true) {
            return
        }
        val task = existing?.task
            ?.let { taskDao.fetch(it) }
            ?: taskCreator.createWithValues("").apply {
                readOnly = calendar.access == ACCESS_READ_ONLY
                taskDao.createNew(this)
            }
        val caldavTask =
            existing
                ?.copy(task = task.id)
                ?: CaldavTask(
                    task = task.id,
                    calendar = calendar.uuid,
                    remoteId = remote.uid,
                    `object` = obj
                )
        val isNew = caldavTask.id == com.todoroo.astrid.data.Task.NO_ID
        val dirty = task.modificationDate > caldavTask.lastSync || caldavTask.lastSync == 0L
        val local = vtodoCache.getVtodo(calendar, caldavTask)?.let { fromVtodo(it) }
        task.applyRemote(remote, local)
        caldavTask.applyRemote(remote, local)

        if ((remote.lastAck ?: 0) > task.reminderLast) {
            notificationManager.cancel(task.id)
        }

        val place = locationDao.getPlaceForTask(task.id)
        if (place?.toGeo() == local?.geoPosition) {
            setPlace(task.id, remote.geoPosition)
        }

        val tags = tagDataDao.getTagDataForTask(task.id)
        val localTags = getTags(local?.categories ?: emptyList())
        if (tags.toSet() == localTags.toSet()) {
            tagDao.applyTags(task, tagDataDao, getTags(remote.categories))
        }

        if (
            isNew &&
            remote.reminders.isEmpty() &&
            !calendar.ctag.isNullOrBlank() && // not initial sync
            vtodo?.prodId()?.supportsReminders() != true // other client doesn't support reminder sync
        ) {
            task.setDefaultReminders(preferences)
            alarmService.synchronizeAlarms(task.id, task.getDefaultAlarms().toMutableSet())
        } else if (account.reminderSync) {
            val alarms = alarmDao.getAlarms(task.id).onEach {
                it.id = 0
                it.task = 0
            }
            val randomReminders = alarms.filter { it.type == TYPE_RANDOM }
            val localReminders =
                local?.reminders?.plus(randomReminders) ?: randomReminders
            if (alarms.toSet() == localReminders.toSet()) {
                val remoteReminders = remote.reminders.plus(randomReminders)
                val changed =
                    alarmService.synchronizeAlarms(caldavTask.task, remoteReminders.toMutableSet())
                if (changed) {
                    task.modificationDate = DateUtilities.now()
                }
            }
        }

        task.suppressSync()
        task.suppressRefresh()
        taskDao.save(task)
        vtodoCache.putVtodo(calendar, caldavTask, vtodo)
        caldavTask.etag = eTag
        if (!dirty) {
            caldavTask.lastSync = task.modificationDate
        }
        if (isNew) {
            caldavDao.insert(caldavTask)
            Timber.d("NEW %s", caldavTask)
        } else {
            caldavDao.update(caldavTask)
            Timber.d("UPDATE %s", caldavTask)
        }
    }

    companion object {
        private const val APPLE_SORT_ORDER = "X-APPLE-SORT-ORDER"
        private const val OC_HIDESUBTASKS = "X-OC-HIDESUBTASKS"
        private const val MOZ_SNOOZE_TIME = "X-MOZ-SNOOZE-TIME"
        private const val MOZ_LASTACK = "X-MOZ-LASTACK"
        private const val HIDE_SUBTASKS = "1"
        private val PRODID_MATCHER = ".*?PRODID:(.*?)\n.*".toPattern(Pattern.DOTALL)
        // VALARM extensions: https://datatracker.ietf.org/doc/html/rfc9074
        private val IGNORE_ALARM = DateTime("19760401T005545Z")
        private val IS_PARENT = { r: RelatedTo ->
            r.parameters.getParameter<RelType>(Parameter.RELTYPE).let {
                it === RelType.PARENT || it == null || it.value.isNullOrBlank()
            }
        }

        internal val IS_APPLE_SORT_ORDER = { x: Property? -> x?.name.equals(APPLE_SORT_ORDER, true) }
        private val IS_OC_HIDESUBTASKS = { x: Property? -> x?.name.equals(OC_HIDESUBTASKS, true) }
        private val IS_MOZ_SNOOZE_TIME = { x: Property? -> x?.name.equals(MOZ_SNOOZE_TIME, true) }
        private val IS_MOZ_LASTACK = { x: Property? -> x?.name.equals(MOZ_LASTACK, true) }

        fun Due?.apply(task: com.todoroo.astrid.data.Task) {
            task.dueDate = toMillis()
        }

        fun Due?.toMillis() =
            when (this?.date) {
                null -> 0
                is DateTime -> com.todoroo.astrid.data.Task.createDueDate(
                    URGENCY_SPECIFIC_DAY_TIME,
                    getLocal(this)
                )
                else -> com.todoroo.astrid.data.Task.createDueDate(
                    URGENCY_SPECIFIC_DAY,
                    getLocal(this)
                )
            }

        fun DtStart?.apply(task: com.todoroo.astrid.data.Task) {
            task.hideUntil = toMillis(task)
        }

        fun DtStart?.toMillis(task: com.todoroo.astrid.data.Task) =
            when (this?.date) {
                null -> 0
                is DateTime -> task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY_TIME, getLocal(this))
                else -> task.createHideUntil(HIDE_UNTIL_SPECIFIC_DAY, getLocal(this))
            }

        // this isn't necessarily the task originator but its the best we can do
        fun String.supportsReminders(): Boolean =
            CLIENTS_WITH_REMINDER_SYNC.any { contains(it) }

        fun String.prodId(): String? =
            PRODID_MATCHER.matcher(this).takeIf { it.matches() }?.group(1)

        private val CLIENTS_WITH_REMINDER_SYNC = listOf(
            "tasks.org",
            "Mozilla.org",
            "Apple Inc.",
        )

        internal fun getLocal(property: DateProperty): Long =
                org.tasks.time.DateTime.from(property.date)?.toLocal()?.millis ?: 0

        fun fromVtodo(vtodo: String): Task? {
            try {
                val tasks = tasksFromReader(StringReader(vtodo))
                if (tasks.size == 1) {
                    return tasks[0]
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            return null
        }

        var Task.parent: String?
            get() = relatedTo.find(IS_PARENT)?.value
            set(value) {
                val parents = relatedTo.filter(IS_PARENT)
                when {
                    value.isNullOrBlank() -> relatedTo.removeAll(parents)
                    parents.isEmpty() -> relatedTo.add(RelatedTo(value))
                    else -> {
                        if (parents.size > 1) {
                            relatedTo.removeAll(parents.drop(1))
                        }
                        parents[0].let {
                            it.value = value
                            it.parameters.replace(RelType.PARENT)
                        }
                    }
                }
            }

        var Task.order: Long?
            get() = unknownProperties.find(IS_APPLE_SORT_ORDER).let { it?.value?.toLongOrNull() }
            set(order) {
                if (order == null) {
                    unknownProperties.removeIf(IS_APPLE_SORT_ORDER)
                } else {
                    unknownProperties
                            .find(IS_APPLE_SORT_ORDER)
                            ?.let { it.value = order.toString() }
                            ?: unknownProperties.add(XProperty(APPLE_SORT_ORDER, order.toString()))
                }
            }

        var Task.collapsed: Boolean
            get() = unknownProperties.find(IS_OC_HIDESUBTASKS).let { it?.value == HIDE_SUBTASKS }
            set(collapsed) {
                if (collapsed) {
                    unknownProperties
                            .find(IS_OC_HIDESUBTASKS)
                            ?.let { it.value = HIDE_SUBTASKS }
                            ?: unknownProperties.add(XProperty(OC_HIDESUBTASKS, HIDE_SUBTASKS))
                } else {
                    unknownProperties.removeIf(IS_OC_HIDESUBTASKS)
                }
            }

        var Task.lastAck: Long?
            get() = unknownProperties.find(IS_MOZ_LASTACK)?.value?.let {
                org.tasks.time.DateTime.from(DateTime(it)).toLocal().millis
            }
            set(value) {
                value
                    ?.toDateTime()
                    ?.toUTC()
                    ?.let { DateTime(true).apply { time = it.millis } }
                    ?.let { utc ->
                        unknownProperties.find(IS_MOZ_LASTACK)
                            ?.let { it.value = utc.toString() }
                            ?: unknownProperties.add(
                                XProperty(MOZ_LASTACK, utc.toString())
                            )
                    }
            }

        var Task.snooze: Long?
            get() = unknownProperties.find(IS_MOZ_SNOOZE_TIME)?.value?.let {
                org.tasks.time.DateTime.from(DateTime(it)).toLocal().millis
            }
            set(value) {
                value
                        ?.toDateTime()
                        ?.takeIf { it.isAfterNow }
                        ?.toUTC()
                        ?.let { DateTime(true).apply { time = it.millis } }
                        ?.let { utc ->
                            unknownProperties.find(IS_MOZ_SNOOZE_TIME)
                                    ?.let { it.value = utc.toString() }
                                    ?: unknownProperties.add(
                                            XProperty(MOZ_SNOOZE_TIME, utc.toString())
                                    )
                            lastAck = lastModified?.toLocal()
                        }
                        ?: unknownProperties.removeIf(IS_MOZ_SNOOZE_TIME)
            }

        fun Task.applyLocal(caldavTask: CaldavTask, task: com.todoroo.astrid.data.Task) {
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
                Due(if (allDay) dueDate.toDate() else getDateTime(dueDate))
            } else {
                null
            }
            dtStart = if (startDate > 0) {
                DtStart(if (allDay) startDate.toDate() else getDateTime(startDate))
            } else {
                null
            }
            if (task.isCompleted) {
                completedAt = Completed(DateTime(task.completionDate))
                status = Status.VTODO_COMPLETED
                percentComplete = 100
            } else if (completedAt != null) {
                completedAt = null
                status = null
                percentComplete = null
            }
            rRule = if (task.isRecurring) {
                try {
                    newRRule(task.recurrence!!)
                } catch (e: ParseException) {
                    Timber.e(e)
                    null
                }
            } else {
                null
            }
            lastModified = newDateTime(task.modificationDate).toUTC().millis
            priority = when (task.priority) {
                com.todoroo.astrid.data.Task.Priority.NONE -> 0
                com.todoroo.astrid.data.Task.Priority.MEDIUM -> 5
                com.todoroo.astrid.data.Task.Priority.HIGH ->
                    if (priority < 5) max(1, priority) else 1
                else -> if (priority > 5) min(9, priority) else 9
            }
            parent = if (task.parent == 0L) null else caldavTask.remoteParent
            order = task.order
            collapsed = task.isCollapsed
        }

        val List<VAlarm>.filtered: List<VAlarm>
            get() =
                filter { it.action == Action.DISPLAY || it.action == Action.AUDIO }
                    .filterNot { it.trigger.dateTime == IGNORE_ALARM }

        val Task.reminders: List<Alarm>
            get() = alarms.filtered.toAlarms().let { alarms ->
                snooze?.let { time -> alarms.plus(Alarm(0, time, TYPE_SNOOZE))} ?: alarms
            }

        internal fun getDateTime(timestamp: Long): DateTime {
            val tz = ical4jTimeZone(TimeZone.getDefault().id)
            val dateTime = DateTime(if (tz != null) timestamp else org.tasks.time.DateTime(timestamp).toUTC().millis)
            dateTime.timeZone = tz
            return dateTime
        }
    }
}