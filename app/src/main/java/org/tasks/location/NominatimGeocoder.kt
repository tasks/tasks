package org.tasks.location

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tasks.DebugNetworkInterceptor
import org.tasks.data.Place
import org.tasks.data.Place.Companion.newPlace
import org.tasks.preferences.Preferences
import java.io.IOException
import javax.inject.Inject

class NominatimGeocoder @Inject constructor(
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
) : Geocoder {
    override suspend fun reverseGeocode(mapPosition: MapPosition): Place? =
            withContext(Dispatchers.IO) {
                val builder = OkHttpClient().newBuilder()
                if (preferences.isFlipperEnabled) {
                    interceptor.apply(builder)
                }
                val client = builder.build()
                val url = "https://nominatim.openstreetmap.org/reverse?format=geocodejson&lat=${mapPosition.latitude}&lon=${mapPosition.longitude}"
                val response = client.newCall(Request.Builder().get().url(url).build()).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonToPlace(it) }
                } else {
                    throw IOException("${response.code} ${response.message}")
                }
            }

    companion object {
        internal fun jsonToPlace(json: String): Place? =
                JsonParser
                        .parseString(json).asJsonObject.getAsJsonArray("features")
                        .takeIf { it.size() > 0 }?.get(0)?.asJsonObject
                        ?.let { feature ->
                            val geocoding = feature
                                    .get("properties").asJsonObject
                                    .get("geocoding").asJsonObject
                            val geometry = feature.get("geometry").asJsonObject
                            newPlace().apply {
                                val type = geocoding.get("type").asString
                                name = if (type.equals("house")) {
                                    "${geocoding.get("housenumber").asString} ${geocoding.get("street").asString}"
                                } else {
                                    geocoding.get("name").asString
                                }
                                address = geocoding.get("label").asString
                                geometry.get("coordinates").asCoordinates.let {
                                    longitude = it.first
                                    latitude = it.second
                                }
                            }
                        }

        private val JsonElement.asCoordinates: Pair<Double, Double>
            get() = asJsonArray.let { Pair(it[0].asDouble, it[1].asDouble) }
    }
}