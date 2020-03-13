package org.tasks.caldav

import at.bitfire.ical4android.Task
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.google.common.collect.Sets.difference
import com.google.common.collect.Sets.newHashSet
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.data.SyncFlags
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskCreator
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.property.RelatedTo
import org.tasks.data.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.StringReader
import javax.inject.Inject

class iCalendar @Inject constructor(
        private val tagDataDao: TagDataDao,
        private val taskCreator: TaskCreator,
        private val tagDao: TagDao,
        private val taskDao: TaskDao,
        private val caldavDao: CaldavDao) {

    companion object {
        private val IS_PARENT = Predicate { r: RelatedTo? ->
            r!!.parameters.isEmpty || r.getParameter(Parameter.RELTYPE) === RelType.PARENT
        }

        fun fromVtodo(vtodo: String): Task? {
            val tasks = tasksFromReader(StringReader(vtodo))
            return if (tasks.size == 1) tasks[0] else null
        }

        fun getParent(remote: Task): String? {
            val relatedTo = remote.relatedTo
            val parent = Iterables.tryFind(relatedTo, IS_PARENT)
            return if (parent.isPresent) parent.get().value else null
        }

        fun setParent(remote: Task, value: String?) {
            val relatedTo = remote.relatedTo
            if (Strings.isNullOrEmpty(value)) {
                Iterables.removeIf(relatedTo, IS_PARENT)
            } else {
                val parent = Iterables.tryFind(relatedTo, IS_PARENT)
                if (parent.isPresent) {
                    parent.get().value = value
                } else {
                    relatedTo.add(RelatedTo(value))
                }
            }
        }
    }

    fun getTags(categories: List<String>): List<TagData> {
        if (categories.isEmpty()) {
            return emptyList()
        }
        val tags = tagDataDao.getTags(categories)
        val existing = Lists.transform(tags) { obj: TagData? -> obj!!.name }
        val toCreate = difference(newHashSet(categories), newHashSet(existing))
        for (name in toCreate) {
            val tag = TagData(name)
            tagDataDao.createNew(tag)
            tags.add(tag)
        }
        return tags
    }

    fun toVtodo(caldavTask: CaldavTask, task: com.todoroo.astrid.data.Task): ByteArray {
        val remoteModel = CaldavConverter.toCaldav(caldavTask, task)
        val categories = remoteModel.categories
        categories.clear()
        categories.addAll(Lists.transform(tagDataDao.getTagDataForTask(task.getId())) { obj: TagData? -> obj!!.name })
        if (Strings.isNullOrEmpty(caldavTask.remoteId)) {
            val caldavUid = UUIDHelper.newUUID()
            caldavTask.remoteId = caldavUid
            remoteModel.uid = caldavUid
        } else {
            remoteModel.uid = caldavTask.remoteId
        }

        val os = ByteArrayOutputStream()
        remoteModel.write(os)
        return os.toByteArray()
    }

    fun fromVtodo(
            calendar: CaldavCalendar,
            existing: CaldavTask?,
            remote: Task,
            vtodo: String,
            obj: String? = null,
            eTag: String? = null) {
        val task: com.todoroo.astrid.data.Task
        val caldavTask: CaldavTask
        if (existing == null) {
            task = taskCreator.createWithValues("")
            taskDao.createNew(task)
            caldavTask = CaldavTask(task.getId(), calendar.uuid, remote.uid, obj)
        } else {
            task = taskDao.fetch(existing.task)
            caldavTask = existing
        }
        CaldavConverter.apply(task, remote)
        tagDao.applyTags(task, tagDataDao, getTags(remote.categories))
        task.putTransitory(SyncFlags.GTASKS_SUPPRESS_SYNC, true)
        task.putTransitory(TaskDao.TRANS_SUPPRESS_REFRESH, true)
        taskDao.save(task)
        caldavTask.vtodo = vtodo
        caldavTask.etag = eTag
        caldavTask.lastSync = DateUtilities.now() + 1000L
        caldavTask.remoteParent = getParent(remote)
        if (caldavTask.id == com.todoroo.astrid.data.Task.NO_ID) {
            caldavTask.id = caldavDao.insert(caldavTask)
            Timber.d("NEW %s", caldavTask)
        } else {
            caldavDao.update(caldavTask)
            Timber.d("UPDATE %s", caldavTask)
        }
    }
}