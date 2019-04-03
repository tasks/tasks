package org.tasks.location;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static java.util.Collections.singletonList;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.GeofencingRequest.Builder;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import javax.inject.Inject;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.injection.ForApplication;
import org.tasks.preferences.PermissionChecker;

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
    register(locationDao.getActiveGeofences());
  }

  public void register(long taskId) {
    register(locationDao.getActiveGeofences(taskId));
  }

  @SuppressLint("MissingPermission")
  public void register(final List<Location> locations) {
    if (!permissionChecker.canAccessLocation()) {
      return;
    }

    List<Geofence> requests = getRequests(locations);
    if (!requests.isEmpty()) {
      LocationServices.getGeofencingClient(context)
          .addGeofences(
              new Builder().addGeofences(requests).build(),
              PendingIntent.getBroadcast(
                  context,
                  0,
                  new Intent(context, GeofenceTransitionsIntentService.Broadcast.class),
                  PendingIntent.FLAG_UPDATE_CURRENT));
    }
  }

  public void cancel(long taskId) {
    cancel(locationDao.getGeofences(taskId));
  }

  public void cancel(final Location location) {
    if (location != null) {
      cancel(singletonList(location));
    }
  }

  public void cancel(final List<Location> locations) {
    if (!permissionChecker.canAccessLocation()) {
      return;
    }
    List<String> requestIds = getRequestIds(locations);
    if (!requestIds.isEmpty()) {
      LocationServices.getGeofencingClient(context).removeGeofences(requestIds);
    }
  }

  @SuppressWarnings("ConstantConditions")
  private List<String> getRequestIds(List<Location> locations) {
    return transform(newArrayList(filter(locations, notNull())), l -> Long.toString(l.getId()));
  }

  private List<com.google.android.gms.location.Geofence> getRequests(List<Location> locations) {
    return transform(
        newArrayList(filter(locations, l -> l != null && (l.isArrival() || l.isDeparture()))),
        this::toGoogleGeofence);
  }

  private com.google.android.gms.location.Geofence toGoogleGeofence(Location location) {
    int transitionTypes = 0;
    if (location.isArrival()) {
      transitionTypes |= GeofencingRequest.INITIAL_TRIGGER_ENTER;
    }
    if (location.isDeparture()) {
      transitionTypes |= GeofencingRequest.INITIAL_TRIGGER_EXIT;
    }
    return new com.google.android.gms.location.Geofence.Builder()
        .setCircularRegion(location.getLatitude(), location.getLongitude(), location.getRadius())
        .setRequestId(Long.toString(location.getId()))
        .setTransitionTypes(transitionTypes)
        .setExpirationDuration(NEVER_EXPIRE)
        .build();
  }
}
