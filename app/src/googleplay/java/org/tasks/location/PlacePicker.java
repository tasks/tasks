package org.tasks.location;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;

import org.tasks.R;
import org.tasks.preferences.Preferences;

import timber.log.Timber;

public class PlacePicker {
    public static Intent getIntent(Activity activity) {
        com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder builder =
                new com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder();
        try {
            return builder.build(activity);
        } catch (GooglePlayServicesRepairableException e) {
            Timber.e(e, e.getMessage());
            activity.startActivity(e.getIntent());
        } catch (GooglePlayServicesNotAvailableException e) {
            Timber.e(e, e.getMessage());
            Toast.makeText(activity, R.string.common_google_play_services_notification_ticker, Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public static Geofence getPlace(Context context, Intent data, Preferences preferences) {
        Place place = com.google.android.gms.location.places.ui.PlacePicker.getPlace(context, data);
        LatLng latLng = place.getLatLng();
        Geofence geofence = new Geofence(place.getName().toString(), latLng.latitude, latLng.longitude, preferences.getIntegerFromString(R.string.p_geofence_radius, 250));
        Timber.i("Picked %s", geofence);
        return geofence;
    }
}
