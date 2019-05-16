package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import com.todoroo.astrid.helper.UUIDHelper;
import org.tasks.data.GoogleTask;

public class GoogleTaskMaker {

  public static final Property<GoogleTask, String> LIST = newProperty();
  public static final Property<GoogleTask, Integer> ORDER = newProperty();
  public static final Property<GoogleTask, String> REMOTE_ID = newProperty();
  public static final Property<GoogleTask, Integer> TASK = newProperty();

  private static final Instantiator<GoogleTask> instantiator = lookup -> {
    GoogleTask task = new GoogleTask();
    task.setListId(lookup.valueOf(LIST, "1"));
    task.setOrder(lookup.valueOf(ORDER, 0));
    task.setRemoteId(lookup.valueOf(REMOTE_ID, UUIDHelper.newUUID()));
    task.setTask(lookup.valueOf(TASK, 1));
    return task;
  };

  @SafeVarargs
  public static GoogleTask newGoogleTask(PropertyValue<? super GoogleTask, ?>... properties) {
    return make(instantiator, properties);
  }
}
