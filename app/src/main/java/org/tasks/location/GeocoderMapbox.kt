package org.tasks.location

import android.content.Context
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tasks.DebugNetworkInterceptor
import org.tasks.R
import org.tasks.data.Place
import org.tasks.extensions.JsonObject.getStringOrNull
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
                JsonParser
                        .parseString(json).asJsonObject.getAsJsonArray("features")
                        .takeIf { it.size() > 0 }?.get(0)?.asJsonObject
                        ?.let { toPlace(it) }

        internal fun toPlace(feature: JsonObject): Place {
            val text = feature.get("text").asString
            val coords = feature.get("center").asCoordinates
            return Place(
                name = if (feature.get("place_type").asStringList.contains("address")) {
                    feature
                        .getStringOrNull("address")
                        ?.let { "$it $text" }
                        ?: text
                } else {
                    text
                },
                address = feature.get("place_name").asString,
                longitude = coords.first,
                latitude = coords.second,
            )
        }

        private val JsonElement.asStringList: List<String>
            get() = asJsonArray.map { it.asString }

        private val JsonElement.asCoordinates: Pair<Double, Double>
            get() = asJsonArray.let { Pair(it[0].asDouble, it[1].asDouble) }
    }
}