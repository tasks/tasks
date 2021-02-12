package org.tasks.location

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.geojson.Point
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.Place
import org.tasks.data.Place.Companion.newPlace
import timber.log.Timber
import javax.inject.Inject

class MapboxGeocoder @Inject constructor(@ApplicationContext context: Context) : Geocoder {
    private val token: String = context.getString(R.string.mapbox_key)

    override suspend fun reverseGeocode(mapPosition: MapPosition): Place =
            withContext(Dispatchers.IO) {
                val response = MapboxGeocoding.builder()
                        .accessToken(token)
                        .query(Point.fromLngLat(mapPosition.longitude, mapPosition.latitude))
                        .build()
                        .executeCall()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    Timber.d(prettyPrint(body.toJson()))
                    val features = body.features()
                    if (features.size > 0) {
                        return@withContext newPlace(features[0])
                    }
                } else {
                    Timber.e(response.errorBody()!!.string())
                }
                newPlace(mapPosition)!!
            }

    companion object {
        private fun prettyPrint(json: String): String {
            return if (BuildConfig.DEBUG) {
                GsonBuilder().setPrettyPrinting().create().toJson(JsonParser().parse(json))
            } else json
        }
    }
}