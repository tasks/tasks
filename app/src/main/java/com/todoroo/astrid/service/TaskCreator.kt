package com.todoroo.astrid.service

import com.todoroo.andlib.utility.AndroidUtilities
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.api.CaldavFilter
import com.todoroo.astrid.api.Filter
import com.todoroo.astrid.api.GtasksFilter
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.Task
import com.todoroo.astrid.data.Task.Companion.DUE_DATE
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL
import com.todoroo.astrid.data.Task.Companion.HIDE_UNTIL_NONE
import com.todoroo.astrid.data.Task.Companion.IMPORTANCE
import com.todoroo.astrid.data.Task.Companion.createDueDate
import com.todoroo.astrid.gcal.GCalHelper
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.utility.TitleParser.parse
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.data.Alarm
import org.tasks.data.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.Alarm.Companion.whenDue
import org.tasks.data.Alarm.Companion.whenOverdue
import org.tasks.data.Alarm.Companion.whenStarted
import org.tasks.data.AlarmDao
import org.tasks.data.CaldavDao
import org.tasks.data.CaldavTask
import org.tasks.data.Geofence
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskDao
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.data.Tag
import org.tasks.data.TagDao
import org.tasks.data.TagData
import org.tasks.data.TagDataDao
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils.startOfDay
import timber.log.Timber
import javax.inject.Inject

class TaskCreator @Inject constructor(
        private val gcalHelper: GCalHelper,
        private val preferences: Preferences,
        private val tagDataDao: TagDataDao,
        private val taskDao: TaskDao,
        private val tagDao: TagDao,
        private val googleTaskDao: GoogleTaskDao,
        private val defaultFilterProvider: DefaultFilterProvider,
        private val caldavDao: CaldavDao,
        private val locationDao: LocationDao,
        private val alarmDao: AlarmDao,
) {

    suspend fun basicQuickAddTask(title: String): Task {
        val task = createWithValues(title.trim { it <= ' ' })
        taskDao.createNew(task)
        val gcalCreateEventEnabled = preferences.isDefaultCalendarSet && task.hasDueDate() // $NON-NLS-1$
        if (!isNullOrEmpty(task.title)
                && gcalCreateEventEnabled
                && isNullOrEmpty(task.calendarURI)) {
            val calendarUri = gcalHelper.createTaskEvent(task, preferences.defaultCalendar)
            task.calendarURI = calendarUri.toString()
        }
        createTags(task)
        val addToTop = preferences.addTasksToTop()
        if (task.hasTransitory(GoogleTask.KEY)) {
            googleTaskDao.insertAndShift(
                task,
                CaldavTask(
                    task = task.id,
                    calendar = task.getTransitory<String>(GoogleTask.KEY)!!,
                    remoteId = null
                ),
                addToTop
            )
        } else if (task.hasTransitory(CaldavTask.KEY)) {
            caldavDao.insert(
                task,
                CaldavTask(
                    task = task.id,
                    calendar = task.getTransitory(CaldavTask.KEY),
                ),
                addToTop
            )
        } else {
            val remoteList = defaultFilterProvider.getDefaultList()
            if (remoteList is GtasksFilter) {
                googleTaskDao.insertAndShift(
                    task,
                    CaldavTask(
                        task = task.id,
                        calendar = remoteList.remoteId,
                        remoteId = null
                    ),
                    addToTop
                )
            } else if (remoteList is CaldavFilter) {
                caldavDao.insert(
                    task,
                    CaldavTask(
                        task = task.id,
                        calendar = remoteList.uuid,
                    ),
                    addToTop
                )
            }
        }
        if (task.hasTransitory(Place.KEY)) {
            val place = locationDao.getPlace(task.getTransitory<String>(Place.KEY)!!)
            if (place != null) {
                locationDao.insert(Geofence(place.uid, preferences))
            }
        }
        taskDao.save(task, null)
        alarmDao.insert(task.getDefaultAlarms())
        return task
    }

    suspend fun createWithValues(title: String?): Task {
        return create(null, title)
    }

    suspend fun createWithValues(filter: Filter?, title: String?): Task {
        return create(
            AndroidUtilities.mapFromSerializedString(filter?.valuesForNewTasks),
            title
        )
    }

    /**
     * Create task from the given content values, saving it. This version doesn't need to start with a
     * base task model.
     */
    internal suspend fun create(values: Map<String, Any>?, title: String?): Task {
        val task = Task(
            title = title?.trim { it <= ' ' },
            creationDate = DateUtilities.now(),
            modificationDate = DateUtilities.now(),
            remoteId = UUIDHelper.newUUID(),
            priority = preferences.defaultPriority,
        )
        preferences.getStringValue(R.string.p_default_recurrence)
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    task.recurrence = it
                    task.repeatFrom = if (preferences.getIntegerFromString(R.string.p_default_recurrence_from, 0) == 1) {
                        Task.RepeatFrom.COMPLETION_DATE
                    } else {
                        Task.RepeatFrom.DUE_DATE
                    }
                }
        preferences.getStringValue(R.string.p_default_location)
                ?.takeIf { it.isNotBlank() }
                ?.let { task.putTransitory(Place.KEY, it) }
        task.setDefaultReminders(preferences)
        val tags = ArrayList<String>()
        values?.entries?.forEach { (key, value) ->
            when (key) {
                Tag.KEY -> tags.add(value as String)
                GoogleTask.KEY, CaldavTask.KEY, Place.KEY -> task.putTransitory(key, value)
                DUE_DATE.name -> value.substitute()?.toLongOrNull()?.let { task.dueDate =
                    createDueDate(Task.URGENCY_SPECIFIC_DAY, it) }
                IMPORTANCE.name -> value.substitute()?.toIntOrNull()?.let { task.priority = it }
                HIDE_UNTIL.name ->
                    value.substitute()?.toLongOrNull()?.let { task.hideUntil = it.startOfDay() }
            }
        }
        if (values?.containsKey(DUE_DATE.name) != true) {
            task.dueDate = createDueDate(
                    preferences.getIntegerFromString(R.string.p_default_urgency_key, Task.URGENCY_NONE),
                    0)
        }
        if (values?.containsKey(HIDE_UNTIL.name) != true) {
            task.hideUntil = task.createHideUntil(
                    preferences.getIntegerFromString(R.string.p_default_hideUntil_key, HIDE_UNTIL_NONE),
                    0
            )
        }
        if (tags.isEmpty()) {
            preferences.getStringValue(R.string.p_default_tags)
                    ?.split(",")
                    ?.map { tagDataDao.getByUuid(it) }
                    ?.mapNotNull { it?.name }
                    ?.let { tags.addAll(it) }
        }
        try {
            parse(tagDataDao, task, tags)
        } catch (e: Throwable) {
            Timber.e(e)
        }
        task.setTags(tags)
        return task
    }

    suspend fun createTags(task: Task) {
        for (tag in task.tags) {
            var tagData = tagDataDao.getTagByName(tag)
            if (tagData == null) {
                tagData = TagData()
                tagData.name = tag
                tagDataDao.createNew(tagData)
            }
            tagDao.insert(Tag(task, tagData))
        }
    }

    companion object {
        fun Task.setDefaultReminders(preferences: Preferences) {
            randomReminder = DateUtilities.ONE_HOUR * preferences.getIntegerFromString(
                R.string.p_rmd_default_random_hours,
                0
            )
            defaultReminders(preferences.defaultReminders)
            ringFlags = preferences.defaultRingMode
        }

        private fun Any?.substitute(): String? =
            (this as? String)?.let { PermaSql.replacePlaceholdersForNewTask(it) }

        fun Task.getDefaultAlarms(): List<Alarm> = ArrayList<Alarm>().apply {
            if (hasStartDate() && isNotifyAtStart) {
                add(whenStarted(id))
            }
            if (hasDueDate()) {
                if (isNotifyAtDeadline) {
                    add(whenDue(id))
                }
                if (isNotifyAfterDeadline) {
                    add(whenOverdue(id))
                }
            }
            if (randomReminder > 0) {
                add(Alarm(id, randomReminder, TYPE_RANDOM))
            }
        }
    }
}
