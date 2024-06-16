package com.todoroo.astrid.service

import android.content.Context
import android.net.Uri
import androidx.annotation.ColorRes
import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import com.todoroo.astrid.dao.TaskDao
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.VtodoCache
import org.tasks.caldav.iCalendar
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.data.CaldavTaskContainer
import org.tasks.data.Location
import org.tasks.data.convertPictureUri
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.GoogleTaskListDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.UpgraderDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.filters.GtasksFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.widget.AppWidgetManager
import org.tasks.widget.WidgetPreferences
import java.io.File
import javax.inject.Inject

class Upgrader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val tagDataDao: TagDataDao,
    private val tagDao: TagDao,
    private val filterDao: FilterDao,
    private val defaultFilterProvider: DefaultFilterProvider,
    private val googleTaskListDao: GoogleTaskListDao,
    private val userActivityDao: UserActivityDao,
    private val taskAttachmentDao: TaskAttachmentDao,
    private val caldavDao: CaldavDao,
    private val taskDao: TaskDao,
    private val locationDao: LocationDao,
    private val iCal: iCalendar,
    private val widgetManager: AppWidgetManager,
    private val taskMover: TaskMover,
    private val upgraderDao: UpgraderDao,
    private val vtodoCache: VtodoCache,
    private val upgrade_11_3: Lazy<Upgrade_11_3>,
    private val upgrade_11_12_3: Lazy<Upgrade_11_12_3>,
    private val upgrade_12_4: Lazy<Upgrade_12_4>,
    private val upgrade_13_2: Lazy<Upgrade_13_2>,
) {

    fun upgrade(from: Int, to: Int) {
        if (from > 0) {
            run(from, V4_9_5) { removeDuplicateTags() }
            run(from, V5_3_0) { migrateFilters() }
            run(from, V6_0_beta_1) { migrateDefaultSyncList() }
            run(from, V6_4) { migrateUris() }
            run(from, V6_7) { this.migrateGoogleTaskFilters() }
            run(from, V6_8_1) { this.migrateCaldavFilters() }
            run(from, V6_9) { applyCaldavCategories() }
            run(from, V7_0) { applyCaldavSubtasks() }
            run(from, V8_2) { migrateColors() }
            run(from, V8_5) { applyCaldavGeo() }
            run(from, V8_8) { preferences.setBoolean(R.string.p_linkify_task_edit, true) }
            run(from, V8_10) { migrateWidgets() }
            run(from, V9_3) { applyCaldavOrder() }
            run(from, V9_6) {
                preferences.setBoolean(R.string.p_astrid_sort_enabled, true)
                taskMover.migrateLocalTasks()
            }
            run(from, V9_7) { caldavDao.resetOrders() }
            run(from, V9_7_3) { caldavDao.updateParents() }
            run(from, V10_0_2) {
                filterDao.getFilters()
                        .filter { it.dirtyHack.trim() == "WHERE" }
                        .forEach { filterDao.delete(it) }
            }
            run(from, Upgrade_11_3.VERSION) {
                with(upgrade_11_3.get()) {
                    applyiCalendarStartDates()
                    applyOpenTaskStartDates()
                }
            }
            run(from, Upgrade_11_12_3.VERSION) {
                upgrade_11_12_3.get().migrateDefaultReminderPreference()
            }
            run(from, V11_13) {
                preferences.setString(R.string.p_completion_ringtone, "")
            }
            run(from, Upgrade_12_4.VERSION) {
                upgrade_12_4.get().syncExistingAlarms()
            }
            run(from, V12_6) {
                setInstallDetails(from)
            }
            run(from, Upgrade_13_2.VERSION) {
                caldavDao.updateParents()
                upgrade_13_2.get().rebuildFilters()
            }
            preferences.setBoolean(R.string.p_just_updated, true)
        } else {
            setInstallDetails(to)
        }
        preferences.lastSubscribeRequest = 0L
        preferences.setCurrentVersion(to)
    }

    private fun setInstallDetails(version: Int) {
        preferences.installVersion = version
        preferences.installDate = currentTimeMillis()
    }

    private fun run(from: Int, version: Int, runnable: suspend () -> Unit) {
        if (from < version) {
            runBlocking {
                runnable()
            }
            preferences.setCurrentVersion(version)
        }
    }

    private fun migrateWidgets() {
        for (widgetId in widgetManager.widgetIds) {
            val widgetPreferences = WidgetPreferences(context, preferences, widgetId)
            widgetPreferences.maintainExistingConfiguration()
        }
    }

    private suspend fun migrateColors() {
        preferences.setInt(
                R.string.p_theme_color, getAndroidColor(preferences.getInt(R.string.p_theme_color, 7)))
        for (calendar in caldavDao.getCalendars()) {
            caldavDao.update(
                calendar.copy(color = getAndroidColor(calendar.color))
            )
        }
        for (tagData in tagDataDao.getAll()) {
            tagDataDao.update(
                tagData.copy(color = getAndroidColor(tagData.color ?: 0))
            )
        }
        for (filter in filterDao.getFilters()) {
            filterDao.update(
                filter.copy(
                    color = getAndroidColor(filter.color ?: 0)
                )
            )
        }
    }

    private fun getAndroidColor(index: Int): Int {
        return getAndroidColor(context, index)
    }

    private suspend fun applyCaldavOrder() {
        for (task in upgraderDao.tasksWithVtodos().map(CaldavTaskContainer::caldavTask)) {
            val remoteTask = vtodoCache.getVtodo(task)?.let { fromVtodo(it) } ?: continue
            val order: Long? = remoteTask.order
            if (order != null) {
                taskDao.setOrder(task.task, order)
            }
        }
    }

    private suspend fun applyCaldavGeo() {
        val tasksWithLocations = locationDao.getActiveGeofences().map(Location::task)
        for (task in upgraderDao.tasksWithVtodos().map(CaldavTaskContainer::caldavTask)) {
            val taskId = task.task
            if (tasksWithLocations.contains(taskId)) {
                continue
            }
            val remoteTask = vtodoCache.getVtodo(task)?.let { fromVtodo(it) } ?: continue
            val geo = remoteTask.geoPosition ?: continue
            iCal.setPlace(taskId, geo)
        }
        taskDao.touch(tasksWithLocations)
    }

    private suspend fun applyCaldavSubtasks() {
        val updated: MutableList<CaldavTask> = ArrayList()
        for (task in upgraderDao.tasksWithVtodos().map(CaldavTaskContainer::caldavTask)) {
            val remoteTask = vtodoCache.getVtodo(task)?.let { fromVtodo(it) } ?: continue
            task.remoteParent = remoteTask.parent
            if (!isNullOrEmpty(task.remoteParent)) {
                updated.add(task)
            }
        }
        caldavDao.update(updated)
        caldavDao.updateParents()
    }

    private suspend fun applyCaldavCategories() {
        val tasksWithTags: List<Long> = upgraderDao.tasksWithTags()
        for (container in upgraderDao.tasksWithVtodos()) {
            val remoteTask =
                vtodoCache.getVtodo(container.caldavTask)?.let { fromVtodo(it) } ?: continue
            tagDao.insert(container.task, iCal.getTags(remoteTask.categories))
        }
        taskDao.touch(tasksWithTags)
    }

    private suspend fun removeDuplicateTags() {
        val tagsByUuid: ListMultimap<String, TagData> = Multimaps.index(tagDataDao.tagDataOrderedByName()) { it!!.remoteId }
        for (uuid in tagsByUuid.keySet()) {
            removeDuplicateTagData(tagsByUuid[uuid])
            removeDuplicateTagMetadata(uuid)
        }
    }

    private suspend fun migrateGoogleTaskFilters() {
        for (filter in filterDao.getFilters()) {
            filterDao.update(
                filter.copy(
                    sql = migrateGoogleTaskFilters(filter.dirtyHack),
                    criterion = migrateGoogleTaskFilters(filter.criterion),
                )
            )
        }
    }

    private suspend fun migrateCaldavFilters() {
        for (filter in filterDao.getFilters()) {
            filterDao.update(
                filter.copy(
                    sql = migrateCaldavFilters(filter.dirtyHack),
                    criterion = migrateCaldavFilters(filter.criterion),
                )
            )
        }
    }

    private suspend fun migrateFilters() {
        for (filter in filterDao.getFilters()) {
            filterDao.update(
                filter.copy(
                    sql = migrateMetadata(filter.dirtyHack),
                    criterion = migrateMetadata(filter.criterion),
                )
            )
        }
    }

    private suspend fun migrateDefaultSyncList() {
        val account = preferences.getStringValue("gtasks_user")
        if (isNullOrEmpty(account)) {
            return
        }
        val defaultGoogleTaskList = preferences.getStringValue("gtasks_defaultlist")
        if (isNullOrEmpty(defaultGoogleTaskList)) {
            // TODO: look up default list
        } else {
            val googleTaskList = googleTaskListDao.getByRemoteId(defaultGoogleTaskList!!)
            if (googleTaskList != null) {
                defaultFilterProvider.defaultList = GtasksFilter(googleTaskList)
            }
        }
    }

    private suspend fun migrateUris() {
        migrateUriPreference(R.string.p_backup_dir)
        migrateUriPreference(R.string.p_attachment_dir)
        for (userActivity in userActivityDao.getComments()) {
            userActivity.convertPictureUri()
            userActivityDao.update(userActivity)
        }
        for (attachment in taskAttachmentDao.getAttachments()) {
            taskAttachmentDao.update(
                attachment.copy(
                    uri = Uri.fromFile(File(attachment.uri)).toString()
                )
            )
        }
    }

    private fun migrateUriPreference(pref: Int) {
        val path = preferences.getStringValue(pref)
        if (isNullOrEmpty(path)) {
            return
        }
        val file = File(path)
        try {
            if (file.canWrite()) {
                preferences.setUri(pref, file.toURI())
            } else {
                preferences.remove(pref)
            }
        } catch (ignored: SecurityException) {
            preferences.remove(pref)
        }
    }

    private fun migrateGoogleTaskFilters(input: String?): String {
        return input.orEmpty()
                .replace("SELECT task FROM google_tasks", "SELECT gt_task as task FROM google_tasks")
                .replace("(list_id", "(gt_list_id")
                .replace("google_tasks.list_id", "google_tasks.gt_list_id")
                .replace("google_tasks.task", "google_tasks.gt_task")
    }

    private fun migrateCaldavFilters(input: String?): String {
        return input.orEmpty()
                .replace("SELECT task FROM caldav_tasks", "SELECT cd_task as task FROM caldav_tasks")
                .replace("(calendar", "(cd_calendar")
    }

    private fun migrateMetadata(input: String?): String {
        return input.orEmpty()
                .replace(
                        """SELECT metadata\.task AS task FROM metadata INNER JOIN tasks ON \(\(metadata\.task=tasks\._id\)\) WHERE \(\(\(tasks\.completed=0\) AND \(tasks\.deleted=0\) AND \(tasks\.hideUntil<\(strftime\(\'%s\',\'now\'\)\*1000\)\)\) AND \(metadata\.key=\'tags-tag\'\) AND \(metadata\.value""".toRegex(),
                        "SELECT tags.task AS task FROM tags INNER JOIN tasks ON ((tags.task=tasks._id)) WHERE (((tasks.completed=0) AND (tasks.deleted=0) AND (tasks.hideUntil<(strftime('%s','now')*1000))) AND (tags.name")
                .replace(
                        """SELECT metadata\.task AS task FROM metadata INNER JOIN tasks ON \(\(metadata\.task=tasks\._id\)\) WHERE \(\(\(tasks\.completed=0\) AND \(tasks\.deleted=0\) AND \(tasks\.hideUntil<\(strftime\(\'%s\',\'now\'\)\*1000\)\)\) AND \(metadata\.key=\'gtasks\'\) AND \(metadata\.value2""".toRegex(),
                        "SELECT google_tasks.task AS task FROM google_tasks INNER JOIN tasks ON ((google_tasks.task=tasks._id)) WHERE (((tasks.completed=0) AND (tasks.deleted=0) AND (tasks.hideUntil<(strftime('%s','now')*1000))) AND (google_tasks.list_id")
                .replace("""AND \(metadata\.deleted=0\)""".toRegex(), "")
    }

    private suspend fun removeDuplicateTagData(tagData: List<TagData>) {
        if (tagData.size > 1) {
            tagDataDao.delete(tagData.subList(1, tagData.size))
        }
    }

    private suspend fun removeDuplicateTagMetadata(uuid: String) {
        val metadatas = tagDao.getByTagUid(uuid)
        val metadataByTask: ImmutableListMultimap<Long, Tag> = Multimaps.index(metadatas) { it!!.task }
        for (key in metadataByTask.keySet()) {
            val tags = metadataByTask[key]
            if (tags.size > 1) {
                tagDao.delete(tags.subList(1, tags.size))
            }
        }
    }

    companion object {
        private const val V4_9_5 = 434
        private const val V5_3_0 = 491
        private const val V6_0_beta_1 = 522
        const val V6_4 = 546
        private const val V6_7 = 585
        private const val V6_8_1 = 607
        private const val V6_9 = 608
        private const val V7_0 = 617
        const val V8_2 = 675
        private const val V8_5 = 700
        private const val V8_8 = 717
        private const val V8_10 = 735
        private const val V9_3 = 90300
        const val V9_6 = 90600
        const val V9_7 = 90700
        const val V9_7_3 = 90704
        const val V10_0_2 = 100012
        const val V11_13 = 111300
        const val V12_4 = 120400
        const val V12_6 = 120601
        const val V12_8 = 120800

        @JvmStatic
        fun getAndroidColor(context: Context, index: Int): Int {
            val legacyColor = getLegacyColor(index, 0)
            return if (legacyColor == 0) 0 else context.getColor(legacyColor)
        }

        @JvmStatic
        @ColorRes
        fun getLegacyColor(index: Int, def: Int): Int {
            return when (index) {
                0 -> org.tasks.kmp.R.color.blue_grey_500
                1 -> org.tasks.kmp.R.color.grey_900
                2 -> org.tasks.kmp.R.color.red_500
                3 -> org.tasks.kmp.R.color.pink_500
                4 -> org.tasks.kmp.R.color.purple_500
                5 -> org.tasks.kmp.R.color.deep_purple_500
                6 -> org.tasks.kmp.R.color.indigo_500
                7 -> org.tasks.kmp.R.color.blue_500
                8 -> org.tasks.kmp.R.color.light_blue_500
                9 -> org.tasks.kmp.R.color.cyan_500
                10 -> org.tasks.kmp.R.color.teal_500
                11 -> org.tasks.kmp.R.color.green_500
                12 -> org.tasks.kmp.R.color.light_green_500
                13 -> org.tasks.kmp.R.color.lime_500
                14 -> org.tasks.kmp.R.color.yellow_500
                15 -> org.tasks.kmp.R.color.amber_500
                16 -> org.tasks.kmp.R.color.orange_500
                17 -> org.tasks.kmp.R.color.deep_orange_500
                18 -> org.tasks.kmp.R.color.brown_500
                19 -> org.tasks.kmp.R.color.grey_500
                20 -> R.color.white_100
                else -> def
            }
        }

        private val Filter.dirtyHack: String
            get() = sql!!.replace("tasks.userId=0", "1")
    }
}