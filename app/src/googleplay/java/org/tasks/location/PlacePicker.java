package org.tasks.location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import org.tasks.R;
import org.tasks.data.Location;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class PlacePicker {

  public static Intent getIntent(Activity activity) {
    com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder builder =
        new com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder();
    try {
      return builder.build(activity);
    } catch (GooglePlayServicesRepairableException e) {
      Timber.e(e);
      activity.startActivity(e.getIntent());
    } catch (GooglePlayServicesNotAvailableException e) {
      Timber.e(e);
      Toast.makeText(
              activity, R.string.common_google_play_services_notification_ticker, Toast.LENGTH_LONG)
          .show();
    }
    return null;
  }

  public static Location getPlace(Context context, Intent data, Preferences preferences) {
    Place place = com.google.android.gms.location.places.ui.PlacePicker.getPlace(context, data);
    LatLng latLng = place.getLatLng();
    Location location = new Location();
    location.setName(place.getName().toString());
    CharSequence address = place.getAddress();
    if (address != null) {
      location.setAddress(place.getAddress().toString());
    }
    CharSequence phoneNumber = place.getPhoneNumber();
    if (phoneNumber != null) {
      location.setPhone(phoneNumber.toString());
    }
    Uri uri = place.getWebsiteUri();
    if (uri != null) {
      location.setUrl(uri.toString());
    }
    location.setLatitude(latLng.latitude);
    location.setLongitude(latLng.longitude);
    location.setRadius(preferences.getInt(R.string.p_default_location_radius, 250));
    Timber.i("Picked %s", location);
    return location;
  }
}
