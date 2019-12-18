package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import org.tasks.data.CaldavTask;

public class CaldavTaskMaker {

  public static final Property<CaldavTask, String> CALENDAR = newProperty();
  public static final Property<CaldavTask, Long> TASK = newProperty();
  public static final Property<CaldavTask, String> REMOTE_ID = newProperty();
  public static final Property<CaldavTask, String> REMOTE_PARENT = newProperty();

  private static final Instantiator<CaldavTask> instantiator =
      lookup -> {
        CaldavTask task =
            new CaldavTask(lookup.valueOf(TASK, 1L), lookup.valueOf(CALENDAR, "calendar"));
        task.setRemoteId(lookup.valueOf(REMOTE_ID, task.getRemoteId()));
        task.setRemoteParent(lookup.valueOf(REMOTE_PARENT, (String) null));
        return task;
      };

  @SafeVarargs
  public static CaldavTask newCaldavTask(PropertyValue<? super CaldavTask, ?>... properties) {
    return make(instantiator, properties);
  }
}
