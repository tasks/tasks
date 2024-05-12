package org.tasks.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tasks.DebugNetworkInterceptor
import org.tasks.R
import org.tasks.data.entity.Place
import org.tasks.preferences.Preferences
import java.io.IOException
import javax.inject.Inject

class GeocoderMapbox @Inject constructor(
        @ApplicationContext context: Context,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
) : Geocoder {
    private val token = context.getString(R.string.mapbox_key)

    override suspend fun reverseGeocode(mapPosition: MapPosition): Place? =
            withContext(Dispatchers.IO) {
                val builder = OkHttpClient().newBuilder()
                if (preferences.isFlipperEnabled) {
                    interceptor.apply(builder)
                }
                val client = builder.build()
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${mapPosition.longitude},${mapPosition.latitude}.json?access_token=$token"
                val response = client.newCall(Request.Builder().get().url(url).build()).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonToPlace(it) }
                } else {
                    throw IOException("${response.code} ${response.message}")
                }
            }

    companion object {
        internal fun jsonToPlace(json: String): Place? =
            Json.parseToJsonElement(json)
                .jsonObject["features"]
                ?.jsonArray
                ?.firstOrNull()
                ?.toPlace()

        internal fun JsonElement.toPlace(): Place {
            val text = jsonObject["text"]!!.jsonPrimitive.content
            val coords = jsonObject["center"]!!.asCoordinates
            return Place(
                name = if (jsonObject["place_type"]!!.asStringList.contains("address")) {
                    jsonObject["address"]
                        ?.jsonPrimitive
                        ?.content
                        ?.let { "$it $text" }
                        ?: text
                } else {
                    text
                },
                address = jsonObject["place_name"]!!.jsonPrimitive.content,
                longitude = coords.first,
                latitude = coords.second,
            )
        }

        private val JsonElement.asStringList: List<String>
            get() = jsonArray.map { it.jsonPrimitive.content }

        val JsonElement.asCoordinates: Pair<Double, Double>
            get() = jsonArray.let { Pair(it[0].jsonPrimitive.double, it[1].jsonPrimitive.double) }
    }
}
