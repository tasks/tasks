package org.tasks.location

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.Place
import org.tasks.extensions.JsonObject.getOrNull
import org.tasks.extensions.JsonObject.getStringOrNull
import org.tasks.http.HttpClientFactory
import org.tasks.http.HttpException
import javax.inject.Inject

class GeocoderNominatim @Inject constructor(
        @ApplicationContext context: Context,
        private val httpClientFactory: HttpClientFactory,
) : Geocoder {
    private val url = context.getString(R.string.tasks_nominatim_url)

    override suspend fun reverseGeocode(mapPosition: MapPosition): Place? =
            withContext(Dispatchers.IO) {
                val client = httpClientFactory.newClient(foreground = true)
                val url = "$url/reverse?format=geocodejson&lat=${mapPosition.latitude}&lon=${mapPosition.longitude}"
                val response = client.newCall(
                        Request.Builder().get().url(url).addHeader(USER_AGENT, UA_VALUE).build()
                ).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonToPlace(it) }
                } else {
                    throw HttpException(response.code, response.message)
                }
            }

    companion object {
        private const val USER_AGENT = "User-Agent"
        private const val UA_VALUE = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}-${BuildConfig.BUILD_TYPE})"

        internal fun jsonToPlace(json: String): Place? =
                JsonParser
                        .parseString(json).asJsonObject.getAsJsonArray("features")
                        .takeIf { it.size() > 0 }?.get(0)?.asJsonObject
                        ?.let { feature ->
                            val geocoding = feature
                                    .get("properties").asJsonObject
                                    .get("geocoding").asJsonObject
                            val geometry = feature.get("geometry").asJsonObject
                            val coords = geometry.get("coordinates").asCoordinates
                            return Place(
                                name = geocoding.getStringOrNull("name")
                                    ?: geocoding.getStringOrNull("housenumber")
                                        ?.let { "$it ${geocoding.get("street").asString}" },
                                address = geocoding.getOrNull("label")?.asString,
                                longitude = coords.first,
                                latitude = coords.second,
                            )
                        }

        private val JsonElement.asCoordinates: Pair<Double, Double>
            get() = asJsonArray.let { Pair(it[0].asDouble, it[1].asDouble) }
    }
}