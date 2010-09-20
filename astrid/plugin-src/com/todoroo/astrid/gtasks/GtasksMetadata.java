package com.todoroo.astrid.gtasks;

import com.todoroo.andlib.data.Property.IntegerProperty;
import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.utility.Preferences;

/**
 * Metadata entries for a GTasks Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class GtasksMetadata {

    private static final int VALUE_UNSET = -1;

    /** metadata key */
    public static final String METADATA_KEY = "gtasks"; //$NON-NLS-1$

    /** task id in google */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    public static final LongProperty LIST_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    public static final LongProperty OWNER_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    public static final IntegerProperty INDENTATION = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    public static final IntegerProperty ORDERING = new IntegerProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public static Metadata createEmptyMetadata() {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, GtasksMetadata.METADATA_KEY);
        metadata.setValue(ID, (long)VALUE_UNSET);
        metadata.setValue(LIST_ID, Preferences.getLong(GtasksPreferenceService.PREF_DEFAULT_LIST,
                VALUE_UNSET));
        metadata.setValue(OWNER_ID, (long)VALUE_UNSET);
        metadata.setValue(INDENTATION, 0);
        metadata.setValue(ORDERING, VALUE_UNSET);
        return metadata;
    }

}
