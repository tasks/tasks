package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tasks.R
import org.tasks.data.entity.Place
import org.tasks.location.MapFragment.MapFragmentCallback
import javax.inject.Inject

class GoogleMapFragment @Inject constructor(
        @ApplicationContext private val context: Context
) : MapFragment, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private val markers: MutableList<Marker> = ArrayList()
    private lateinit var callback: MapFragmentCallback
    private var dark = false
    private var map: GoogleMap? = null
    private var circle: Circle? = null

    override fun init(activity: AppCompatActivity, callback: MapFragmentCallback, dark: Boolean) {
        this.callback = callback
        this.dark = dark
        val fragmentManager = activity.supportFragmentManager
        var mapFragment = fragmentManager.findFragmentByTag(FRAG_TAG_MAP) as SupportMapFragment?
        if (mapFragment == null) {
            mapFragment = SupportMapFragment()
            fragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit()
        }
        mapFragment.getMapAsync(this)
    }

    override val mapPosition: MapPosition?
        get() = map?.cameraPosition?.let {
            val target = it.target
            return MapPosition(target.latitude, target.longitude, it.zoom)
        }

    override fun movePosition(mapPosition: MapPosition, animate: Boolean) {
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                        .target(LatLng(mapPosition.latitude, mapPosition.longitude))
                        .zoom(mapPosition.zoom)
                        .build())
        if (animate) {
            map!!.animateCamera(cameraUpdate)
        } else {
            map!!.moveCamera(cameraUpdate)
        }
    }

    override fun setMarkers(places: List<Place>) {
        if (map == null) {
            return
        }
        markers
            .onEach { it.remove() }
            .clear()
        places
            .mapNotNull { map?.addMarker(it) }
            .let { markers.addAll(it) }
    }

    override fun disableGestures() {
        map?.uiSettings?.setAllGesturesEnabled(false)
    }

    @SuppressLint("MissingPermission")
    override fun showMyLocation() {
        map?.isMyLocationEnabled = true
    }

    override fun showCircle(radius: Double, latitude: Double, longitude: Double) {
        circle?.remove()
        circle = map?.addCircle(
            CircleOptions()
                .radius(radius)
                .center(LatLng(latitude, longitude))
                .strokeColor(context.getColor(R.color.map_circle_stroke))
                .fillColor(context.getColor(R.color.map_circle_fill))
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (dark) {
            map!!.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night))
        }
        val uiSettings = map!!.uiSettings
        uiSettings.isMyLocationButtonEnabled = false
        uiSettings.isRotateGesturesEnabled = false
        map!!.setOnMarkerClickListener(this)
        callback.onMapReady(this)
    }

    override fun onPause() {}

    override fun onResume() {}

    override fun onDestroy() {}

    override fun onMarkerClick(marker: Marker): Boolean {
        callback.onPlaceSelected(marker.tag as Place)
        return true
    }

    companion object {
        private const val FRAG_TAG_MAP = "frag_tag_map"

        private fun GoogleMap.addMarker(place: Place) =
            addMarker(MarkerOptions().position(LatLng(place.latitude, place.longitude)))?.apply {
                tag = place
            }
    }
}