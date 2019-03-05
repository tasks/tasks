package org.tasks.location;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMarkerClickListener;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tasks.data.Place;

public class MapboxMapFragment implements MapFragment, OnMapReadyCallback, OnMarkerClickListener {

  private final Context context;
  private final MapFragmentCallback callbacks;
  private final boolean dark;
  private MapboxMap map;
  private Map<Marker, Place> markers = new HashMap<>();

  MapboxMapFragment(
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
    return new MapPosition(
        target.getLatitude(), target.getLongitude(), (float) cameraPosition.zoom);
  }

  @Override
  public void movePosition(MapPosition mapPosition, boolean animate) {
    CameraUpdate cameraUpdate =
        CameraUpdateFactory.newCameraPosition(
            new CameraPosition.Builder()
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
    for (Marker marker : map.getMarkers()) {
      map.removeMarker(marker);
    }
    markers.clear();
    for (Place place : places) {
      Marker marker =
          map.addMarker(
              new MarkerOptions()
                  .setPosition(new LatLng(place.getLatitude(), place.getLongitude())));
      markers.put(marker, place);
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    map = mapboxMap;
    map.getUiSettings().setRotateGesturesEnabled(false);
    map.setStyle(
        dark ? Style.DARK : Style.MAPBOX_STREETS,
        style -> {
          LocationComponent locationComponent = map.getLocationComponent();
          locationComponent.activateLocationComponent(context, style);
          locationComponent.setLocationComponentEnabled(true);
          locationComponent.setCameraMode(CameraMode.NONE);
          locationComponent.setRenderMode(RenderMode.NORMAL);
        });
    map.setOnMarkerClickListener(this);
    callbacks.onMapReady(this);
  }

  @Override
  public boolean onMarkerClick(@NonNull Marker marker) {
    Place place = markers.get(marker);
    callbacks.onPlaceSelected(place);
    return false;
  }
}
