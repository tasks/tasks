package org.tasks.location

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.tasks.R
import org.tasks.data.Place
import org.tasks.location.MapFragment.MapFragmentCallback
import java.util.*

class GoogleMapFragment(private val context: Context) : MapFragment, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private val markers: MutableList<Marker> = ArrayList()
    private lateinit var callbacks: MapFragmentCallback
    private var dark = false
    private var map: GoogleMap? = null

    override fun init(activity: AppCompatActivity, callbacks: MapFragmentCallback, dark: Boolean) {
        this.callbacks = callbacks
        this.dark = dark
        val fragmentManager = activity.supportFragmentManager
        var mapFragment = fragmentManager.findFragmentByTag(FRAG_TAG_MAP) as SupportMapFragment?
        if (mapFragment == null) {
            mapFragment = SupportMapFragment()
            fragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit()
        }
        mapFragment.getMapAsync(this)
    }

    override fun getMapPosition(): MapPosition? {
        if (map == null) {
            return null
        }
        val cameraPosition = map!!.cameraPosition
        val target = cameraPosition.target
        return MapPosition(target.latitude, target.longitude, cameraPosition.zoom)
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
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
        for (place in places) {
            val marker = map!!.addMarker(
                    MarkerOptions().position(LatLng(place.latitude, place.longitude)))
            marker.tag = place
            markers.add(marker)
        }
    }

    override fun disableGestures() {
        map!!.uiSettings.setAllGesturesEnabled(false)
    }

    @SuppressLint("MissingPermission")
    override fun showMyLocation() {
        map!!.isMyLocationEnabled = true
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
        callbacks.onMapReady(this)
    }

    override fun onPause() {}

    override fun onResume() {}

    override fun onDestroy() {}

    override fun onMarkerClick(marker: Marker): Boolean {
        callbacks.onPlaceSelected(marker.tag as Place?)
        return true
    }

    companion object {
        private const val FRAG_TAG_MAP = "frag_tag_map"
    }
}