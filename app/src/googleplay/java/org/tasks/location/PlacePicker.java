package org.tasks.location;

import android.app.Activity;
import android.content.Intent;
import org.tasks.R;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class PlacePicker {

  static final String EXTRA_PLACE = "extra_place";

  public static Intent getIntent(Activity activity) {
    return new Intent(activity, LocationPicker.class);
  }

  public static Location getPlace(Intent data, Preferences preferences) {
    org.tasks.data.Place result = data.getParcelableExtra(EXTRA_PLACE);

    Geofence g = new Geofence();
    g.setRadius(preferences.getInt(R.string.p_default_location_radius, 250));
    int defaultReminders =
        preferences.getIntegerFromString(R.string.p_default_location_reminder_key, 1);
    g.setArrival(defaultReminders == 1 || defaultReminders == 3);
    g.setDeparture(defaultReminders == 2 || defaultReminders == 3);

    Location location = new Location(g, result);
    Timber.i("Picked %s", location);
    return location;
  }
}
