package org.tasks.makers;

import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.TaskList;
import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import com.natpryce.makeiteasy.PropertyValue;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.time.DateTimeUtils.currentTimeMillis;
import static org.tasks.makers.Maker.make;

public class RemoteGtaskListMaker {
    public static final Property<TaskList, String> REMOTE_ID = newProperty();
    public static final Property<TaskList, String> NAME = newProperty();

    public static TaskList newRemoteList(PropertyValue<? super TaskList, ?>... properties) {
        return make(instantiator, properties);
    }

    private static final Instantiator<TaskList> instantiator = new Instantiator<TaskList>() {
        @Override
        public TaskList instantiate(final PropertyLookup<TaskList> lookup) {
            return new TaskList()
                    .setId(lookup.valueOf(REMOTE_ID, "1"))
                    .setTitle(lookup.valueOf(NAME, "Default"))
                    .setUpdated(new DateTime(currentTimeMillis()));
        }
    };
}
