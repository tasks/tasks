package org.tasks.location;

import java.util.List;
import javax.inject.Inject;
import org.tasks.data.Location;

@SuppressWarnings("EmptyMethod")
public class GeofenceApi {

  @Inject
  public GeofenceApi() {}

  public void register(List<Location> activeGeofences) {}

  public void cancel(Location geofence) {}

  public void cancel(List<Location> geofences) {}

  public void cancel(long taskId) {}

  public void register(long taskId) {}

  public void registerAll() {}
}
