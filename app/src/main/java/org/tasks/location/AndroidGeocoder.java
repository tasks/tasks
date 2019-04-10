package org.tasks.location;

import static com.todoroo.andlib.utility.AndroidUtilities.assertNotMainThread;
import static org.tasks.data.Place.newPlace;

import android.content.Context;
import android.location.Address;
import java.io.IOException;
import java.util.List;
import org.tasks.data.Place;

@SuppressWarnings("unused")
public class AndroidGeocoder implements Geocoder {

  private final Context context;

  public AndroidGeocoder(Context context) {
    this.context = context;
  }

  @Override
  public Place reverseGeocode(MapPosition mapPosition) throws IOException {
    assertNotMainThread();

    android.location.Geocoder geocoder = new android.location.Geocoder(context);
    List<Address> addresses =
        geocoder.getFromLocation(mapPosition.getLatitude(), mapPosition.getLongitude(), 1);
    Place place = newPlace(mapPosition);
    if (addresses.isEmpty()) {
      return place;
    }

    Address address = addresses.get(0);
    if (address.getMaxAddressLineIndex() >= 0) {
      place.setName(address.getAddressLine(0));
      StringBuilder builder = new StringBuilder(place.getName());
      for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
        builder.append(", ").append(address.getAddressLine(i));
      }
      place.setAddress(builder.toString());
    }
    if (address.hasLatitude() && address.hasLongitude()) {
      place.setLatitude(address.getLatitude());
      place.setLongitude(address.getLongitude());
    }
    place.setPhone(address.getPhone());
    place.setUrl(address.getUrl());
    return place;
  }
}
