package org.tasks.data

import org.tasks.data.entity.Task
import org.tasks.R
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.preferences.Preferences

suspend fun LocationDao.getLocation(task: Task, preferences: Preferences): Location? {
    if (task.isNew) {
        if (task.hasTransitory(Place.KEY)) {
            getPlace(task.getTransitory<String>(Place.KEY)!!)?.let {
                return Location(createGeofence(it.uid, preferences), it)
            }
        }
    } else {
        return getGeofences(task.id)
    }
    return null
}

fun createGeofence(
    place: String?,
    preferences: Preferences,
    defaultReminders: Int = preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1)
) = Geofence(
    place = place,
    isArrival = defaultReminders == 1 || defaultReminders == 3,
    isDeparture = defaultReminders == 2 || defaultReminders == 3,
)
