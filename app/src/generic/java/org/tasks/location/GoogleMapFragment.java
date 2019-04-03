package org.tasks.location;

import android.content.Context;
import androidx.fragment.app.FragmentManager;
import java.util.List;
import org.tasks.data.Place;

public class GoogleMapFragment implements MapFragment {

  public GoogleMapFragment(Context context) {}

  @Override
  public void init(FragmentManager fragmentManager, MapFragmentCallback callback, boolean dark) {}

  @Override
  public MapPosition getMapPosition() {
    return null;
  }

  @Override
  public void movePosition(MapPosition mapPosition, boolean animate) {}

  @Override
  public void setMarkers(List<Place> places) {}

  @Override
  public void showMyLocation() {}
}
