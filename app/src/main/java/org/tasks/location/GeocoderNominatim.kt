package org.tasks.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.data.entity.Place
import org.tasks.http.HttpClientFactory
import org.tasks.http.HttpException
import org.tasks.location.GeocoderMapbox.Companion.asCoordinates
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
            Json.parseToJsonElement(json)
                .jsonObject["features"]
                ?.jsonArray
                ?.firstOrNull()
                ?.let { feature ->
                            val geocoding = feature
                                .jsonObject["properties"]!!
                                .jsonObject["geocoding"]!!
                            val geometry = feature.jsonObject["geometry"]!!
                            val coords = geometry.jsonObject["coordinates"]!!.asCoordinates
                            return Place(
                                name = geocoding.jsonObject["name"]?.jsonPrimitive?.content
                                    ?: geocoding.jsonObject["housenumber"]?.jsonPrimitive?.content
                                    ?.let { "$it ${geocoding.jsonObject["street"]?.jsonPrimitive?.content}" },
                                address = geocoding.jsonObject["label"]?.jsonPrimitive?.content,
                                longitude = coords.first,
                                latitude = coords.second,
                            )
                        }
    }
}