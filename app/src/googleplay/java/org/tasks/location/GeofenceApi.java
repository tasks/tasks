package org.tasks.location;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.GeofencingRequest.Builder;
import com.google.android.gms.location.LocationServices;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import org.tasks.data.LocationDao;
import org.tasks.data.MergedGeofence;
import org.tasks.data.Place;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;
import timber.log.Timber;

public class GeofenceApi {

  private final Context context;
  private final PermissionChecker permissionChecker;
  private final LocationDao locationDao;

  @Inject
  public GeofenceApi(
      @ForApplication Context context,
      PermissionChecker permissionChecker,
      LocationDao locationDao) {
    this.context = context;
    this.permissionChecker = permissionChecker;
    this.locationDao = locationDao;
  }

  public void registerAll() {
    for (Place place : locationDao.getPlacesWithGeofences()) {
      update(place);
    }
  }

  public void update(long taskId) {
    update(locationDao.getPlaceForTask(taskId));
  }

  public void update(String place) {
    update(locationDao.getPlace(place));
  }

  public void update(@Nullable Place place) {
    if (place == null || !permissionChecker.canAccessLocation()) {
      return;
    }

    GeofencingClient client = LocationServices.getGeofencingClient(context);
    MergedGeofence geofence = locationDao.getGeofencesByPlace(place.getUid());
    if (geofence != null) {
      Timber.d("Adding geofence for %s", geofence);
      client.addGeofences(
          new Builder().addGeofence(toGoogleGeofence(geofence)).build(),
          PendingIntent.getBroadcast(
              context,
              0,
              new Intent(context, GeofenceTransitionsIntentService.Broadcast.class),
              PendingIntent.FLAG_UPDATE_CURRENT));
    } else {
      Timber.d("Removing geofence for %s", place);
      client.removeGeofences(ImmutableList.of(Long.toString(place.getId())));
    }
  }

  private com.google.android.gms.location.Geofence toGoogleGeofence(MergedGeofence geofence) {
    int transitionTypes = 0;
    if (geofence.isArrival()) {
      transitionTypes |= GeofencingRequest.INITIAL_TRIGGER_ENTER;
    }
    if (geofence.isDeparture()) {
      transitionTypes |= GeofencingRequest.INITIAL_TRIGGER_EXIT;
    }
    return new com.google.android.gms.location.Geofence.Builder()
        .setCircularRegion(geofence.getLatitude(), geofence.getLongitude(), geofence.getRadius())
        .setRequestId(geofence.getUid())
        .setTransitionTypes(transitionTypes)
        .setExpirationDuration(NEVER_EXPIRE)
        .build();
  }
}
