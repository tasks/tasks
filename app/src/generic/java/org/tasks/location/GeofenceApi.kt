package org.tasks.location

import org.tasks.data.Place
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class GeofenceApi @Inject constructor() {
    fun registerAll() {}
    fun update(place: Place?) {}
    fun update(place: String?) {}
    fun update(taskId: Long) {}
}