package org.tasks.caldav

import at.bitfire.ical4android.Task
import at.bitfire.ical4android.Task.Companion.tasksFromReader
import com.todoroo.astrid.dao.TaskDao
import com.todoroo.astrid.helper.UUIDHelper
import com.todoroo.astrid.service.TaskCreator
import net.fortuna.ical4j.model.Parameter
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.parameter.RelType
import net.fortuna.ical4j.model.property.Geo
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.XProperty
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
        const val APPLE_SORT_ORDER = "X-APPLE-SORT-ORDER"
        private val IS_PARENT = { r: RelatedTo ->
            r.parameters.getParameter<RelType>(Parameter.RELTYPE).let {
                it === RelType.PARENT || it == null || it.value.isNullOrBlank()
            }
        }

        private val IS_APPLE_SORT_ORDER = { x: Property? ->
            x?.name.equals(APPLE_SORT_ORDER, true)
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

        fun Task.getParents(): List<RelatedTo> = relatedTo.filter(IS_PARENT)

        fun Task.getParent(): String? {
            return relatedTo.find(IS_PARENT)?.value
        }

        fun Task.setParent(value: String?) {
            val parents = getParents()
            if (value.isNullOrBlank()) {
                relatedTo.removeAll(parents)
            } else {
                if (parents.isEmpty()) {
                    relatedTo.add(RelatedTo(value))
                } else {
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
            get() = unknownProperties
                    .find { it.name?.equals(APPLE_SORT_ORDER, true) == true }
                    .let { it?.value?.toLongOrNull() }
            set(order) {
                if (order == null) {
                    unknownProperties.removeAll(unknownProperties.filter(IS_APPLE_SORT_ORDER))
                } else {
                    val existingOrder = unknownProperties
                            .find { it.name?.equals(APPLE_SORT_ORDER, true) == true }

                    if (existingOrder != null) {
                        existingOrder.value = order.toString()
                    } else {
                        unknownProperties.add(XProperty(APPLE_SORT_ORDER, order.toString()))
                    }
                }
            }
    }

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

    suspend fun toVtodo(caldavTask: CaldavTask, task: com.todoroo.astrid.data.Task): ByteArray {
        val remoteModel = CaldavConverter.toCaldav(caldavTask, task)
        remoteModel.order = caldavTask.order
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

    suspend fun fromVtodo(
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
            caldavTask = existing
            task = taskDao.fetch(existing.task)
                    ?: taskCreator.createWithValues("").apply {
                        taskDao.createNew(this)
                        caldavTask.task = id
                    }
        }
        CaldavConverter.apply(task, remote)
        caldavTask.order = remote.order
        setPlace(task.id, remote.geoPosition)
        tagDao.applyTags(task, tagDataDao, getTags(remote.categories))
        task.suppressSync()
        task.suppressRefresh()
        taskDao.save(task)
        caldavTask.vtodo = vtodo
        caldavTask.etag = eTag
        caldavTask.lastSync = task.modificationDate
        caldavTask.remoteParent = remote.getParent()
        if (caldavTask.id == com.todoroo.astrid.data.Task.NO_ID) {
            caldavTask.id = caldavDao.insert(caldavTask)
            Timber.d("NEW %s", caldavTask)
        } else {
            caldavDao.update(caldavTask)
            Timber.d("UPDATE %s", caldavTask)
        }
    }
}