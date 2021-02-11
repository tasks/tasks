package org.tasks.location

interface LocationProvider {
    suspend fun currentLocation(): MapPosition?
}