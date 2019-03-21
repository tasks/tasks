package org.tasks.location;

import static com.google.common.collect.Lists.newArrayList;
import static com.todoroo.astrid.data.Task.NO_ID;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import org.tasks.data.Geofence;
import org.tasks.data.Location;
import org.tasks.data.LocationDao;
import org.tasks.data.Place;

public class GeofenceService {

  private final GeofenceApi geofenceApi;
  private final LocationDao locationDao;

  @Inject
  public GeofenceService(GeofenceApi geofenceApi, LocationDao locationDao) {
    this.geofenceApi = geofenceApi;
    this.locationDao = locationDao;
  }

  public List<Location> getGeofences(long taskId) {
    return taskId == NO_ID ? emptyList() : locationDao.getGeofences(taskId);
  }

  public void setupGeofences() {
    geofenceApi.register(getActiveGeofences());
  }

  public void setupGeofences(long taskId) {
    geofenceApi.register(getGeofencesForTask(taskId));
  }

  public void cancelGeofences(long taskId) {
    for (Location location : getGeofences(taskId)) {
      geofenceApi.cancel(location);
    }
  }

  public boolean synchronizeGeofences(final long taskId, Set<Location> locations) {
    boolean changed = synchronizeMetadata(taskId, newArrayList(locations), geofenceApi::cancel);

    if (changed) {
      setupGeofences(taskId);
    }

    return changed;
  }

  private boolean synchronizeMetadata(
      long taskId, List<Location> locations, final SynchronizeGeofenceCallback callback) {
    boolean dirty = false;
    for (Location metadatum : locations) {
      metadatum.setTask(taskId);
      metadatum.setId(0L);
    }

    for (Location item : locationDao.getGeofences(taskId)) {
      long id = item.getId();

      // clear item id when matching with incoming values
      item.setId(0L);

      if (locations.contains(item)) {
        locations.remove(item);
      } else {
        // not matched. cut it
        item.setId(id);
        if (callback != null) {
          callback.beforeDelete(item);
        }
        locationDao.delete(item.geofence);
        dirty = true;
      }
    }

    // everything that remains shall be written
    for (Location location : locations) {
      Place place = location.place;
      locationDao.insert(place);
      Geofence geofence = location.geofence;
      geofence.setPlace(place.getUid());
      locationDao.insert(geofence);
      dirty = true;
    }

    return dirty;
  }

  private List<Location> getActiveGeofences() {
    return locationDao.getActiveGeofences();
  }

  private List<Location> getGeofencesForTask(long taskId) {
    return locationDao.getActiveGeofences(taskId);
  }

  interface SynchronizeGeofenceCallback {

    void beforeDelete(Location location);
  }
}
