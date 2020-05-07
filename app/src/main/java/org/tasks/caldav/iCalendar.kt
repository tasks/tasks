package org.tasks.caldav

import at.bitfire.ical4android.Task
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.google.common.collect.Iterables
import com.todoroo.andlib.utility.DateUtilities
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskCreator
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.RelatedTo
import org.tasks.Strings.isNullOrEmpty
import org.tasks.caldav.GeoUtils.equalish
import org.tasks.caldav.GeoUtils.toGeo
import org.tasks.caldav.GeoUtils.toLikeString
import org.tasks.data.*
import org.tasks.jobs.WorkManager
import org.tasks.location.GeofenceApi
import org.tasks.preferences.Preferences
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.StringReader
import javax.inject.Inject

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
        private val caldavDao: CaldavDao) {

    companion object {
        private val IS_PARENT: (RelatedTo?) -> Boolean = { r: RelatedTo? ->
            r!!.parameters.isEmpty || r.getParameter(Parameter.RELTYPE) === RelType.PARENT
        }

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

        fun getParent(remote: Task): String? {
            return remote.relatedTo.find(IS_PARENT)?.value
        }

        fun setParent(remote: Task, value: String?) {
            val relatedTo = remote.relatedTo
            if (isNullOrEmpty(value)) {
                Iterables.removeIf(relatedTo, IS_PARENT)
            } else {
                val parent = relatedTo.find(IS_PARENT)
                if (parent != null) {
                    parent.value = value
                } else {
                    relatedTo.add(RelatedTo(value))
                }
            }
        }
    }

    fun setPlace(taskId: Long, geo: Geo) {
        var place: Place? = locationDao.findPlace(
                geo.latitude.toLikeString(),
                geo.longitude.toLikeString())
        if (place == null) {
            place = Place.newPlace(geo)
            place.id = locationDao.insert(place)
            workManager.reverseGeocode(place)
        }
        val existing: Location? = locationDao.getGeofences(taskId)
        if (existing == null) {
            val geofence = Geofence(place.uid, preferences)
            geofence.task = taskId
            geofence.id = locationDao.insert(geofence)
        } else if (place != existing.place) {
            val geofence = existing.geofence
            geofence.place = place.uid
            locationDao.update(geofence)
            geofenceApi.update(existing.place)
        }
        geofenceApi.update(place)
    }

    fun getTags(categories: List<String>): List<TagData> {
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

    fun toVtodo(caldavTask: CaldavTask, task: com.todoroo.astrid.data.Task): ByteArray {
        val remoteModel = CaldavConverter.toCaldav(caldavTask, task)
        val categories = remoteModel.categories
        categories.clear()
        categories.addAll(tagDataDao.getTagDataForTask(task.id).map { it.name!! })
        if (isNullOrEmpty(caldavTask.remoteId)) {
            val caldavUid = UUIDHelper.newUUID()
            caldavTask.remoteId = caldavUid
            remoteModel.uid = caldavUid
        } else {
            remoteModel.uid = caldavTask.remoteId
        }
        val location = locationDao.getGeofences(task.id)
        val localGeo = toGeo(location)
        if (localGeo == null || !localGeo.equalish(remoteModel.geoPosition)) {
            remoteModel.geoPosition = localGeo
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
            caldavTask = CaldavTask(task.id, calendar.uuid, remote.uid, obj)
        } else {
            task = taskDao.fetch(existing.task)!!
            caldavTask = existing
        }
        CaldavConverter.apply(task, remote)
        val geo = remote.geoPosition
        if (geo == null) {
            locationDao.getActiveGeofences(task.id).forEach {
                locationDao.delete(it.geofence)
                geofenceApi.update(it.place)
            }
        } else {
            setPlace(task.id, geo)
        }
        tagDao.applyTags(task, tagDataDao, getTags(remote.categories))
        task.suppressSync()
        task.suppressRefresh()
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