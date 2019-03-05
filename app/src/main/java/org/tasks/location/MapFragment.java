package org.tasks.location;

import java.util.List;
import org.tasks.data.Place;

public interface MapFragment {

  MapPosition getMapPosition();

  void movePosition(MapPosition mapPosition, boolean animate);

  void setMarkers(List<Place> places);

  interface MapFragmentCallback {
    void onMapReady(MapFragment mapFragment);

    void onPlaceSelected(Place place);
  }
}
