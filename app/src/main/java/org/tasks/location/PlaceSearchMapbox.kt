package org.tasks.location

import android.content.Context
import android.os.Bundle
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tasks.DebugNetworkInterceptor
import org.tasks.R
import org.tasks.data.entity.Place
import org.tasks.location.GeocoderMapbox.Companion.toPlace
import org.tasks.preferences.Preferences
import java.io.IOException
import javax.inject.Inject

class PlaceSearchMapbox @Inject constructor(
        @ApplicationContext context: Context,
        private val preferences: Preferences,
        private val interceptor: DebugNetworkInterceptor,
) : PlaceSearch {
    val token = context.getString(R.string.mapbox_key)

    override fun restoreState(savedInstanceState: Bundle?) {}

    override fun saveState(outState: Bundle) {}

    override fun getAttributionRes(dark: Boolean) = R.drawable.mapbox_logo_icon

    override suspend fun search(query: String, bias: MapPosition?): List<PlaceSearchResult> =
            withContext(Dispatchers.IO) {
                val builder = OkHttpClient().newBuilder()
                if (preferences.isFlipperEnabled) {
                    interceptor.apply(builder)
                }
                val proximity = bias?.let {
                    "&proximity=${bias.longitude},${bias.latitude}"
                }
                val client = builder.build()
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$query.json?access_token=$token$proximity"
                val response = client.newCall(Request.Builder().get().url(url).build()).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { jsonToSearchResults(it) } ?: emptyList()
                } else {
                    throw IOException("${response.code} ${response.message}")
                }
            }

    override suspend fun fetch(placeSearchResult: PlaceSearchResult): Place =
            placeSearchResult.place

    companion object {
        internal fun jsonToSearchResults(json: String): List<PlaceSearchResult> =
                JsonParser
                        .parseString(json).asJsonObject.getAsJsonArray("features")
                        .map { it.asJsonObject }
                        .map {
                            val place = toPlace(it)
                            PlaceSearchResult(
                                    it.get("id").asString,
                                    place.name,
                                    place.displayAddress,
                                    place
                            )
                        }
    }
}