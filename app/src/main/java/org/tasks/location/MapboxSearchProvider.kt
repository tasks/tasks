package org.tasks.location

import android.content.Context
import android.os.Bundle
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.data.Place
import org.tasks.data.Place.Companion.newPlace
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.coroutines.suspendCoroutine

class MapboxSearchProvider(private val context: Context) : PlaceSearchProvider {
    private var builder: MapboxGeocoding.Builder? = null

    override fun restoreState(savedInstanceState: Bundle?) {}

    override fun saveState(outState: Bundle) {}

    override fun getAttributionRes(dark: Boolean) = R.drawable.mapbox_logo_icon

    override suspend fun search(query: String, bias: MapPosition?): List<PlaceSearchResult> =
            withContext(Dispatchers.IO) {
                suspendCoroutine { cont ->
                    if (builder == null) {
                        val token = context.getString(R.string.mapbox_key)
                        builder = MapboxGeocoding.builder().autocomplete(true).accessToken(token)
                        if (bias != null) {
                            builder?.proximity(Point.fromLngLat(bias.longitude, bias.latitude))
                        }
                    }
                    builder
                            ?.query(query)
                            ?.build()
                            ?.enqueueCall(
                                    object : retrofit2.Callback<GeocodingResponse> {
                                        override fun onResponse(
                                                call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                                            val results: MutableList<PlaceSearchResult> = ArrayList()
                                            results.clear()
                                            for (feature in response.body()!!.features()) {
                                                results.add(toSearchResult(feature))
                                            }
                                            cont.resumeWith(Result.success(results))
                                        }

                                        override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                                            cont.resumeWith(Result.failure(t))
                                        }
                                    })
                }
            }

    override suspend fun fetch(placeSearchResult: PlaceSearchResult): Place =
            placeSearchResult.place

    private fun toSearchResult(feature: CarmenFeature): PlaceSearchResult {
        val place = newPlace(feature)
        return PlaceSearchResult(feature.id(), place.name, place.displayAddress, place)
    }
}