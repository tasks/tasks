package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;

public class RemoteGtaskListMaker {

  public static final Property<TaskList, String> REMOTE_ID = newProperty();
  public static final Property<TaskList, String> NAME = newProperty();
  private static final Instantiator<TaskList> instantiator =
      lookup ->
          new TaskList()
              .setId(lookup.valueOf(REMOTE_ID, "1"))
              .setTitle(lookup.valueOf(NAME, "Default"))
              .setUpdated(new DateTime(currentTimeMillis()));

  public static TaskList newRemoteList(PropertyValue<? super TaskList, ?>... properties) {
    return make(instantiator, properties);
  }
}
