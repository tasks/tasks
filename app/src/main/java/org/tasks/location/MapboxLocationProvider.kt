package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class MapboxLocationProvider(private val context: Context) : LocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): MapPosition = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            LocationEngineProvider.getBestLocationEngine(context)
                    .getLastLocation(
                            object : LocationEngineCallback<LocationEngineResult> {
                                override fun onSuccess(result: LocationEngineResult) {
                                    val location = result.lastLocation!!
                                    cont.resumeWith(Result.success(MapPosition(location.latitude, location.longitude)))
                                }

                                override fun onFailure(exception: Exception) {
                                    cont.resumeWith(Result.failure(exception))
                                }
                            })
        }
    }
}