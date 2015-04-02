package org.tasks.location;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.astrid.data.Metadata;

import static com.todoroo.andlib.data.Property.DoubleProperty;
import static com.todoroo.andlib.data.Property.StringProperty;

public class GeofenceFields {

    public static final String METADATA_KEY = "geofence";

    public static final StringProperty PLACE = new StringProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final DoubleProperty LATITUDE = new DoubleProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final DoubleProperty LONGITUDE = new DoubleProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    public static final IntegerProperty RADIUS = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);
}
