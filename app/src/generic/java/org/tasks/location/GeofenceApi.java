package org.tasks.location;

import javax.inject.Inject;
import org.jetbrains.annotations.Nullable;
import org.tasks.data.Place;

@SuppressWarnings("EmptyMethod")
public class GeofenceApi {

  @Inject
  public GeofenceApi() {}

  public void registerAll() {}

  public void update(@Nullable Place place) {}

  public void update(String place) {}

  public void update(long taskId) {}
}
