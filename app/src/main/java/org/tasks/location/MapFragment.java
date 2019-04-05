package org.tasks.location;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import java.util.List;
import org.tasks.data.Place;

public interface MapFragment {

  void init(FragmentManager fragmentManager, MapFragmentCallback callback, boolean dark);

  @Nullable MapPosition getMapPosition();

  void movePosition(MapPosition mapPosition, boolean animate);

  void setMarkers(List<Place> places);

  void showMyLocation();

  int getMarkerId();

  interface MapFragmentCallback {
    void onMapReady(MapFragment mapFragment);

    void onPlaceSelected(Place place);
  }
}
