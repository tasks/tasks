package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.suspendCoroutine

class PlayLocationProvider(private val context: Context) : LocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): MapPosition = withContext(Dispatchers.IO) {
        suspendCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener {
                        cont.resumeWith(Result.success(MapPosition(it.latitude, it.longitude)))
                    }
                    .addOnFailureListener { cont.resumeWith(Result.failure(it)) }
        }
    }
}