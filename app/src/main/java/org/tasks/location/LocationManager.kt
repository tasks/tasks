package org.tasks.location

import android.location.Location

interface LocationManager {
    val lastKnownLocations: List<Location>
}