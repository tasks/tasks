package org.tasks.makers;

import com.natpryce.makeiteasy.Instantiator;
import com.natpryce.makeiteasy.Property;
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

    private static final Instantiator<GtasksList> instantiator = lookup -> {
        StoreObject storeObject = new StoreObject() {{
            setType(GtasksList.TYPE);
            setDeleted(0L);
            setId(lookup.valueOf(GtaskListMaker.ID, 0L));
            setItem(lookup.valueOf(REMOTE_ID, "1"));
            setValue(lookup.valueOf(NAME, "Default"));
            setValue2(String.valueOf(lookup.valueOf(ORDER, 0)));
            setValue3(String.valueOf(lookup.valueOf(LAST_SYNC, 0L)));
        }};
        return new GtasksList(storeObject);
    };
}
