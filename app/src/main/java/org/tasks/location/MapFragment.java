package org.tasks.location;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import org.tasks.data.Place;

public interface MapFragment {

  void init(AppCompatActivity activity, MapFragmentCallback callback, boolean dark);

  @Nullable MapPosition getMapPosition();

  void movePosition(MapPosition mapPosition, boolean animate);

  void setMarkers(List<Place> places);

  void disableGestures();

  void showMyLocation();

  void onPause();

  void onResume();

  void onDestroy();

  interface MapFragmentCallback {
    void onMapReady(MapFragment mapFragment);

    void onPlaceSelected(Place place);
  }
}
