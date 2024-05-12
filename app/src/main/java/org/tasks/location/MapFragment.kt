package org.tasks.location

import androidx.appcompat.app.AppCompatActivity
import org.tasks.data.entity.Place

interface MapFragment {
    fun init(activity: AppCompatActivity, callback: MapFragmentCallback, dark: Boolean)

    val mapPosition: MapPosition?

    fun movePosition(mapPosition: MapPosition, animate: Boolean)

    fun setMarkers(places: List<Place>)

    fun disableGestures()

    fun showMyLocation()

    fun showCircle(radius: Double, latitude: Double, longitude: Double)

    fun onPause()

    fun onResume()

    fun onDestroy()

    interface MapFragmentCallback {
        fun onMapReady(mapFragment: MapFragment)
        fun onPlaceSelected(place: Place)
    }
}