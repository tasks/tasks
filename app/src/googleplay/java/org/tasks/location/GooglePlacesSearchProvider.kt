package org.tasks.location

import android.content.Context
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place.Field
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.*
import org.tasks.Callback
import org.tasks.R
import org.tasks.data.Place
import org.tasks.data.Place.Companion.newPlace

class GooglePlacesSearchProvider(private val context: Context) : PlaceSearchProvider {
    private var token: AutocompleteSessionToken? = null
    private var placesClient: PlacesClient? = null

    override fun restoreState(savedInstanceState: Bundle) {
        token = savedInstanceState.getParcelable(EXTRA_SESSION_TOKEN)
    }

    override fun saveState(outState: Bundle) {
        outState.putParcelable(EXTRA_SESSION_TOKEN, token)
    }

    override fun getAttributionRes(dark: Boolean): Int {
        return if (dark) R.drawable.places_powered_by_google_dark else R.drawable.places_powered_by_google_light
    }

    override fun search(
            query: String,
            bias: MapPosition?,
            onSuccess: Callback<List<PlaceSearchResult>>,
            onError: Callback<String>) {
        if (!Places.isInitialized()) {
            Places.initialize(context, context.getString(R.string.google_key))
        }
        if (placesClient == null) {
            placesClient = Places.createClient(context)
        }
        if (token == null) {
            token = AutocompleteSessionToken.newInstance()
        }
        val request = FindAutocompletePredictionsRequest.builder().setSessionToken(token).setQuery(query)
        if (bias != null) {
            request.setLocationBias(
                    RectangularBounds.newInstance(
                            LatLngBounds.builder()
                                    .include(LatLng(bias.latitude, bias.longitude))
                                    .build()))
        }
        placesClient!!
                .findAutocompletePredictions(request.build())
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse -> onSuccess.call(toSearchResults(response.autocompletePredictions)) }
                .addOnFailureListener { e: Exception -> onError.call(e.message) }
    }

    override fun fetch(
            placeSearchResult: PlaceSearchResult, onSuccess: Callback<Place>, onError: Callback<String>) {
        placesClient!!
                .fetchPlace(
                        FetchPlaceRequest.builder(
                                placeSearchResult.id,
                                listOf(
                                        Field.ID,
                                        Field.LAT_LNG,
                                        Field.ADDRESS,
                                        Field.WEBSITE_URI,
                                        Field.NAME,
                                        Field.PHONE_NUMBER))
                                .setSessionToken(token)
                                .build())
                .addOnSuccessListener { result: FetchPlaceResponse -> onSuccess.call(toPlace(result)) }
                .addOnFailureListener { e: Exception -> onError.call(e.message) }
    }

    private fun toSearchResults(predictions: List<AutocompletePrediction>): List<PlaceSearchResult> {
        return predictions.map {
            PlaceSearchResult(
                    it.placeId,
                    it.getPrimaryText(null).toString(),
                    it.getSecondaryText(null).toString())
        }
    }

    private fun toPlace(fetchPlaceResponse: FetchPlaceResponse): Place {
        val place = fetchPlaceResponse.place
        val result = newPlace()
        result.name = place.name
        val address: CharSequence? = place.address
        if (address != null) {
            result.address = place.address
        }
        val phoneNumber: CharSequence? = place.phoneNumber
        if (phoneNumber != null) {
            result.phone = phoneNumber.toString()
        }
        val uri = place.websiteUri
        if (uri != null) {
            result.url = uri.toString()
        }
        val latLng = place.latLng
        result.latitude = latLng!!.latitude
        result.longitude = latLng.longitude
        return result
    }

    companion object {
        private const val EXTRA_SESSION_TOKEN = "extra_session_token"
    }
}