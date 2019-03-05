package org.tasks.location;

import android.annotation.SuppressLint;
import android.content.Context;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.List;
import org.tasks.R;
import org.tasks.data.Place;

public class GoogleMapFragment implements MapFragment, OnMapReadyCallback, OnMarkerClickListener {

  private final Context context;
  private final MapFragmentCallback callbacks;
  private final boolean dark;
  private final List<Marker> markers = new ArrayList<>();
  private GoogleMap map;

  GoogleMapFragment(
      Context context, SupportMapFragment fragment, MapFragmentCallback callbacks, boolean dark) {
    this.context = context;
    this.callbacks = callbacks;
    this.dark = dark;
    fragment.getMapAsync(this);
  }

  @Override
  public MapPosition getMapPosition() {
    CameraPosition cameraPosition = map.getCameraPosition();
    LatLng target = cameraPosition.target;
    return new MapPosition(target.latitude, target.longitude, cameraPosition.zoom);
  }

  @Override
  public void movePosition(MapPosition mapPosition, boolean animate) {
    CameraUpdate cameraUpdate =
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder()
                .target(new LatLng(mapPosition.getLatitude(), mapPosition.getLongitude()))
                .zoom(mapPosition.getZoom())
                .build());
    if (animate) {
      map.animateCamera(cameraUpdate);
    } else {
      map.moveCamera(cameraUpdate);
    }
  }

  @Override
  public void setMarkers(List<Place> places) {
    for (Marker marker : markers) {
      marker.remove();
    }
    markers.clear();
    for (Place place : places) {
      Marker marker =
          map.addMarker(
              new MarkerOptions().position(new LatLng(place.getLatitude(), place.getLongitude())));
      marker.setTag(place);
      markers.add(marker);
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(GoogleMap googleMap) {
    map = googleMap;
    map.setMyLocationEnabled(true);
    if (dark) {
      map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night));
    }
    UiSettings uiSettings = map.getUiSettings();
    uiSettings.setMyLocationButtonEnabled(false);
    uiSettings.setRotateGesturesEnabled(false);
    map.setOnMarkerClickListener(this);
    callbacks.onMapReady(this);
  }

  @Override
  public boolean onMarkerClick(Marker marker) {
    callbacks.onPlaceSelected((Place) marker.getTag());
    return true;
  }
}
