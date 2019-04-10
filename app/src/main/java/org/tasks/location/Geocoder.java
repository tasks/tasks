package org.tasks.location;

import java.io.IOException;
import org.tasks.data.Place;

public interface Geocoder {
  Place reverseGeocode(MapPosition mapPosition) throws IOException;
}
