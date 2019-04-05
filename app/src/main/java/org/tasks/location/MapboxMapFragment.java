package org.tasks.location;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import com.mapbox.mapboxsdk.Mapbox;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.tasks.R;
import org.tasks.data.Place;

public class MapboxMapFragment implements MapFragment, OnMapReadyCallback, OnMarkerClickListener {

  private static final String FRAG_TAG_MAP = "frag_tag_map";
  private final Context context;
  private MapFragmentCallback callbacks;
  private boolean dark;
  private MapboxMap map;
  private Map<Marker, Place> markers = new HashMap<>();

  public MapboxMapFragment(Context context) {
    this.context = context;
  }

  @Override
  public void init(FragmentManager fragmentManager, MapFragmentCallback callbacks, boolean dark) {
    this.callbacks = callbacks;
    this.dark = dark;
    Mapbox.getInstance(context, context.getString(R.string.mapbox_key));
    com.mapbox.mapboxsdk.maps.SupportMapFragment mapFragment =
        (com.mapbox.mapboxsdk.maps.SupportMapFragment)
            fragmentManager.findFragmentByTag(FRAG_TAG_MAP);
    if (mapFragment == null) {
      mapFragment = new com.mapbox.mapboxsdk.maps.SupportMapFragment();
      fragmentManager.beginTransaction().replace(R.id.map, mapFragment).commit();
    }
    mapFragment.getMapAsync(this);
  }

  @Override
  public MapPosition getMapPosition() {
    if (map == null) {
      return null;
    }
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
    if (map == null) {
      return;
    }
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
  public void showMyLocation() {
    LocationComponent locationComponent = map.getLocationComponent();
    locationComponent.activateLocationComponent(context, map.getStyle());
    locationComponent.setLocationComponentEnabled(true);
    locationComponent.setCameraMode(CameraMode.NONE);
    locationComponent.setRenderMode(RenderMode.NORMAL);
  }

  @Override
  public void onMapReady(@NonNull MapboxMap mapboxMap) {
    map = mapboxMap;
    map.getUiSettings().setRotateGesturesEnabled(false);
    map.setOnMarkerClickListener(this);
    map.setStyle(dark ? Style.DARK : Style.MAPBOX_STREETS, style -> callbacks.onMapReady(this));
  }

  @Override
  public int getMarkerId() {
    return R.id.mapbox_marker;
  }

  @Override
  public boolean onMarkerClick(@NonNull Marker marker) {
    Place place = markers.get(marker);
    callbacks.onPlaceSelected(place);
    return false;
  }
}
