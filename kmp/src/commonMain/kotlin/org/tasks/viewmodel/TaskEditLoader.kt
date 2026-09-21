package org.tasks.viewmodel

import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.tasks.compose.pickers.initialStartSelection
import org.tasks.compose.pickers.resolveStartDate
import org.tasks.compose.pickers.startDayOf
import org.tasks.data.TaskCreator
import org.tasks.data.createDueDate
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.getDefaultAlarms
import org.tasks.data.getOrCreateDefaultListFilter
import org.tasks.data.setDefaultReminders
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.AppPreferences
import org.tasks.preferences.DatePickerPreferences
import org.tasks.preferences.TaskDefaultSettings

internal class TaskEditLoader(
    private val taskId: Long?,
    private val uuid: String?,
    private val listId: Long?,
    private val tagUuid: String?,
    private val isSubtaskDraft: Boolean,
    private val taskDao: TaskDao,
    private val caldavDao: CaldavDao,
    private val tagDataDao: TagDataDao,
    private val alarmDao: AlarmDao,
    private val appPreferences: AppPreferences,
    private val taskCreator: TaskCreator,
    private val log: Logger,
) {
    suspend fun read(
        prefs: DatePickerPreferences,
        defaults: TaskDefaultSettings,
    ): TaskEditViewModel.State {
        val loaded: Task
        val list: CaldavFilter?
        val tags: List<TagData>
        val alarms: ImmutableSet<Alarm>
        if (taskId == null) {
            val existing = uuid?.let { taskDao.fetch(it) }
            if (existing != null) {
                loaded = existing
                coroutineScope {
                    val listDeferred = async { caldavListFor(existing.id, defaults.defaultList) }
                    val tagsDeferred = async { tagDataDao.getTagDataForTask(existing.id) }
                    val alarmsDeferred = async { alarmDao.getAlarms(existing.id).toPersistentSet() }
                    list = listDeferred.await()
                    tags = tagsDeferred.await()
                    alarms = alarmsDeferred.await()
                }
            } else {
                if (isSubtaskDraft) {
                    return TaskEditViewModel.State(isLoading = false, deleted = true)
                }
                loaded = (uuid
                    ?.let { taskCreator.createBlankTask(remoteId = it) }
                    ?: taskCreator.createBlankTask())
                    .apply {
                        applyDefaults(defaults)
                        setDefaultReminders(defaults)
                    }
                coroutineScope {
                    val listDeferred = async { seedList(defaults.defaultList) }
                    val tagsDeferred = async { seedTags(defaults.defaultTags) }
                    list = listDeferred.await()
                    tags = tagsDeferred.await()
                }
                alarms = persistentSetOf()
            }
        } else {
            val existing: Task?
            coroutineScope {
                val loadedDeferred = async { taskDao.fetch(taskId) }
                val listDeferred = async { caldavListFor(taskId, defaults.defaultList) }
                val tagsDeferred = async { tagDataDao.getTagDataForTask(taskId) }
                val alarmsDeferred = async { alarmDao.getAlarms(taskId).toPersistentSet() }
                existing = loadedDeferred.await()
                list = listDeferred.await()
                tags = tagsDeferred.await()
                alarms = alarmsDeferred.await()
            }
            if (existing == null) {
                return TaskEditViewModel.State(isLoading = false, deleted = true)
            }
            loaded = existing
        }
        val (startDay, startTime) = initialStartSelection(
            hideUntil = loaded.hideUntil,
            dueDate = loaded.dueDate,
            isNew = loaded.isNew,
            defaultHideUntil = defaults.defaultHideUntil,
        )
        val task = if (loaded.hideUntil <= 0) {
            loaded.copy(hideUntil = resolveStartDate(startDayOf(startDay), startTime, loaded.dueDate))
        } else {
            loaded
        }
        val initialAlarms = if (loaded.isNew) {
            task.getDefaultAlarms(appPreferences.isDefaultDueTimeEnabled()).toPersistentSet()
        } else {
            alarms
        }
        return TaskEditViewModel.State(
            isLoading = false,
            task = task,
            originalTask = task.copy(),
            deleted = task.isDeleted,
            list = list,
            originalList = list,
            tags = tags,
            originalTags = tags,
            alarms = initialAlarms,
            originalAlarms = initialAlarms,
            startDay = startDay,
            startTime = startTime,
            originalStartDay = startDay,
            originalStartTime = startTime,
            datePickerPreferences = prefs,
            addTasksToTop = defaults.addTasksToTop,
        )
    }

    private fun Task.applyDefaults(defaults: TaskDefaultSettings) {
        priority = defaults.defaultPriority
        dueDate = defaultDueDate(defaults.defaultDueDate)
        defaults.defaultRecurrence?.let {
            recurrence = it
            repeatFrom = if (defaults.defaultRecurrenceFrom == Task.RepeatFrom.COMPLETION_DATE) {
                Task.RepeatFrom.COMPLETION_DATE
            } else {
                Task.RepeatFrom.DUE_DATE
            }
            if (dueDate == 0L) {
                dueDate = createDueDate(Task.URGENCY_TODAY, 0)
            }
        }
    }

    private fun defaultDueDate(setting: Int): Long = try {
        createDueDate(setting, 0)
    } catch (e: IllegalArgumentException) {
        log.e(e) { "Unknown default due date $setting" }
        0
    }

    private suspend fun seedList(defaultList: String?): CaldavFilter? {
        val calendar = listId
            ?.let { caldavDao.getCalendarById(it) }
            ?.takeIf { !it.readOnly() }
            ?: return fallbackList(defaultList)
        val account = calendar.account?.let { caldavDao.getAccountByUuid(it) }
            ?: return fallbackList(defaultList)
        return CaldavFilter(calendar = calendar, account = account)
    }

    private suspend fun seedTags(defaultTags: List<String>): List<TagData> {
        tagUuid?.let { uuid -> tagDataDao.getByUuid(uuid)?.let { return listOf(it) } }
        return defaultTags
            .takeIf { it.isNotEmpty() }
            ?.let { tagDataDao.getByUuid(it) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private suspend fun fallbackList(defaultList: String?): CaldavFilter =
        caldavDao.getOrCreateDefaultListFilter(defaultList)

    private suspend fun caldavListFor(taskId: Long, defaultList: String?): CaldavFilter? {
        val caldavTask = caldavDao.getTask(taskId)
        val calendar = caldavTask?.calendar?.let { caldavDao.getCalendarByUuid(it) }
        val account = calendar?.account?.let { caldavDao.getAccountByUuid(it) }
        return if (calendar != null && account != null) {
            CaldavFilter(calendar = calendar, account = account)
        } else {
            fallbackList(defaultList)
        }
    }
}
