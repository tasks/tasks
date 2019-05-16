package org.tasks.makers;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyValue;
import org.tasks.data.GoogleTaskList;

public class GtaskListMaker {

  public static final Property<GoogleTaskList, Long> ID = newProperty();
  public static final Property<GoogleTaskList, String> ACCOUNT = newProperty();
  public static final Property<GoogleTaskList, String> REMOTE_ID = newProperty();
  public static final Property<GoogleTaskList, Long> LAST_SYNC = newProperty();
  public static final Property<GoogleTaskList, String> NAME = newProperty();
  private static final Property<GoogleTaskList, Integer> ORDER = newProperty();
  private static final Property<GoogleTaskList, Integer> COLOR = newProperty();
  private static final Instantiator<GoogleTaskList> instantiator =
      lookup -> {
        GoogleTaskList list = new GoogleTaskList();
        list.setId(lookup.valueOf(GtaskListMaker.ID, 0L));
        list.setAccount(lookup.valueOf(ACCOUNT, "account"));
        list.setRemoteId(lookup.valueOf(REMOTE_ID, "1"));
        list.setTitle(lookup.valueOf(NAME, "Default"));
        list.setRemoteOrder(lookup.valueOf(ORDER, 0));
        list.setLastSync(lookup.valueOf(LAST_SYNC, 0L));
        list.setColor(lookup.valueOf(COLOR, -1));
        return list;
      };

  @SafeVarargs
  public static GoogleTaskList newGtaskList(
      PropertyValue<? super GoogleTaskList, ?>... properties) {
    return make(instantiator, properties);
  }
}
