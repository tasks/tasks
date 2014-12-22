package org.tasks.makers;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
import com.natpryce.makeiteasy.PropertyLookup;
import com.natpryce.makeiteasy.PropertyValue;
import com.todoroo.astrid.data.StoreObject;
import com.todoroo.astrid.gtasks.GtasksList;

import static com.natpryce.makeiteasy.Property.newProperty;
import static org.tasks.makers.Maker.make;

public class GtaskListMaker {

    public static final Property<GtasksList, Long> ID = newProperty();
    public static final Property<GtasksList, Integer> ORDER = newProperty();
    public static final Property<GtasksList, String> REMOTE_ID = newProperty();
    public static final Property<GtasksList, Long> LAST_SYNC = newProperty();
    public static final Property<GtasksList, String> NAME = newProperty();

    public static GtasksList newGtaskList(PropertyValue<? super GtasksList, ?>... properties) {
        return make(instantiator, properties);
    }

    private static final Instantiator<GtasksList> instantiator = new Instantiator<GtasksList>() {
        @Override
        public GtasksList instantiate(final PropertyLookup<GtasksList> lookup) {
            return new GtasksList(new StoreObject() {{
                setType(GtasksList.TYPE);
                setValue(StoreObject.ID, lookup.valueOf(GtaskListMaker.ID, 1L));
                setValue(StoreObject.ITEM, lookup.valueOf(REMOTE_ID, "1"));
                setValue(StoreObject.VALUE1, lookup.valueOf(NAME, "Default"));
                setValue(StoreObject.VALUE2, String.valueOf(lookup.valueOf(ORDER, 0)));
                setValue(StoreObject.VALUE3, String.valueOf(lookup.valueOf(LAST_SYNC, 0L)));
            }});
        }
    };
}
