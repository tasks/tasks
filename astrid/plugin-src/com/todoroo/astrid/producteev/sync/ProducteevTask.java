package com.todoroo.astrid.producteev.sync;

import com.todoroo.andlib.data.Property.LongProperty;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.data.Metadata;
import com.todoroo.astrid.producteev.ProducteevUtilities;

/**
 * Metadata entries for a Producteev Task
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class ProducteevTask {

    /** metadata key */
    public static final String METADATA_KEY = "producteev"; //$NON-NLS-1$

    /** task id in producteev */
    public static final LongProperty ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE1.name);

    /** dashboard id */
    public static final LongProperty DASHBOARD_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE2.name);

    /** creator id */
    public static final LongProperty CREATOR_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE3.name);

    /** responsible id */
    public static final LongProperty RESPONSIBLE_ID = new LongProperty(Metadata.TABLE,
            Metadata.VALUE4.name);

    /** repeating settings */
    public static final StringProperty REPEATING_SETTING = new StringProperty(Metadata.TABLE,
            Metadata.VALUE5.name);

    public static Metadata newMetadata() {
        Metadata metadata = new Metadata();
        metadata.setValue(Metadata.KEY, ProducteevTask.METADATA_KEY);
        metadata.setValue(ID, 0L);
        metadata.setValue(DASHBOARD_ID, ProducteevUtilities.INSTANCE.getDefaultDashboard());
        metadata.setValue(CREATOR_ID, Preferences.getLong(ProducteevUtilities.PREF_USER_ID, 0L));
        metadata.setValue(RESPONSIBLE_ID, Preferences.getLong(ProducteevUtilities.PREF_USER_ID, 0L));
        return metadata;
    }

}
