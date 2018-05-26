package org.tasks.location;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.tasks.R;
import org.tasks.data.Location;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;
import timber.log.Timber;

public class GeofenceApi {

  private final Context context;
  private final Preferences preferences;
  private final PermissionChecker permissionChecker;

  @Inject
  public GeofenceApi(
      @ForApplication Context context,
      Preferences preferences,
      PermissionChecker permissionChecker) {
    this.context = context;
    this.preferences = preferences;
    this.permissionChecker = permissionChecker;
  }

  public void register(final List<Location> locations) {
    if (locations.isEmpty() || !permissionChecker.canAccessLocation()) {
      return;
    }

    newClient(
        client -> {
          @SuppressWarnings("ResourceType")
          @SuppressLint("MissingPermission")
          PendingResult<Status> result =
              LocationServices.GeofencingApi.addGeofences(
                  client,
                  getRequests(locations),
                  PendingIntent.getBroadcast(
                      context,
                      0,
                      new Intent(context, GeofenceTransitionsIntentService.Broadcast.class),
                      PendingIntent.FLAG_UPDATE_CURRENT));
          result.setResultCallback(
              status -> {
                if (status.isSuccess()) {
                  Timber.i("Registered %s", locations);
                } else {
                  Timber.e("Failed to register %s", locations);
                }

                client.disconnect();
              });
        });
  }

  public void cancel(final Location location) {
    cancel(singletonList(location));
  }

  public void cancel(final List<Location> locations) {
    if (locations.isEmpty() || !permissionChecker.canAccessLocation()) {
      return;
    }

    List<String> ids = Lists.transform(locations, geofence -> Long.toString(geofence.getId()));

    newClient(
        client ->
            LocationServices.GeofencingApi.removeGeofences(client, ids)
                .setResultCallback(
                    status -> {
                      if (status.isSuccess()) {
                        Timber.i("Removed %s", locations);
                      } else {
                        Timber.e("Failed to remove %s", locations);
                      }

                      client.disconnect();
                    }));
  }

  private void newClient(final GoogleApi.GoogleApiClientConnectionHandler handler) {
    new GoogleApi(context).connect(handler);
  }

  private List<com.google.android.gms.location.Geofence> getRequests(List<Location> locations) {
    return newArrayList(transform(locations, this::toGoogleGeofence));
  }

  private com.google.android.gms.location.Geofence toGoogleGeofence(Location location) {
    int radius = preferences.getIntegerFromString(R.string.p_geofence_radius, 250);
    int responsiveness =
        (int)
            TimeUnit.SECONDS.toMillis(
                preferences.getIntegerFromString(R.string.p_geofence_responsiveness, 60));
    return new com.google.android.gms.location.Geofence.Builder()
        .setCircularRegion(location.getLatitude(), location.getLongitude(), radius)
        .setNotificationResponsiveness(responsiveness)
        .setRequestId(Long.toString(location.getId()))
        .setTransitionTypes(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .setExpirationDuration(NEVER_EXPIRE)
        .build();
  }
}
